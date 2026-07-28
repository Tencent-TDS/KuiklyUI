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

package com.tencent.kuikly.compose.foundation.relocation

import com.tencent.kuikly.compose.foundation.gestures.wasFocusedChildClippedByViewportShrink
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Rect
import com.tencent.kuikly.compose.ui.layout.LayoutCoordinates
import com.tencent.kuikly.compose.ui.layout.boundsInRoot
import com.tencent.kuikly.compose.ui.layout.findRootCoordinates
import com.tencent.kuikly.compose.ui.node.GlobalPositionAwareModifierNode
import com.tencent.kuikly.compose.ui.node.ModifierNodeElement
import com.tencent.kuikly.compose.ui.node.TraversableNode
import com.tencent.kuikly.compose.ui.platform.InspectorInfo
import kotlinx.coroutines.launch
import kotlin.math.abs

internal interface BringIntoViewResponder {
    fun onFocusedBoundsChanged(focusedBounds: LayoutCoordinates?)
    suspend fun bringChildIntoView(childCoordinates: LayoutCoordinates, rect: Rect? = null)
}

internal class BringIntoViewResponderCoordinator {
    private var responder: BringIntoViewResponder? = null

    fun bind(responder: BringIntoViewResponder?) {
        this.responder = responder
    }

    fun onFocusedBoundsChanged(focusedBounds: LayoutCoordinates?) {
        responder?.onFocusedBoundsChanged(focusedBounds)
    }
}

internal abstract class BringIntoViewResponderNode(
    responderCoordinator: BringIntoViewResponderCoordinator,
    imeBottomPx: Float,
) : Modifier.Node(), BringIntoViewResponder, TraversableNode, GlobalPositionAwareModifierNode {
    override val traverseKey: Any = TraverseKey

    private var responderCoordinator: BringIntoViewResponderCoordinator = responderCoordinator
    private var imeBottomPx: Float = imeBottomPx

    // --- Path A (FocusedBounds tracking) ---
    // Aligned with official ContentInViewNode.focusedChild: only records the focused child,
    // does NOT immediately schedule a request. Scrolling from path A is triggered only when
    // the viewport shrinks and the focused child goes from visible to clipped.
    private var focusedChild: LayoutCoordinates? = null

    // --- Path B (explicit BringIntoViewRequester request) ---
    // A one-shot request target set by bringChildIntoView(). Takes priority over focusedChild.
    // Cleared after processRequests() completes.
    // [rect] is in local coordinates of the requester node; null means use entire node bounds.
    private data class RequestTarget(val coordinates: LayoutCoordinates, val rect: Rect? = null)
    private var requestTarget: RequestTarget? = null

    private var containerCoordinates: LayoutCoordinates? = null
    private var previousViewportHeight: Float = 0f
    private var isProcessingRequest = false
    private var hasPendingRequest = false

    override fun onAttach() {
        responderCoordinator.bind(this)
    }

    override fun onDetach() {
        responderCoordinator.bind(null)
        focusedChild = null
        requestTarget = null
        containerCoordinates = null
        previousViewportHeight = 0f
        hasPendingRequest = false
        isProcessingRequest = false
    }

    fun update(
        responderCoordinator: BringIntoViewResponderCoordinator,
        imeBottomPx: Float,
    ) {
        if (this.responderCoordinator !== responderCoordinator) {
            this.responderCoordinator.bind(null)
            this.responderCoordinator = responderCoordinator
            if (isAttached) {
                this.responderCoordinator.bind(this)
            }
        }
        if (this.imeBottomPx != imeBottomPx) {
            this.imeBottomPx = imeBottomPx
            // IME height change effectively shrinks the visible viewport.
            // Trigger path-A viewport-shrink check (aligned with official onRemeasured).
            checkViewportShrinkAndSchedule()
        }
    }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        containerCoordinates = coordinates
        // Detect viewport shrink (e.g. keyboard appearing with ADJUST_RESIZE).
        // Aligned with official ContentInViewNode.onRemeasured: only trigger path-A
        // compensation when the focused child was visible before but is now clipped.
        checkViewportShrinkAndSchedule()
    }

    override fun onFocusedBoundsChanged(focusedBounds: LayoutCoordinates?) {
        // Path A: only record the focused child, do NOT immediately schedule a request.
        // Scrolling from path A is triggered by viewport shrink detection, not by
        // focus bounds change alone. This aligns with official ContentInViewNode
        // which only stores focusedChild in onFocusBoundsChanged().
        focusedChild = focusedBounds
    }

    override suspend fun bringChildIntoView(childCoordinates: LayoutCoordinates, rect: Rect?) {
        // Path B: set the one-shot request target and trigger processing.
        // Takes priority over focusedChild (path A).
        requestTarget = RequestTarget(childCoordinates, rect)
        processRequests()
        // Clear the one-shot request after processing.
        requestTarget = null
    }

    protected open fun viewportRect(containerCoordinates: LayoutCoordinates): Rect =
        containerCoordinates.boundsInRoot()

    protected open fun mapScrollDelta(delta: Float): Float = delta

    protected abstract suspend fun stopOngoingScroll()

    protected abstract suspend fun performScroll(delta: Float)

    private fun scheduleRequest() {
        // Path B (requestTarget) takes priority; fall back to path A (focusedChild).
        val target = requestTarget?.coordinates?.takeIf { it.isAttached }
            ?: focusedChild?.takeIf { it.isAttached }
            ?: return
        if (!isAttached) return
        if (isProcessingRequest) {
            hasPendingRequest = true
            return
        }
        coroutineScope.launch {
            processRequests()
        }
    }

    /**
     * Detects viewport shrink (e.g. keyboard appearing) and triggers path-A compensation
     * scrolling if the focused child was previously visible but is now clipped.
     *
     * Aligned with official ContentInViewNode.onRemeasured:
     * - Only triggers when viewport height decreased.
     * - Only triggers when focusedChild was fully visible before but is now partially clipped.
     * - Does NOT trigger when viewport grew (e.g. keyboard dismissing).
     */
    /**
     * Effective visible height of the viewport after IME clipping.
     *
     * Aligned with official ContentInViewNode.onRemeasured semantics, but generalized:
     * official uses the container's own size (works with ADJUST_RESIZE where the container
     * physically shrinks when the IME appears). Kuikly Android keeps the container size
     * unchanged in overlay IME mode, so the IME inset must be subtracted explicitly,
     * otherwise the viewport-shrink check would never fire.
     */
    private fun effectiveVisibleHeight(container: LayoutCoordinates): Float {
        val containerRect = viewportRect(container)
        val windowBottom = container.findRootCoordinates().boundsInRoot().bottom
        val visibleBottom = minOf(containerRect.bottom, windowBottom - imeBottomPx.coerceAtLeast(0f))
        return (visibleBottom - containerRect.top).coerceAtLeast(0f)
    }

    private fun checkViewportShrinkAndSchedule() {
        val container = containerCoordinates?.takeIf { it.isAttached } ?: return
        // Track the IME-clipped effective visible height instead of the raw container height,
        // so that IME changes (overlay mode, container size unchanged) are also detected.
        val currentViewportHeight = effectiveVisibleHeight(container)
        val oldHeight = previousViewportHeight
        previousViewportHeight = currentViewportHeight

        // Don't care if the viewport grew. Skip silently when unchanged to avoid log spam.
        if (oldHeight <= 0f || currentViewportHeight >= oldHeight) {
            return
        }

        val focused = focusedChild?.takeIf { it.isAttached } ?: return
        if (!focused.isAttached || !container.isAttached) return

        val focusedRect = container.localBoundingBoxOf(focused, clipBounds = false)
        val oldViewport = Rect(0f, 0f, container.size.width.toFloat(), oldHeight)
        val newViewport = Rect(0f, 0f, container.size.width.toFloat(), currentViewportHeight)

        if (wasFocusedChildClippedByViewportShrink(focusedRect, oldViewport, newViewport)) {
            scheduleRequest()
        }
    }

    private suspend fun processRequests() {
        if (isProcessingRequest) {
            hasPendingRequest = true
            return
        }
        isProcessingRequest = true
        try {
            var attempt = 0
            do {
                hasPendingRequest = false
                // Path B (requestTarget) takes priority; fall back to path A (focusedChild).
                val activeRequest = requestTarget
                val targetCoordinates = activeRequest?.coordinates?.takeIf { it.isAttached }
                    ?: focusedChild?.takeIf { it.isAttached }
                if (targetCoordinates == null) {
                    break
                }
                val delta = calculateScrollDelta(targetCoordinates, activeRequest?.rect)
                if (delta == null) {
                    break
                }
                if (abs(delta) <= DefaultBringIntoViewThresholdPx) {
                    break
                }
                stopOngoingScroll()
                performScroll(mapScrollDelta(delta))
                attempt++
                if (attempt >= 2) {
                    break
                }
                val recheckRequest = requestTarget
                val recheckCoordinates = recheckRequest?.coordinates?.takeIf { it.isAttached }
                    ?: focusedChild?.takeIf { it.isAttached }
                    ?: targetCoordinates
                val remainingDelta = calculateScrollDelta(recheckCoordinates, recheckRequest?.rect)
                if (remainingDelta == null || abs(remainingDelta) <= DefaultBringIntoViewThresholdPx) {
                    break
                }
                stopOngoingScroll()
                performScroll(mapScrollDelta(remainingDelta))
                attempt++
            } while (hasPendingRequest && attempt < 2)
        } finally {
            isProcessingRequest = false
            // Clear the one-shot path-B request target.
            requestTarget = null
            if (hasPendingRequest) {
                hasPendingRequest = false
                scheduleRequest()
            }
        }
    }

    private fun calculateScrollDelta(targetCoordinates: LayoutCoordinates, localRect: Rect? = null): Float? {
        val containerCoordinates = containerCoordinates?.takeIf { it.isAttached } ?: return null
        if (!targetCoordinates.isAttached) {
            return null
        }
        val containerRect = viewportRect(containerCoordinates)
        // Use the unclipped bounds of the target (clipBounds = false) so that a partially
        // visible child (e.g. clipped by the LazyColumn viewport) reports its full geometry.
        // Aligned with path-A's focusedChild rect which also uses clipBounds = false.
        // With clipped boundsInRoot(), a half-visible child would be wrongly treated as
        // fully visible and both paths would no-op.
        val localBounds = containerCoordinates.localBoundingBoxOf(targetCoordinates, clipBounds = false)
        val nodeBounds = Rect(
            left = localBounds.left + containerRect.left,
            top = localBounds.top + containerRect.top,
            right = localBounds.right + containerRect.left,
            bottom = localBounds.bottom + containerRect.top,
        )
        // If a local rect is provided (e.g. cursor rect), translate it to root coordinates
        // by offsetting from the node's root position.
        val targetRect = if (localRect != null) {
            Rect(
                left = nodeBounds.left + localRect.left,
                top = nodeBounds.top + localRect.top,
                right = nodeBounds.left + localRect.right,
                bottom = nodeBounds.top + localRect.bottom,
            )
        } else {
            nodeBounds
        }
        if (targetRect == Rect.Zero || containerRect == Rect.Zero) {
            return null
        }
        val windowBottom = containerCoordinates.findRootCoordinates().boundsInRoot().bottom
        val delta = calculateBringIntoViewDelta(
            targetRect = targetRect,
            containerRect = containerRect,
            windowBottom = windowBottom,
            imeBottomPx = imeBottomPx,
        )
        return delta
    }

    companion object TraverseKey
}

internal data class BringIntoViewResponderModifierElement<T : BringIntoViewResponderNode>(
    private val nodeFactory: () -> T,
    private val nodeUpdater: (T) -> Unit,
) : ModifierNodeElement<T>() {
    override fun create(): T = nodeFactory()

    override fun update(node: T) {
        nodeUpdater(node)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "bringIntoViewResponder"
    }
}
