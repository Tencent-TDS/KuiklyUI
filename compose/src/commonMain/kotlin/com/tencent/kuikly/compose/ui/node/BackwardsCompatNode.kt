/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("DEPRECATION")

package com.tencent.kuikly.compose.ui.node

import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.BuildDrawCacheParams
import com.tencent.kuikly.compose.ui.draw.DrawCacheModifier
import com.tencent.kuikly.compose.ui.draw.DrawModifier
import com.tencent.kuikly.compose.ui.geometry.Size
import com.tencent.kuikly.compose.ui.graphics.drawscope.ContentDrawScope
import com.tencent.kuikly.compose.ui.layout.IntrinsicMeasurable
import com.tencent.kuikly.compose.ui.layout.IntrinsicMeasureScope
import com.tencent.kuikly.compose.ui.layout.LayoutCoordinates
import com.tencent.kuikly.compose.ui.layout.LayoutModifier
import com.tencent.kuikly.compose.ui.layout.Measurable
import com.tencent.kuikly.compose.ui.layout.MeasureResult
import com.tencent.kuikly.compose.ui.layout.MeasureScope
import com.tencent.kuikly.compose.ui.layout.OnGloballyPositionedModifier
import com.tencent.kuikly.compose.ui.layout.OnPlacedModifier
import com.tencent.kuikly.compose.ui.layout.OnRemeasuredModifier
import com.tencent.kuikly.compose.ui.layout.ParentDataModifier
import com.tencent.kuikly.compose.ui.layout.RemeasurementModifier
import com.tencent.kuikly.compose.ui.modifier.BackwardsCompatLocalMap
import com.tencent.kuikly.compose.ui.modifier.ModifierLocal
import com.tencent.kuikly.compose.ui.modifier.ModifierLocalConsumer
import com.tencent.kuikly.compose.ui.modifier.ModifierLocalMap
import com.tencent.kuikly.compose.ui.modifier.ModifierLocalModifierNode
import com.tencent.kuikly.compose.ui.modifier.ModifierLocalProvider
import com.tencent.kuikly.compose.ui.modifier.ModifierLocalReadScope
import com.tencent.kuikly.compose.ui.modifier.modifierLocalMapOf
import com.tencent.kuikly.compose.ui.semantics.SemanticsPropertyReceiver
import com.tencent.kuikly.compose.ui.unit.Constraints
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.unit.LayoutDirection
import com.tencent.kuikly.compose.ui.unit.toSize
import com.tencent.kuikly.compose.ui.input.pointer.PointerEvent
import com.tencent.kuikly.compose.ui.input.pointer.PointerEventPass
import com.tencent.kuikly.compose.ui.input.pointer.PointerInputModifier
import com.tencent.kuikly.compose.ui.node.DrawModifierNode
import com.tencent.kuikly.compose.ui.node.invalidateDraw

/**
 * This entity will end up implementing all of the entity type interfaces, but its [kindSet]
 * will only be set to the expected values based on the interface(s) that the modifier element that
 * it has implements. This is nice because it will be one class that simply delegates / pipes
 * everything to the modifier instance, but those interfaces should only be called in the cases
 * where the modifier would have been previously.
 */
@OptIn(ExperimentalComposeUiApi::class)
internal
class BackwardsCompatNode(element: Modifier.Element) :
    LayoutModifierNode,
    DrawModifierNode,
    SemanticsModifierNode,
    PointerInputModifierNode,
    ModifierLocalModifierNode,
    ModifierLocalReadScope,
    ParentDataModifierNode,
    LayoutAwareModifierNode,
    GlobalPositionAwareModifierNode,
    // todo: jonas
//    FocusEventModifierNode,
//    FocusPropertiesModifierNode,
//    FocusRequesterModifierNode,
    OwnerScope,
    BuildDrawCacheParams,
    Modifier.Node() {
    init {
        kindSet = calculateNodeKindSetFrom(element)
    }

    var element: Modifier.Element = element
        set(value) {
            if (isAttached) unInitializeModifier()
            field = value
            kindSet = calculateNodeKindSetFrom(value)
            if (isAttached) initializeModifier(false)
        }

    override fun onAttach() {
        initializeModifier(true)
    }

    override fun onDetach() {
        unInitializeModifier()
    }

    private fun unInitializeModifier() {
        check(isAttached) { "unInitializeModifier called on unattached node" }
        val element = element
        if (isKind(Nodes.Locals)) {
            if (element is ModifierLocalProvider<*>) {
                requireOwner()
                    .modifierLocalManager
                    .removedProvider(this, element.key)
            }
            if (element is ModifierLocalConsumer) {
                element.onModifierLocalsUpdated(DetachedModifierLocalReadScope)
            }
        }
        if (isKind(Nodes.Semantics)) {
            requireOwner().onSemanticsChange()
        }
//        if (element is FocusRequesterModifier) {
//            // TODO: jonas
////            element.focusRequester.focusRequesterNodes -= this
//        }
    }

    private fun initializeModifier(duringAttach: Boolean) {
        check(isAttached) { "initializeModifier called on unattached node" }
        val element = element
        if (isKind(Nodes.Locals)) {
            if (element is ModifierLocalConsumer) {
                sideEffect { updateModifierLocalConsumer() }
            }
            if (element is ModifierLocalProvider<*>) {
                updateModifierLocalProvider(element)
            }
        }
        if (isKind(Nodes.Draw)) {
            if (element is DrawCacheModifier) {
                invalidateCache = true
            }
            if (!duringAttach) {
                invalidateLayer()
            }
        }
        if (isKind(Nodes.Layout)) {
            val isChainUpdate = isChainUpdate()
            if (isChainUpdate) {
                val coordinator = coordinator!!
                coordinator as LayoutModifierNodeCoordinator
                coordinator.layoutModifierNode = this
                coordinator.onLayoutModifierNodeChanged()
            }
            if (!duringAttach) {
                invalidateLayer()
                requireLayoutNode().invalidateMeasurements()
            }
        }
        if (element is RemeasurementModifier) {
            element.onRemeasurementAvailable(requireLayoutNode())
        }
        if (isKind(Nodes.LayoutAware)) {
            if (element is OnRemeasuredModifier) {
                // if the modifier was added but layout has already happened and might not change,
                // we want to call remeasured in case layout doesn't happen again
                val isChainUpdate = isChainUpdate()
                if (isChainUpdate) {
                    requireLayoutNode().invalidateMeasurements()
                }
            }
            if (element is OnPlacedModifier) {
                lastOnPlacedCoordinates = null
                val isChainUpdate = isChainUpdate()
                if (isChainUpdate) {
                    requireOwner().registerOnLayoutCompletedListener(
                        object : Owner.OnLayoutCompletedListener {
                            override fun onLayoutComplete() {
                                if (lastOnPlacedCoordinates == null) {
                                    onPlaced(requireCoordinator(Nodes.LayoutAware))
                                }
                            }
                        }
                    )
                }
            }
        }
        if (isKind(Nodes.GlobalPositionAware)) {
            // if the modifier was added but layout has already happened and might not change,
            // we want to call remeasured in case layout doesn't happen again
            if (element is OnGloballyPositionedModifier) {
                val isChainUpdate = isChainUpdate()
                if (isChainUpdate) {
                    requireLayoutNode().invalidateMeasurements()
                }
            }
        }
//        if (element is FocusRequesterModifier) {
//            // todo: jonas
////            element.focusRequester.focusRequesterNodes += this
//        }
        if (isKind(Nodes.PointerInput)) {
            if (element is PointerInputModifier) {
                element.pointerInputFilter.layoutCoordinates = coordinator
            }
        }
        if (isKind(Nodes.Semantics)) {
            requireOwner().onSemanticsChange()
        }
    }

    // BuildDrawCacheParams
    override val density get() = requireLayoutNode().density
    override val layoutDirection: LayoutDirection get() = requireLayoutNode().layoutDirection
    override val size: Size
        get() {
            return requireCoordinator(Nodes.LayoutAware).size.toSize()
        }

    // Flag to determine if the cache should be re-built
    private var invalidateCache = true

    override fun onMeasureResultChanged() {
        invalidateCache = true
        invalidateDraw()
    }

    private fun updateDrawCache() {
        val element = element
        if (element is DrawCacheModifier) {
            requireOwner()
                .snapshotObserver
                .observeReads(this, onDrawCacheReadsChanged) {
                    element.onBuildCache(this)
                }
        }
        invalidateCache = false
    }

    internal fun onDrawCacheReadsChanged() {
        invalidateCache = true
        invalidateDraw()
    }

    private var _providedValues: BackwardsCompatLocalMap? = null
    var readValues = hashSetOf<ModifierLocal<*>>()
    override val providedValues: ModifierLocalMap get() = _providedValues ?: modifierLocalMapOf()

    override val <T> ModifierLocal<T>.current: T
        get() {
            val key = this
            readValues.add(key)
            visitAncestors(Nodes.Locals) {
                if (it.providedValues.contains(key)) {
                    @Suppress("UNCHECKED_CAST")
                    return it.providedValues[key] as T
                }
            }
            return key.defaultFactory()
        }

    fun updateModifierLocalConsumer() {
        if (isAttached) {
            readValues.clear()
            requireOwner().snapshotObserver.observeReads(
                this,
                updateModifierLocalConsumer
            ) {
                (element as ModifierLocalConsumer).onModifierLocalsUpdated(this)
            }
        }
    }

    private fun updateModifierLocalProvider(element: ModifierLocalProvider<*>) {
        val providedValues = _providedValues
        if (providedValues != null && providedValues.contains(element.key)) {
            providedValues.element = element
            requireOwner()
                .modifierLocalManager
                .updatedProvider(this, element.key)
        } else {
            _providedValues = BackwardsCompatLocalMap(element)
            // we only need to notify the modifierLocalManager of an inserted provider
            // in the cases where a provider was added to the chain where it was possible
            // that consumers below it could need to be invalidated. If this layout node
            // is just now being created, then that is impossible. In this case, we can just
            // do nothing and wait for the child consumers to read us. We infer this by
            // checking to see if the tail node is attached or not. If it is not, then the node
            // chain is being attached for the first time.
            val isChainUpdate = isChainUpdate()
            if (isChainUpdate) {
                requireOwner()
                    .modifierLocalManager
                    .insertedProvider(this, element.key)
            }
        }
    }

    override val isValidOwnerScope: Boolean get() = isAttached

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        return with(element as LayoutModifier) {
            measure(measurable, constraints)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int = with(element as LayoutModifier) {
        minIntrinsicWidth(measurable, height)
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = with(element as LayoutModifier) {
        minIntrinsicHeight(measurable, width)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int = with(element as LayoutModifier) {
        maxIntrinsicWidth(measurable, height)
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int = with(element as LayoutModifier) {
        maxIntrinsicHeight(measurable, width)
    }

    override fun ContentDrawScope.draw() {
        val element = element
        with(element as DrawModifier) {
            if (invalidateCache && element is DrawCacheModifier) {
                updateDrawCache()
            }
            draw()
        }
    }

    override fun SemanticsPropertyReceiver.applySemantics() {
        // todo: jonasluo
//        val config = (element as SemanticsModifier).semanticsConfiguration
//        val toMergeInto = (this as SemanticsConfiguration)
//        toMergeInto.collapsePeer(config)
    }

    override fun onPointerEvent(
        pointerEvent: PointerEvent,
        pass: PointerEventPass,
        bounds: IntSize
    ) {
        with(element as PointerInputModifier) {
            pointerInputFilter.onPointerEvent(pointerEvent, pass, bounds)
        }
    }

    override fun onCancelPointerInput() {
        with(element as PointerInputModifier) {
            pointerInputFilter.onCancel()
        }
    }

    override fun sharePointerInputWithSiblings(): Boolean {
        return with(element as PointerInputModifier) {
            pointerInputFilter.shareWithSiblings
        }
    }

    override fun interceptOutOfBoundsChildEvents(): Boolean {
        return with(element as PointerInputModifier) {
            pointerInputFilter.interceptOutOfBoundsChildEvents
        }
    }

    override fun Density.modifyParentData(parentData: Any?): Any? {
        return with(element as ParentDataModifier) {
            modifyParentData(parentData)
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        (element as OnGloballyPositionedModifier).onGloballyPositioned(coordinates)
    }

    override fun onRemeasured(size: IntSize) {
        val element = element
        if (element is OnRemeasuredModifier) {
            element.onRemeasured(size)
        }
    }

    private var lastOnPlacedCoordinates: LayoutCoordinates? = null
    override fun onPlaced(coordinates: LayoutCoordinates) {
        lastOnPlacedCoordinates = coordinates
        val element = element
        if (element is OnPlacedModifier) {
            element.onPlaced(coordinates)
        }
    }

    // todo: jonasluo
//    override fun onFocusEvent(focusState: FocusState) {
//        val focusEventModifier = element
//        check(focusEventModifier is FocusEventModifier) { "onFocusEvent called on wrong node" }
//        focusEventModifier.onFocusEvent(focusState)
//    }

//    override fun applyFocusProperties(focusProperties: FocusProperties) {
//        val focusOrderModifier = element
//        check(focusOrderModifier is FocusOrderModifier) {
//            "applyFocusProperties called on wrong node"
//        }
//
//        @Suppress("DEPRECATION")
//        focusOrderModifier.populateFocusOrder(FocusOrder(focusProperties))
//    }

    override fun toString(): String = element.toString()
}

private val DetachedModifierLocalReadScope = object : ModifierLocalReadScope {
    override val <T> ModifierLocal<T>.current: T
        get() = defaultFactory()
}

private val onDrawCacheReadsChanged = { it: BackwardsCompatNode ->
    it.onDrawCacheReadsChanged()
}

private val updateModifierLocalConsumer = { it: BackwardsCompatNode ->
    it.updateModifierLocalConsumer()
}

private fun BackwardsCompatNode.isChainUpdate(): Boolean {
    val tailNode = requireLayoutNode().nodes.tail as TailModifierNode
    return tailNode.attachHasBeenRun
}
