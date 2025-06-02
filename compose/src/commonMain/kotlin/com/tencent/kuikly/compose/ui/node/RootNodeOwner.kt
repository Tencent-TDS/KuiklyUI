/*
 * Copyright 2023 The Android Open Source Project
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

@file:OptIn(InternalComposeUiApi::class)

package com.tencent.kuikly.compose.ui.node

import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.InternalComposeUiApi
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.focus.FocusOwner
import com.tencent.kuikly.compose.ui.focus.FocusOwnerImpl
import com.tencent.kuikly.compose.ui.focus.FocusRequester
import com.tencent.kuikly.compose.ui.focus.focusProperties
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Canvas
import com.tencent.kuikly.compose.ui.graphics.Matrix
import com.tencent.kuikly.compose.ui.input.InputMode
import com.tencent.kuikly.compose.ui.input.pointer.PointerEventType
import com.tencent.kuikly.compose.ui.input.pointer.PointerInputEvent
import com.tencent.kuikly.compose.ui.input.pointer.PointerInputEventProcessor
import com.tencent.kuikly.compose.ui.input.pointer.PositionCalculator
import com.tencent.kuikly.compose.ui.layout.RootMeasurePolicy
import com.tencent.kuikly.compose.ui.modifier.ModifierLocalManager
import com.tencent.kuikly.compose.ui.platform.KuiklySoftwareKeyboardController
import com.tencent.kuikly.compose.ui.platform.PlatformContext
import com.tencent.kuikly.compose.ui.platform.RenderNodeLayer
import com.tencent.kuikly.compose.ui.scene.ComposeScene
import com.tencent.kuikly.compose.ui.scene.ComposeSceneInputHandler
import com.tencent.kuikly.compose.ui.semantics.EmptySemanticsElement
import com.tencent.kuikly.compose.ui.semantics.EmptySemanticsModifier
import com.tencent.kuikly.compose.ui.unit.Constraints
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.unit.LayoutDirection
import com.tencent.kuikly.compose.ui.unit.toIntRect
import com.tencent.kuikly.compose.ui.unit.toRect
import com.tencent.kuikly.compose.ui.util.fastAll
import com.tencent.kuikly.core.base.DeclarativeBaseView
import kotlin.coroutines.CoroutineContext

/**
 * Owner of root [LayoutNode].
 *
 * It hides [Owner]/[RootForTest] implementations, but provides everything that
 * [ComposeScene] need.
 */
internal class RootNodeOwner(
    density: Density,
    layoutDirection: LayoutDirection,
    size: IntSize?,
    coroutineContext: CoroutineContext,
    val rootKView: DeclarativeBaseView<*, *>,
    val platformContext: PlatformContext,
    private val snapshotInvalidationTracker: SnapshotInvalidationTracker,
    private val inputHandler: ComposeSceneInputHandler,
    internal val scene: ComposeScene,
) {
    val focusOwner: FocusOwner =
        FocusOwnerImpl(
            onRequestFocusForOwner = { _, _ ->
                platformContext.requestFocus()
            },
            onRequestApplyChangesListener = {
                owner.registerOnEndApplyChangesListener(it)
            },
            // onMoveFocusInterop's purpose is to move focus inside embed interop views.
            // Another logic is used in our child-interop views (SwingPanel, etc)
            onMoveFocusInterop = { false },
            onFocusRectInterop = { null },
            onLayoutDirection = { _layoutDirection },
            onClearFocusForOwner = {
                platformContext.parentFocusManager.clearFocus(true)
            },
        )
//    val dragAndDropOwner = DragAndDropOwner(platformContext.dragAndDropManager)

    private val rootSemanticsNode = EmptySemanticsModifier()
    private val snapshotObserver = snapshotInvalidationTracker.snapshotObserver()

    private val rootModifier =
        EmptySemanticsElement
            .focusProperties {
                @OptIn(ExperimentalComposeUiApi::class)
                exit = {
                    // if focusDirection is forward/backward,
                    // it will move the focus after/before ComposePanel
                    if (platformContext.parentFocusManager.moveFocus(it)) {
                        FocusRequester.Cancel
                    } else {
                        FocusRequester.Default
                    }
                }
            }.then(focusOwner.modifier)

//        .then(dragAndDropOwner.modifier)
//        .semantics {
//            // This makes the reported role of the root node "PANEL", which is ignored by VoiceOver
//            // (which is what we want).
//            isTraversalGroup = true
//        }
    val owner: Owner = OwnerImpl(layoutDirection, coroutineContext, rootKView, density)

//    val semanticsOwner = SemanticsOwner(owner.root, rootSemanticsNode)
    var size: IntSize? = size
        set(value) {
            field = value
            onRootConstrainsChanged(value?.toConstraints())
        }
    var density by mutableStateOf(density)

    private var _layoutDirection by mutableStateOf(layoutDirection)
    var layoutDirection: LayoutDirection
        get() = _layoutDirection
        set(value) {
            _layoutDirection = value
            owner.root.layoutDirection = value
        }

//    private val rootForTest = PlatformRootForTestImpl()
    private val pointerInputEventProcessor = PointerInputEventProcessor(owner.root)
    private val measureAndLayoutDelegate = MeasureAndLayoutDelegate(owner.root)
    private var isDisposed = false

    init {
        snapshotObserver.startObserving()
        owner.root.attach(owner)
//        platformContext.rootForTestListener?.onRootForTestCreated(rootForTest)
        onRootConstrainsChanged(size?.toConstraints())
    }

    fun dispose() {
        check(!isDisposed) { "RootNodeOwner is already disposed" }
//        platformContext.rootForTestListener?.onRootForTestDisposed(rootForTest)
        snapshotObserver.stopObserving()
//        graphicsContext.dispose()
        // we don't need to call root.detach() because root will be garbage collected
        isDisposed = true
    }

    private var needClearObservations = false

    private fun clearInvalidObservations() {
        if (needClearObservations) {
            snapshotObserver.clearInvalidObservations()
            needClearObservations = false
        }
    }

    /**
     * Provides a way to measure Owner's content in given [constraints]
     * Draw/pointer and other callbacks won't be called here like in [measureAndLayout] functions
     */
    fun measureInConstraints(constraints: Constraints): IntSize {
        try {
            // TODO: is it possible to measure without reassigning root constraints?
            measureAndLayoutDelegate.updateRootConstraintsWithInfinityCheck(constraints)
            measureAndLayoutDelegate.measureOnly()

            // Don't use mainOwner.root.width here, as it strictly coerced by [constraints]
            val children = owner.root.children
            return IntSize(
                width = children.maxOfOrNull { it.outerCoordinator.measuredWidth } ?: 0,
                height = children.maxOfOrNull { it.outerCoordinator.measuredHeight } ?: 0,
            )
        } finally {
            measureAndLayoutDelegate.updateRootConstraintsWithInfinityCheck(constraints)
        }
    }

    fun measureAndLayout() {
        owner.measureAndLayout(sendPointerUpdate = true)
    }

    fun invalidatePositionInWindow() {
        owner.root.layoutDelegate.measurePassDelegate
            .notifyChildrenUsingCoordinatesWhilePlacing()
        measureAndLayoutDelegate.dispatchOnPositionedCallbacks(forceDispatch = true)
    }

    fun draw(canvas: Canvas) {
        owner.root.draw(
            canvas = canvas,
//            graphicsLayer = null // the root node will provide the root graphics layer
        )
        clearInvalidObservations()
    }

    fun setRootModifier(modifier: Modifier) {
        owner.root.modifier = rootModifier then modifier
    }

    private fun onRootConstrainsChanged(constraints: Constraints?) {
        measureAndLayoutDelegate.updateRootConstraintsWithInfinityCheck(constraints)
        if (measureAndLayoutDelegate.hasPendingMeasureOrLayout) {
            snapshotInvalidationTracker.requestMeasureAndLayout()
        }
    }

    @OptIn(InternalCoreApi::class, ExperimentalComposeUiApi::class)
    fun onPointerInput(event: PointerInputEvent) {
        if (event.button != null) {
            platformContext.inputModeManager.requestInputMode(InputMode.Touch)
        }
        val isInBounds =
            event.eventType != PointerEventType.Exit &&
                event.pointers.fastAll { isInBounds(it.position) }
        pointerInputEventProcessor.process(
            event,
            IdentityPositionCalculator,
            isInBounds = isInBounds,
            onProcessResult = {
                inputHandler.setProcessResult(it)
            },
        )
    }

    private fun isInBounds(localPosition: Offset): Boolean = size?.toIntRect()?.toRect()?.contains(localPosition) ?: true

    private inner class OwnerImpl(
        layoutDirection: LayoutDirection,
        override val coroutineContext: CoroutineContext,
        val rootKView: DeclarativeBaseView<*, *>,
        val densityIn: Density,
    ) : Owner {
        override val root =
            KNode(rootKView).also {
                it.density = densityIn
                it.layoutDirection = layoutDirection
                it.measurePolicy = RootMeasurePolicy
                it.modifier = rootModifier
            }

        override val sharedDrawScope = LayoutNodeDrawScope()
        override val rootForTest get() = TODO("Not yet implemented")
        override val inputModeManager get() = platformContext.inputModeManager
        override val density get() = this@RootNodeOwner.density
        override val softwareKeyboardController =
            KuiklySoftwareKeyboardController()
        override val focusOwner get() = this@RootNodeOwner.focusOwner

//        @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
//        override val fontLoader = androidx.compose.ui.text.platform.FontLoader()
//        override val fontFamilyResolver = createFontFamilyResolver()
        override val layoutDirection get() = _layoutDirection
        override var showLayoutBounds = false
            @InternalCoreApi
            set

        override val modifierLocalManager = ModifierLocalManager(this)
        override val snapshotObserver get() = this@RootNodeOwner.snapshotObserver
        override val viewConfiguration get() = platformContext.viewConfiguration
        override val measureIteration: Long get() = measureAndLayoutDelegate.measureIteration

        override fun onAttach(node: LayoutNode) = Unit

        override fun onDetach(node: LayoutNode) {
            measureAndLayoutDelegate.onNodeDetached(node)
            snapshotObserver.clear(node)
            needClearObservations = true
        }

        override fun measureAndLayout(sendPointerUpdate: Boolean) {
            // only run the logic when we have something pending
            if (measureAndLayoutDelegate.hasPendingMeasureOrLayout ||
                measureAndLayoutDelegate.hasPendingOnPositionedCallbacks
            ) {
//                 {
                val resend = if (sendPointerUpdate) inputHandler::onPointerUpdate else null
                val rootNodeResized = measureAndLayoutDelegate.measureAndLayout(resend)
                if (rootNodeResized) {
                    snapshotInvalidationTracker.requestDraw()
                }
                measureAndLayoutDelegate.dispatchOnPositionedCallbacks()
//                }
            }
        }

        override fun measureAndLayout(
            layoutNode: LayoutNode,
            constraints: Constraints,
        ) {
            measureAndLayoutDelegate.measureAndLayout(layoutNode, constraints)
            inputHandler.onPointerUpdate()
            // only dispatch the callbacks if we don't have other nodes to process as otherwise
            // we will have one more measureAndLayout() pass anyway in the same frame.
            // it allows us to not traverse the hierarchy twice.
            if (!measureAndLayoutDelegate.hasPendingMeasureOrLayout) {
                measureAndLayoutDelegate.dispatchOnPositionedCallbacks()
            }
        }

        override fun forceMeasureTheSubtree(
            layoutNode: LayoutNode,
            affectsLookahead: Boolean,
        ) {
            measureAndLayoutDelegate.forceMeasureTheSubtree(layoutNode, affectsLookahead)
        }

        override fun onRequestMeasure(
            layoutNode: LayoutNode,
            affectsLookahead: Boolean,
            forceRequest: Boolean,
            scheduleMeasureAndLayout: Boolean,
        ) {
            if (affectsLookahead) {
                if (measureAndLayoutDelegate.requestLookaheadRemeasure(layoutNode, forceRequest) &&
                    scheduleMeasureAndLayout
                ) {
                    snapshotInvalidationTracker.requestMeasureAndLayout()
                }
            } else if (measureAndLayoutDelegate.requestRemeasure(layoutNode, forceRequest) &&
                scheduleMeasureAndLayout
            ) {
                snapshotInvalidationTracker.requestMeasureAndLayout()
            }
        }

        override fun onRequestRelayout(
            layoutNode: LayoutNode,
            affectsLookahead: Boolean,
            forceRequest: Boolean,
        ) {
            if (affectsLookahead) {
                if (measureAndLayoutDelegate.requestLookaheadRelayout(layoutNode, forceRequest)) {
                    snapshotInvalidationTracker.requestMeasureAndLayout()
                }
            } else {
                if (measureAndLayoutDelegate.requestRelayout(layoutNode, forceRequest)) {
                    snapshotInvalidationTracker.requestMeasureAndLayout()
                }
            }
        }

        override fun requestOnPositionedCallback(layoutNode: LayoutNode) {
            measureAndLayoutDelegate.requestOnPositionedCallback(layoutNode)
            snapshotInvalidationTracker.requestMeasureAndLayout()
        }

        override fun createLayer(
            drawBlock: (canvas: Canvas) -> Unit,
            invalidateParentLayer: () -> Unit,
            view: DeclarativeBaseView<*, *>?,
        ) = /*
            TODO: Use GraphicsLayerOwnerLayer instead of RenderNodeLayer
            GraphicsLayerOwnerLayer(
                graphicsLayer = graphicsContext.createGraphicsLayer(),
                context = graphicsContext,
                drawBlock = drawBlock,
                invalidateParentLayer = {
                    invalidateParentLayer()
                    snapshotInvalidationTracker.requestDraw()
                },
            )
         */
            RenderNodeLayer(
                invalidateParentLayer = {
                    invalidateParentLayer()
                    snapshotInvalidationTracker.requestDraw()
                },
                drawBlock = drawBlock,
                onDestroy = { needClearObservations = true },
            )

        override fun onSemanticsChange() {
//            platformContext.semanticsOwnerListener?.onSemanticsChange(semanticsOwner)
        }

        override fun onZIndexChange(layoutNode: LayoutNode) {
            (layoutNode as? KNode<*>)?.apply {
                view.getViewAttr().apply {
                    zIndex((layoutNode.innerCoordinator.zIndex * 10000).toInt())
                }
            }
        }

        override fun onLayoutChange(layoutNode: LayoutNode) {
//            platformContext.semanticsOwnerListener?.onLayoutChange(
//                semanticsOwner = semanticsOwner,
//                semanticsNodeId = layoutNode.semanticsId
//            )
        }

        private val endApplyChangesListeners = mutableVectorOf<(() -> Unit)?>()

        override fun onEndApplyChanges() {
            clearInvalidObservations()

            // Listeners can add more items to the list and we want to ensure that they
            // are executed after being added, so loop until the list is empty
            while (endApplyChangesListeners.isNotEmpty()) {
                val size = endApplyChangesListeners.size
                for (i in 0 until size) {
                    val listener = endApplyChangesListeners[i]
                    // null out the item so that if the listener is re-added then we execute it again.
                    endApplyChangesListeners[i] = null
                    listener?.invoke()
                }
                // Remove all the items that were visited. Removing items shifts all items after
                // to the front of the list, so removing in a chunk is cheaper than removing one-by-one
                endApplyChangesListeners.removeRange(0, size)
            }
        }

        override fun registerOnEndApplyChangesListener(listener: () -> Unit) {
            if (listener !in endApplyChangesListeners) {
                endApplyChangesListeners += listener
            }
        }

        override fun registerOnLayoutCompletedListener(listener: Owner.OnLayoutCompletedListener) {
            measureAndLayoutDelegate.registerOnLayoutCompletedListener(listener)
            snapshotInvalidationTracker.requestMeasureAndLayout()
        }
    }
}

// TODO a proper way is to provide API in Constraints to get this value

/**
 * Equals [Constraints.MinNonFocusMask]
 */
private const val ConstraintsMinNonFocusMask = 0x7FFF // 32767

/**
 * The max value that can be passed as Constraints(0, LargeDimension, 0, LargeDimension)
 *
 * Greater values cause "Can't represent a width of".
 * See [Constraints.createConstraints] and [Constraints.bitsNeedForSize]:
 *  - it fails if `widthBits + heightBits > 31`
 *  - widthBits/heightBits are greater than 15 if we pass size >= [Constraints.MinNonFocusMask]
 */
internal const val LargeDimension = ConstraintsMinNonFocusMask - 1

/**
 * After https://android-review.googlesource.com/c/platform/frameworks/support/+/2901556
 * Compose core doesn't allow measuring in infinity constraints,
 * but RootNodeOwner and ComposeScene allow passing Infinity constraints by contract
 * (Android on the other hand doesn't have public API for that and don't have such an issue).
 *
 * This method adds additional check on Infinity constraints,
 * and pass constraint large enough instead
 */
private fun MeasureAndLayoutDelegate.updateRootConstraintsWithInfinityCheck(constraints: Constraints?) {
    updateRootConstraints(
        constraints =
            Constraints(
                minWidth = constraints?.minWidth ?: 0,
                maxWidth =
                    if (constraints != null && constraints.hasBoundedWidth) {
                        constraints.maxWidth
                    } else {
                        LargeDimension
                    },
                minHeight = constraints?.minHeight ?: 0,
                maxHeight =
                    if (constraints != null && constraints.hasBoundedHeight) {
                        constraints.maxHeight
                    } else {
                        LargeDimension
                    },
            ),
    )
}

private fun IntSize.toConstraints() = Constraints(maxWidth = width, maxHeight = height)

private object IdentityPositionCalculator : PositionCalculator {
    override fun screenToLocal(positionOnScreen: Offset): Offset = positionOnScreen

    override fun localToScreen(localPosition: Offset): Offset = localPosition

    override fun localToScreen(localTransform: Matrix) = Unit
}
