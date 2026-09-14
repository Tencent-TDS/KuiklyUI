/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.animation.core

import com.tencent.kuikly.core.base.AbstractBaseView
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.NativeAnimationBridge
import com.tencent.kuikly.core.base.PagerScope
import com.tencent.kuikly.core.base.nativeCallbackTimeoutMillis
import com.tencent.kuikly.core.base.nativeSnap
import com.tencent.kuikly.core.base.registerPersistentNativeAnimationCompletion
import com.tencent.kuikly.core.base.unregisterNativeAnimationCompletion
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.pager.IPager
import com.tencent.kuikly.core.timer.clearTimeout
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.ReusableGraphicsLayerScope
import com.tencent.kuikly.compose.ui.graphics.TransformOrigin
import com.tencent.kuikly.compose.ui.graphics.toArgb
import com.tencent.kuikly.compose.ui.unit.IntOffset
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.math.abs
import kotlin.coroutines.resume

internal fun shouldPreserveNativePresentationOnCancellation(
    replacementRequested: Boolean,
    delayedNativeSnap: Boolean
): Boolean = replacementRequested || delayedNativeSnap

/**
 * A page-local transaction coordinator. It stages the target-state property writes produced by
 * one Compose render pass and only exposes them to Native Render after the complete batch has
 * passed validation.
 */
internal class NativeAnimationCoordinator private constructor(
    private val pager: IPager
) : NativeAnimationBridge {
    internal enum class TransitionCompletion {
        Finished,
        Fallback,
        Superseded
    }

    private val pagerScope = pager as PagerScope
    private data class Operation(
        val view: AbstractBaseView<*, *>,
        val attr: Attr,
        val propertyKey: String,
        val previousValue: Any?,
        val targetValue: Any
    )

    private data class Endpoint(val initialValue: Any?, val targetValue: Any?)

    private class Group(
        val id: Long,
        val animation: Animation,
        val descriptorSignature: String,
        val continuation: CancellableContinuation<Boolean>?,
        val transitionKey: Any? = null,
        val operations: MutableList<Operation> = mutableListOf(),
        val animatedProperties: MutableSet<String> = mutableSetOf(),
        val transitionCompletions: MutableList<(TransitionCompletion) -> Unit> = mutableListOf(),
        val targetStateCommits: MutableList<() -> Unit> = mutableListOf(),
        val genericEndpoints: MutableList<Endpoint> = mutableListOf(),
        val propertyEndpoints: MutableMap<String, MutableList<Endpoint>> = mutableMapOf(),
        val ownedProperties: MutableSet<Pair<Int, String>> = mutableSetOf(),
        var unsupported: Boolean = false,
        var hasUnhintedParticipant: Boolean = false,
        var deferredEmptyCollectionPass: Boolean = false,
        var preparingInitialState: Boolean = transitionKey != null,
        var committed: Boolean = false,
        var timeoutRef: String? = null,
        val pendingViews: MutableSet<Int> = mutableSetOf()
    )

    private var nextGroupId = 1L
    private var activeGroup: Group? = null
    private val runningGroups = mutableMapOf<Long, Group>()
    private val rejectedTransitionKeys = mutableSetOf<Any>()
    private var destroyed = false

    suspend fun animate(
        animation: Animation,
        initialValue: Any?,
        targetValue: Any?,
        shouldPreservePresentationOnCancellation: () -> Boolean,
        targetStateCommit: () -> Unit
    ): Boolean =
        suspendCancellableCoroutine { continuation ->
            if (destroyed) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }
            cancelActiveGroup()
            val group = Group(
                nextGroupId++,
                animation,
                animation.toString(),
                continuation,
                hasUnhintedParticipant = true
            )
            animation.key = "composeNativeAnimation_${group.id}"
            group.genericEndpoints += Endpoint(initialValue, targetValue)
            NativeAnimationTrace.log {
                "create group=${group.id} kind=animatable descriptor=${group.descriptorSignature}"
            }
            activeGroup = group
            continuation.invokeOnCancellation {
                if (activeGroup === group) {
                    NativeAnimationTrace.log {
                        "cancel pending group=${group.id} operations=${group.operations.size}"
                    }
                    rollback(group)
                    activeGroup = null
                } else if (runningGroups[group.id] === group) {
                    if (
                        shouldPreserveNativePresentationOnCancellation(
                            replacementRequested =
                                shouldPreservePresentationOnCancellation(),
                            delayedNativeSnap = group.isDelayedNativeSnap()
                        )
                    ) {
                        NativeAnimationTrace.log {
                            "cancel running group=${group.id} " +
                                "action=preserve-presentation-for-replacement"
                        }
                    } else {
                        NativeAnimationTrace.log {
                            "cancel running group=${group.id} action=snap-and-finish-false"
                        }
                        snapCommittedGroupToLogicalTarget(group)
                    }
                    finish(group, false)
                }
            }
            targetStateCommit()
        }

    fun animateTransition(
        transitionKey: Any,
        propertyHint: String?,
        animation: Animation,
        initialValue: Any?,
        targetValue: Any?,
        targetStateCommit: () -> Unit,
        completion: (TransitionCompletion) -> Unit
    ): Boolean {
        if (destroyed || transitionKey in rejectedTransitionKeys) return false
        val signature = animation.toString()
        val pending = activeGroup
        val group = when {
            pending == null -> Group(
                nextGroupId++,
                animation,
                signature,
                continuation = null,
                transitionKey = transitionKey
            ).also {
                animation.key = "composeNativeAnimation_${it.id}"
                activeGroup = it
                NativeAnimationTrace.log {
                    "create group=${it.id} kind=transition key=${transitionKey.hashCode()} " +
                        "property=$propertyHint descriptor=$signature"
                }
                supersedeRunningTransitionGroups(transitionKey, it.id)
            }
            pending.continuation != null -> {
                NativeAnimationTrace.log {
                    "reject transition key=${transitionKey.hashCode()} reason=animatable-pending"
                }
                return false
            }
            pending.transitionKey !== transitionKey -> {
                NativeAnimationTrace.log {
                    "reject transition key=${transitionKey.hashCode()} reason=different-key " +
                        "activeGroup=${pending.id}"
                }
                return false
            }
            pending.descriptorSignature != signature -> {
                pending.unsupported = true
                NativeAnimationTrace.log {
                    "reject group=${pending.id} reason=mixed-descriptor"
                }
                return false
            }
            else -> pending
        }
        if (propertyHint != null) {
            group.animatedProperties += propertyHint
            group.propertyEndpoints.getOrPut(propertyHint) { mutableListOf() }
                .add(Endpoint(initialValue, targetValue))
        } else {
            group.hasUnhintedParticipant = true
            group.genericEndpoints += Endpoint(initialValue, targetValue)
        }
        group.transitionCompletions += completion
        group.targetStateCommits += targetStateCommit
        NativeAnimationTrace.log {
            "join group=${group.id} property=$propertyHint " +
                "participants=${group.transitionCompletions.size}"
        }
        return true
    }

    /**
     * A transition needs one frame to materialize its initial state before its target properties
     * can be staged. Do not leave the previous logical group alive during that frame: its native
     * animator may finish and report success, which can make AnimatedVisibility dispose an exit
     * node before the reversing group gets a chance to commit.
     *
     * Finishing the coordinator record does not cancel the platform animator. It keeps presenting
     * until the new property batch is committed, where Native Render replaces it from the current
     * presentation value.
     */
    private fun supersedeRunningTransitionGroups(transitionKey: Any, replacementGroupId: Long) {
        runningGroups.values
            .filter { it.transitionKey === transitionKey }
            .toList()
            .forEach {
                NativeAnimationTrace.log {
                    "supersede running group=${it.id} by pending group=$replacementGroupId " +
                        "key=${transitionKey.hashCode()}"
                }
                finish(it, false)
            }
    }

    fun rejectTransition(transitionKey: Any) {
        rejectedTransitionKeys += transitionKey
        activeGroup?.takeIf { it.transitionKey === transitionKey }?.unsupported = true
        NativeAnimationTrace.log {
            "reject transition key=${transitionKey.hashCode()} activeGroup=${activeGroup?.id}"
        }
    }

    override fun stageProperty(
        view: AbstractBaseView<*, *>,
        attr: Attr,
        propertyKey: String,
        previousValue: Any?,
        targetValue: Any
    ): Boolean {
        val group = activeGroup ?: return false
        if (group.committed || propertyKey == Attr.StyleConst.ANIMATION) return false
        if (group.preparingInitialState) {
            NativeAnimationTrace.log {
                "materialize initial group=${group.id} view=${view.nativeRef} property=$propertyKey"
            }
            return false
        }
        if (
            propertyKey in SUPPORTED_PROPERTIES &&
            !group.owns(view, propertyKey) &&
            !group.matchesPropertyEndpoint(propertyKey, previousValue, targetValue)
        ) {
            NativeAnimationTrace.log {
                "pass foreign group=${group.id} view=${view.nativeRef} property=$propertyKey"
            }
            return false
        }
        if (propertyKey in SUPPORTED_PROPERTIES) {
            group.ownedProperties += view.nativeRef to propertyKey
        }
        if (propertyKey !in SUPPORTED_PROPERTIES) {
            if (!group.hasUnhintedParticipant) {
                NativeAnimationTrace.log {
                    "pass unrelated group=${group.id} view=${view.nativeRef} " +
                        "property=$propertyKey"
                }
                return false
            }
            group.unsupported = true
            NativeAnimationTrace.log {
                "mark unsupported group=${group.id} property=$propertyKey"
            }
        } else if (
            group.transitionKey != null &&
            group.animatedProperties.isNotEmpty() &&
            propertyKey !in group.animatedProperties
        ) {
            NativeAnimationTrace.log {
                "pass static property group=${group.id} view=${view.nativeRef} " +
                    "property=$propertyKey"
            }
            return false
        }
        group.operations += Operation(view, attr, propertyKey, previousValue, targetValue)
        NativeAnimationTrace.log {
            "stage group=${group.id} view=${view.nativeRef} property=$propertyKey " +
                "from=$previousValue to=$targetValue"
        }
        return true
    }

    /**
     * Associates target-state graphics-layer writes with the animation state that produced them.
     * This prevents a page-level transaction from capturing an unrelated Compose animation that
     * happens to update the same property during the collection frame.
     */
    fun registerGraphicsLayerTarget(
        view: AbstractBaseView<*, *>?,
        previousAlpha: Float,
        previousScaleX: Float,
        previousScaleY: Float,
        previousTranslationX: Float,
        previousTranslationY: Float,
        previousRotationX: Float,
        previousRotationY: Float,
        previousRotationZ: Float,
        previousTransformOrigin: TransformOrigin,
        target: ReusableGraphicsLayerScope
    ) {
        val group = activeGroup ?: return
        if (view == null || group.preparingInitialState || group.committed) return
        if (group.matchesFloatEndpoint(
                Attr.StyleConst.OPACITY,
                previousAlpha,
                target.alpha
            )
        ) {
            group.ownedProperties += view.nativeRef to Attr.StyleConst.OPACITY
        }
        val scalarTransformChanges = listOf(
            previousScaleX to target.scaleX,
            previousScaleY to target.scaleY,
            previousTranslationX to target.translationX,
            previousTranslationY to target.translationY,
            previousRotationX to target.rotationX,
            previousRotationY to target.rotationY,
            previousRotationZ to target.rotationZ,
            previousTransformOrigin.pivotFractionX to target.transformOrigin.pivotFractionX,
            previousTransformOrigin.pivotFractionY to target.transformOrigin.pivotFractionY
        )
        if (
            scalarTransformChanges.any { (initial, targetValue) ->
                group.matchesFloatEndpoint(Attr.StyleConst.TRANSFORM, initial, targetValue)
            } ||
            group.matchesStructuredTransformEndpoint(
                previousTranslationX,
                previousTranslationY,
                target.translationX,
                target.translationY,
                previousTransformOrigin,
                target.transformOrigin
            )
        ) {
            group.ownedProperties += view.nativeRef to Attr.StyleConst.TRANSFORM
        }
    }

    /**
     * Associates a solid background write with the Animatable/Transition endpoint that produced
     * it. Background is synchronized from a draw modifier rather than [RenderNodeLayer], so it
     * needs an explicit ownership hook of its own.
     */
    fun registerBackgroundColorTarget(
        view: AbstractBaseView<*, *>?,
        previousColor: Color?,
        targetColor: Color
    ) {
        val group = activeGroup ?: return
        if (
            view == null ||
            previousColor == null ||
            group.preparingInitialState ||
            group.committed
        ) {
            return
        }
        if (group.matchesColorEndpoint(previousColor, targetColor)) {
            group.ownedProperties += view.nativeRef to Attr.StyleConst.BACKGROUND_COLOR
        }
    }

    override fun stageFrame(view: AbstractBaseView<*, *>): Boolean {
        val group = activeGroup ?: return false
        if (group.committed) return false
        if (group.preparingInitialState) return false
        // Entering/crossfading nodes were laid out during the initial-state materialization pass.
        // The following target-state pass exists only to collect supported visual properties.
        // Releasing its frame writes would expose transient page/sibling layout from this extra
        // pass (for example a LazyColumn section title briefly moving during AnimatedVisibility).
        if (group.animatedProperties.isNotEmpty()) {
            NativeAnimationTrace.log {
                "consume frame group=${group.id} view=${view.nativeRef} " +
                    "reason=visual-transition-target-pass"
            }
            return true
        }
        group.unsupported = true
        NativeAnimationTrace.log {
            "consume frame group=${group.id} view=${view.nativeRef} reason=frame-only"
        }
        return true
    }

    override fun commitStagedProperties() {
        val group = activeGroup ?: run {
            rejectedTransitionKeys.clear()
            return
        }
        rejectedTransitionKeys.clear()
        if (group.committed) return
        if (group.preparingInitialState) {
            if (group.unsupported) {
                NativeAnimationTrace.log {
                    "fallback initial group=${group.id} reason=unsupported"
                }
                activeGroup = null
                group.transitionCompletions.forEach { it(TransitionCompletion.Fallback) }
                return
            }
            group.preparingInitialState = false
            NativeAnimationTrace.log {
                "initial materialized group=${group.id}; schedule target pass " +
                    "participants=${group.targetStateCommits.size}"
            }
            group.targetStateCommits.forEach { it() }
            group.targetStateCommits.clear()
            return
        }
        if (
            group.continuation != null &&
            !group.unsupported &&
            group.operations.isEmpty() &&
            !group.deferredEmptyCollectionPass
        ) {
            // Animatable/animateAsState is commonly started from a side effect after the current
            // render pass has already drawn. Its logical target mutation schedules the following
            // pass, so an empty transaction here does not yet mean the property is unsupported.
            // Keep the group armed for exactly one additional pass. A truly unsupported consumer
            // still falls back on that next pass (or immediately when a frame write marks it).
            group.deferredEmptyCollectionPass = true
            NativeAnimationTrace.log {
                "defer empty target pass group=${group.id}"
            }
            return
        }
        if (group.unsupported || group.operations.isEmpty()) {
            NativeAnimationTrace.log {
                "fallback group=${group.id} unsupported=${group.unsupported} " +
                    "operations=${group.operations.size}"
            }
            rollback(group)
            activeGroup = null
            if (group.continuation?.isActive == true) group.continuation.resume(false)
            group.transitionCompletions.forEach { it(TransitionCompletion.Fallback) }
            return
        }

        group.committed = true
        NativeAnimationTrace.log {
            "commit group=${group.id} views=${group.operations.map { it.view.nativeRef }.distinct()} " +
                "properties=${group.operations.map { it.propertyKey }} " +
                "participants=${group.transitionCompletions.size}"
        }
        val operationsByView = group.operations.groupBy { it.view }
        val replacingViews = group.operations.mapTo(mutableSetOf()) { it.view.nativeRef }
        runningGroups.values.filter { running ->
            running.operations.any { it.view.nativeRef in replacingViews }
        }.toList().forEach { finish(it, false) }
        operationsByView.forEach { (view, operations) ->
            val declarativeView = view as DeclarativeBaseView<*, *>
            operations.forEach { operation ->
                if (operation.previousValue == null) {
                    nativeDefaultInitialValue(operation.propertyKey)?.let { initialValue ->
                        NativeAnimationTrace.log {
                            "materialize default group=${group.id} view=${view.nativeRef} " +
                                "property=${operation.propertyKey} value=$initialValue"
                        }
                        view.syncProp(operation.propertyKey, initialValue)
                    }
                }
            }
            // Android and HarmonyOS report one completion for the whole View batch. iOS reports
            // once per property, but every property in this group uses the same descriptor and
            // therefore finishes on the same timeline. Treat the first callback as the View's
            // batch completion so all three Render implementations share one contract.
            group.pendingViews += view.nativeRef
            NativeAnimationTrace.log {
                "enqueue group=${group.id} view=${view.nativeRef}"
            }
            declarativeView.registerPersistentNativeAnimationCompletion(
                group.animation.key
            ) { finished: Boolean ->
                onViewAnimationFinished(group, view.nativeRef, finished)
            }
            view.syncProp(Attr.StyleConst.ANIMATION, group.animation.toString())
            operations.forEach { operation ->
                view.syncProp(operation.propertyKey, operation.targetValue)
            }
        }
        activeGroup = null
        runningGroups[group.id] = group
        val timeoutMillis = group.animation.nativeCallbackTimeoutMillis()
        group.timeoutRef = pagerScope.setTimeout(timeoutMillis) {
            if (runningGroups[group.id] === group) {
                NativeAnimationTrace.log {
                    "timeout group=${group.id} pending=${group.pendingViews}"
                }
                snapCommittedGroupToLogicalTarget(group)
                finish(
                    group = group,
                    result = false,
                    transitionCompletion = TransitionCompletion.Finished
                )
            }
        }
        // Commit only after every descriptor and target property has entered the render queue.
        operationsByView.keys.forEach { view ->
            view.syncProp(Attr.StyleConst.ANIMATION, "")
        }
    }

    private fun onViewAnimationFinished(group: Group, viewRef: Int, finished: Boolean) {
        if (runningGroups[group.id] !== group) {
            NativeAnimationTrace.log {
                "ignore callback group=${group.id} view=$viewRef finished=$finished reason=not-running"
            }
            return
        }
        NativeAnimationTrace.log {
            "callback group=${group.id} view=$viewRef finished=$finished " +
                "pending=${group.pendingViews}"
        }
        if (!finished) {
            snapCommittedGroupToLogicalTarget(group)
            finish(
                group = group,
                result = false,
                transitionCompletion = TransitionCompletion.Finished
            )
            return
        }
        if (!group.pendingViews.remove(viewRef)) return
        if (group.pendingViews.isEmpty()) finish(group, true)
    }

    private fun finish(
        group: Group,
        result: Boolean,
        transitionCompletion: TransitionCompletion =
            if (result) TransitionCompletion.Finished else TransitionCompletion.Superseded
    ) {
        NativeAnimationTrace.log {
            "finish group=${group.id} result=$result transition=$transitionCompletion " +
                "active=${activeGroup === group} " +
                "pending=${group.pendingViews}"
        }
        if (activeGroup === group) activeGroup = null
        runningGroups.remove(group.id)
        group.operations.map { it.view }.distinct().forEach {
            (it as? DeclarativeBaseView<*, *>)
                ?.unregisterNativeAnimationCompletion(group.animation.key)
        }
        group.timeoutRef?.let { pagerScope.clearTimeout(it) }
        group.timeoutRef = null
        if (group.continuation?.isActive == true) {
            if (result) {
                group.continuation.resume(true)
            } else {
                group.continuation.cancel()
            }
        }
        group.transitionCompletions.forEach { it(transitionCompletion) }
    }

    private fun rollback(group: Group) {
        NativeAnimationTrace.log {
            "rollback group=${group.id} operations=${group.operations.size}"
        }
        group.operations.asReversed().forEach {
            if (it.previousValue == null) {
                it.attr.removePropCache(it.propertyKey)
            } else {
                it.attr.updatePropCache(it.propertyKey, it.previousValue)
            }
        }
        group.operations.clear()
    }

    private fun cancelActiveGroup() {
        val old = activeGroup ?: return
        NativeAnimationTrace.log {
            "cancel active group=${old.id} committed=${old.committed}"
        }
        rollback(old)
        activeGroup = null
        old.continuation?.cancel()
        old.transitionCompletions.forEach { it(TransitionCompletion.Fallback) }
    }

    private fun snapCommittedGroupToLogicalTarget(group: Group) {
        NativeAnimationTrace.log {
            "snap group=${group.id} views=${group.operations.map { it.view.nativeRef }.distinct()}"
        }
        val snap = Animation.nativeSnap(0f, "composeNativeAnimationCancel_${group.id}")
        group.operations.groupBy { it.view }.forEach { (view, operations) ->
            view.syncProp(Attr.StyleConst.ANIMATION, snap.toString())
            operations.forEach {
                view.syncProp(it.propertyKey, it.targetValue)
            }
            view.syncProp(Attr.StyleConst.ANIMATION, "")
        }
    }

    override fun destroy() {
        NativeAnimationTrace.log {
            "destroy active=${activeGroup?.id} running=${runningGroups.keys}"
        }
        destroyed = true
        cancelActiveGroup()
        runningGroups.values.toList().forEach {
            snapCommittedGroupToLogicalTarget(it)
            it.timeoutRef?.let { timeoutRef -> pagerScope.clearTimeout(timeoutRef) }
            it.timeoutRef = null
            if (it.continuation?.isActive == true) it.continuation.cancel()
            it.transitionCompletions.forEach { completion ->
                completion(TransitionCompletion.Superseded)
            }
        }
        runningGroups.clear()
        pager.setValueForKey(NativeAnimationBridge.PAGER_CACHE_KEY, null)
    }

    companion object {
        private const val NATIVE_DESCRIPTOR_DELAY_INDEX = 5

        private val SUPPORTED_PROPERTIES = setOf(
            Attr.StyleConst.OPACITY,
            Attr.StyleConst.TRANSFORM,
            Attr.StyleConst.BACKGROUND_COLOR
        )

        fun currentOrNull(): NativeAnimationCoordinator? = try {
            getOrCreate(PagerManager.getCurrentPager())
        } catch (_: Throwable) {
            null
        }

        fun currentExistingOrNull(): NativeAnimationCoordinator? = try {
            PagerManager.getCurrentPager()
                .getValueForKey(NativeAnimationBridge.PAGER_CACHE_KEY)
                as? NativeAnimationCoordinator
        } catch (_: Throwable) {
            null
        }

        /**
         * Render/draw callbacks are not guaranteed to retain PagerManager's current-pager
         * context, especially when an Animatable was started from a coroutine. Resolve the
         * page-local coordinator from the actual target View instead.
         */
        fun existingForView(
            view: AbstractBaseView<*, *>?
        ): NativeAnimationCoordinator? = try {
            view?.getPager()
                ?.getValueForKey(NativeAnimationBridge.PAGER_CACHE_KEY)
                as? NativeAnimationCoordinator
        } catch (_: Throwable) {
            null
        }

        fun getOrCreate(pager: IPager): NativeAnimationCoordinator {
            val existing =
                pager.getValueForKey(NativeAnimationBridge.PAGER_CACHE_KEY)
                    as? NativeAnimationCoordinator
            if (existing != null) return existing
            return NativeAnimationCoordinator(pager).also {
                pager.setValueForKey(NativeAnimationBridge.PAGER_CACHE_KEY, it)
            }
        }
    }

    private fun Group.endpoints(propertyKey: String): List<Endpoint> =
        propertyEndpoints[propertyKey].orEmpty() + genericEndpoints

    private fun Group.owns(view: AbstractBaseView<*, *>, propertyKey: String): Boolean =
        (view.nativeRef to propertyKey) in ownedProperties

    private fun Group.matchesFloatEndpoint(
        propertyKey: String,
        initialValue: Float,
        targetValue: Float
    ): Boolean = endpoints(propertyKey).any { endpoint ->
        val initial = endpoint.initialValue as? Float ?: return@any false
        val target = endpoint.targetValue as? Float ?: return@any false
        !initial.approximately(target) &&
            initialValue.approximately(initial) &&
            targetValue.approximately(target)
    }

    private fun Group.matchesStructuredTransformEndpoint(
        initialX: Float,
        initialY: Float,
        targetX: Float,
        targetY: Float,
        initialOrigin: TransformOrigin,
        targetOrigin: TransformOrigin
    ): Boolean = endpoints(Attr.StyleConst.TRANSFORM).any { endpoint ->
        when {
            endpoint.initialValue is IntOffset && endpoint.targetValue is IntOffset -> {
                initialX.approximately(endpoint.initialValue.x.toFloat()) &&
                    initialY.approximately(endpoint.initialValue.y.toFloat()) &&
                    targetX.approximately(endpoint.targetValue.x.toFloat()) &&
                    targetY.approximately(endpoint.targetValue.y.toFloat())
            }
            endpoint.initialValue is Offset && endpoint.targetValue is Offset -> {
                initialX.approximately(endpoint.initialValue.x) &&
                    initialY.approximately(endpoint.initialValue.y) &&
                    targetX.approximately(endpoint.targetValue.x) &&
                    targetY.approximately(endpoint.targetValue.y)
            }
            endpoint.initialValue is TransformOrigin &&
                endpoint.targetValue is TransformOrigin -> {
                initialOrigin == endpoint.initialValue && targetOrigin == endpoint.targetValue
            }
            else -> false
        }
    }

    private fun Group.matchesPropertyEndpoint(
        propertyKey: String,
        previousValue: Any?,
        targetValue: Any
    ): Boolean = endpoints(propertyKey).any { endpoint ->
        when (val expectedTarget = endpoint.targetValue) {
            is Color -> {
                val initialColor = endpoint.initialValue as? Color ?: return@any false
                val initialArgb = initialColor.toArgb().toLong() and 0xFFFFFFFFL
                val targetArgb = expectedTarget.toArgb().toLong() and 0xFFFFFFFFL
                previousValue?.toString() == initialArgb.toString() &&
                    targetValue.toString() == targetArgb.toString()
            }
            is Float -> {
                (previousValue as? Number)?.toFloat()?.let { previous ->
                    (targetValue as? Number)?.toFloat()?.let { target ->
                        val initial = endpoint.initialValue as? Float
                        initial != null &&
                            previous.approximately(initial) &&
                            target.approximately(expectedTarget)
                    }
                } == true
            }
            else -> false
        }
    }

    private fun Group.matchesColorEndpoint(
        initialColor: Color,
        targetColor: Color
    ): Boolean = endpoints(Attr.StyleConst.BACKGROUND_COLOR).any { endpoint ->
        endpoint.initialValue == initialColor && endpoint.targetValue == targetColor
    }

    private fun Group.isDelayedNativeSnap(): Boolean {
        if (!descriptorSignature.endsWith(" v2,snap")) return false
        return descriptorSignature
            .split(' ')
            .getOrNull(NATIVE_DESCRIPTOR_DELAY_INDEX)
            ?.toFloatOrNull()
            ?.let { it > 0f } == true
    }

    private fun Float.approximately(other: Float): Boolean =
        abs(this - other) <= 0.001f
}

internal fun nativeDefaultInitialValue(propertyKey: String): Any? = when (propertyKey) {
    Attr.StyleConst.OPACITY -> 1f
    Attr.StyleConst.TRANSFORM ->
        "0.0|1.0 1.0|0.0 0.0|0.5 0.5|0.0 0.0|0.0 0.0"
    else -> null
}
