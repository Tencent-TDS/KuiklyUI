/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
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

package com.tencent.kuikly.compose.ui.node

import androidx.compose.runtime.CompositionLocalMap
import com.tencent.kuikly.compose.extension.approximatelyEqual
import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.ui.geometry.CornerRadius
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.geometry.RoundRect
import com.tencent.kuikly.compose.ui.geometry.isRect
import com.tencent.kuikly.compose.ui.graphics.Canvas
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Matrix
import com.tencent.kuikly.compose.ui.graphics.isIdentity
import com.tencent.kuikly.compose.ui.layout.LayoutCoordinates
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.text.style.modulate
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.views.KuiklyInfoKey
import com.tencent.kuikly.compose.views.VirtualNodeView
import com.tencent.kuikly.compose.gestures.KuiklyScrollInfo
import com.tencent.kuikly.compose.layout.resetViewVisible
import com.tencent.kuikly.compose.node.extPropsVar
import com.tencent.kuikly.compose.ui.KuiklyPath
import com.tencent.kuikly.compose.ui.graphics.Path
import com.tencent.kuikly.compose.ui.layout.LookaheadLayoutCoordinates
import com.tencent.kuikly.compose.ui.unit.plus
import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.Attr.StyleConst
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.PropValuePath
import com.tencent.kuikly.core.base.Rotate
import com.tencent.kuikly.core.base.Scale
import com.tencent.kuikly.core.base.Translate
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.domChildren
import com.tencent.kuikly.core.base.event.notifyLayoutFrameDidChange
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.views.DivView
import com.tencent.kuikly.core.views.HoverView
import com.tencent.kuikly.core.views.ModalView
import com.tencent.kuikly.core.views.ScrollerView
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal class KNode<T : DeclarativeBaseView<*, *>>(
    val view: T,
    override var isVirtual: Boolean = false,
    var init: T.() -> Unit = {}
) : LayoutNode(isVirtual = isVirtual) {

    var kuiklyCoordinates: LayoutCoordinates? = null

    /**
     * Determines if the current node is a sticky header
     * Any HoverView type is considered a sticky header, not limited to currently active ones
     */
    private fun isStickyHeaderNode(): Boolean {
        return view is HoverView
    }

    /**
     * Gets cached position for sticky header
     * Uses StickyHeaderCacheManager bound to KuiklyScrollInfo to manage cache
     */
    private fun getCachedStickyPosition(currentPos: Offset): Offset {
        val currentItemKey = (foldedParent as? KNode<*>)?.lazyItemKey ?: return currentPos

        // Get bound cache manager
        val cacheManager = getCacheManager()

        return cacheManager?.getCachedStickyPosition(currentItemKey, currentPos) ?: currentPos
    }

    /**
     * Gets StickyHeaderCacheManager instance
     * Gets bound cache manager from KuiklyScrollInfo
     */
    private fun getCacheManager(): StickyHeaderCacheManager? {
        val scrollNode = parent as? KNode<*>
        val kuiklyInfo = scrollNode?.view?.extProps?.get(KuiklyInfoKey) as? KuiklyScrollInfo

        return kuiklyInfo?.stickyHeaderCacheManager
    }


    /**
     * The key of the Lazy item that this KNode represents
     * This is used to identify which item this node corresponds to
     */
    var lazyItemKey: Any? = null

    var viewVisible: Boolean? = null
    var needFixScrollOffset = true

    var update: () -> Unit = {}
        set(value) {
            field = value
            runUpdate()
        }

    private val runUpdate: () -> Unit = {
        if (isAttached) {
            requireOwner().snapshotObserver.observeReads(this, {
                update()
            }, update)
        }
    }

    override fun attach(owner: Owner) {
        super.attach(owner)
        runUpdate()
    }

    override fun detach() {
        requireOwner().snapshotObserver.clear(this)
        super.detach()
    }

    private val currentView: ViewContainer<*, *>
        get() {
            return (view as ViewContainer<*, *>).realContainerView()
        }

    override var compositionLocalMap = CompositionLocalMap.Empty
        set(value) {
            field = value
            density = value[LocalDensity]
            nodes.headToTail(Nodes.CompositionLocalConsumer) { modifierNode ->
                val delegatedNode = modifierNode.node
                if (delegatedNode.isAttached) {
                    autoInvalidateUpdatedNode(delegatedNode)
                } else {
                    delegatedNode.updatedNodeAwaitingAttachForInvalidation = true
                }
            }
        }

    // Construct Kuikly node tree
    fun insertTopDown(index: Int, instance: KNode<DeclarativeBaseView<*, *>>) {
        val childView = instance.view
        currentView.addChild(childView, instance.init, index)
        currentView.insertDomSubView(childView, index)
    }

    override fun move(from: Int, to: Int, count: Int) {
        currentView.move(from, to, count)
        super.move(from, to, count)
    }

    override fun removeAll() {
        currentView.domChildren().forEach { child ->
            currentView.removeDomSubView(child)
        }
        currentView.removeChildren()
        super.removeAll()
    }

    override fun removeAt(index: Int, count: Int) {
        currentView.removeChildrenAt(index, count)
        super.removeAt(index, count)
    }

    private var drawInvalidated = true

    override fun draw(canvas: Canvas) {
        if (!drawInvalidated) {
            return
        }
        drawInvalidated = false
        view.reset()
        canvas.view = view
        super.draw(canvas)
        canvas.view = null
        view.flush()
    }

    override fun invalidateDraw() {
        if (!drawInvalidated) {
            drawInvalidated = true
            parent?.invalidateDraw()
        }
    }

    override fun onWillStartMeasure() {
        super.onWillStartMeasure()
        kuiklyCoordinates = null
    }

    private fun LayoutCoordinates.toCoordinator() =
        (this as? LookaheadLayoutCoordinates)?.coordinator ?: this as NodeCoordinator

    private fun LayoutCoordinates.viewPositionOf(coordinator: LayoutCoordinates): Offset {
        var nodeCoordinator = coordinator.toCoordinator()
        var position = Offset.Zero
        while (nodeCoordinator !== this) {
            position += nodeCoordinator.position
            nodeCoordinator = nodeCoordinator.wrappedBy!!
        }
        return position
    }

    override fun updateKuiklyViewFrame(coordinator: LayoutCoordinates) {
        val curCoordinator = kuiklyCoordinates ?: innerCoordinator

        resetViewVisible()

        val ksScrollSubView = view.parent is VirtualNodeView
        val parentNode = parent as? KNode<*>
        val parentCoordinator = parentNode?.kuiklyCoordinates ?: parentNode?.innerCoordinator
        var pos = parentCoordinator?.viewPositionOf(curCoordinator) ?: Offset.Zero

        // Child nodes on scrollview, parent is virtual node
        if (ksScrollSubView && needFixScrollOffset) {
            val scrollerView = (parent as KNode<*>).view
            (scrollerView.extProps[KuiklyInfoKey] as? KuiklyScrollInfo)?.apply {
                val deltaOffset = composeOffset
                pos = if (orientation == Orientation.Vertical) {
                    Offset(pos.x, pos.y + deltaOffset)
                } else {
                    Offset(pos.x + deltaOffset, pos.y)
                }
            }
        }

        val densityValue = view.getPager().pagerDensity()

        // Check if it's a sticky header and handle position caching
        val isStickyHeader = isStickyHeaderNode()
        if (isStickyHeader) {
            pos = getCachedStickyPosition(pos)
        }

        var newFrame = Frame(
            x = pos.x / densityValue,
            y = pos.y / densityValue,
            width = curCoordinator.size.width.toFloat() / densityValue,
            height = curCoordinator.size.height.toFloat() / densityValue,
        )

        // Special handling for modalView
        if (view is ModalView) {
            newFrame = Frame(
                0f,
                0f,
                requireOwner().root.width.toFloat() / densityValue,
                requireOwner().root.height.toFloat() / densityValue,
            )
        }

        view.updateFrame(newFrame)
    }

    /**
     * Corrects composeOffset when ScrollView height changes
     * Mainly handles the following scenarios:
     * 1. When currently scrolled to the bottom and height becomes shorter, adjust offset to avoid exceeding boundaries
     * 2. When there is current offset but height increases, scrolling may no longer be needed, set offset to 0
     */
    private fun updateScrollViewOffset(curFrame: Frame, newFrame: Frame) {
        if (view !is ScrollerView<*, *>) {
            return
        }

        val scrollerView = view as ScrollerView<*, *>
        val kuiklyInfo = scrollerView.extProps[KuiklyInfoKey] as? KuiklyScrollInfo ?: return

        if (curFrame != newFrame) {
            kuiklyInfo.offsetDirty = true
        }

        // Get current and new height
        val curHeight = if (kuiklyInfo.isVertical()) curFrame.height else curFrame.width
        val newHeight = if (kuiklyInfo.isVertical()) newFrame.height else newFrame.width

        // If height hasn't changed, no correction needed
        if (curHeight == newHeight) {
            return
        }

        // Get current scroll offset - convert to pixel units
        val currentOffset = if (kuiklyInfo.isVertical()) {
            (scrollerView.curOffsetY * kuiklyInfo.getDensity()).toInt()
        } else {
            (scrollerView.curOffsetX * kuiklyInfo.getDensity()).toInt()
        }

        // Calculate maximum scrollable distance - use pixel units, consistent with composeOffset
        val currentContentSize = kuiklyInfo.currentContentSize // already in pixel units
        val viewportSize = if (kuiklyInfo.isVertical()) {
            (newHeight * kuiklyInfo.getDensity()).toInt()
        } else {
            (newFrame.width * kuiklyInfo.getDensity()).toInt()
        }

        // Handle edge cases: if contentSize is 0 or viewportSize is 0, no scrolling needed
        if (currentContentSize <= 0) {
            kuiklyInfo.composeOffset = 0f
            return
        }

        val maxScrollOffset = maxOf(0, currentContentSize - viewportSize)

        // Correct composeOffset - use pixel units
        val correctedOffset = when {
            // If currently scrolled to bottom and height becomes shorter, adjust offset
            currentOffset >= maxScrollOffset && newHeight < curHeight -> {
                val newMaxScrollOffset = maxOf(0, currentContentSize - viewportSize)
                minOf(currentOffset, newMaxScrollOffset)
            }
            // If there is current offset but height increases, check if adjustment is needed
            currentOffset > 0 && newHeight > curHeight -> {
                val newMaxScrollOffset = maxOf(0, currentContentSize - viewportSize)
                if (newMaxScrollOffset <= 0) {
                    0 // no scrolling needed
                } else {
                    // When height increases, keep composeOffset unchanged, but check if it exceeds boundaries
                    if (currentOffset > newMaxScrollOffset) {
                        newMaxScrollOffset // adjust to maximum when exceeding boundaries
                    } else {
                        currentOffset // keep unchanged when within boundaries
                    }
                }
            }
            // Other cases keep current offset
            else -> {
                currentOffset
            }
        }

        // Update composeOffset
        if (correctedOffset != currentOffset) {
            kuiklyInfo.composeOffset = correctedOffset.toFloat()
        }
    }

    private fun DeclarativeBaseView<*, *>.updateFrame(newFrame: Frame) {
        val curFrame = renderView?.currentFrame ?: Frame.zero
        if (!curFrame.approximatelyEqual(newFrame)) {
            val densityFrame = Frame(
                x = newFrame.x,
                y = newFrame.y,
                width =  newFrame.width,
                height = newFrame.height
            )

            updateScrollViewOffset(curFrame, densityFrame)
            setFrameToRenderView(densityFrame)
            getViewEvent().notifyLayoutFrameDidChange(newFrame)
        }
    }

    internal companion object {

        internal val ShadowLayoutConstructor: (Boolean) -> (() -> KNode<*>) = { hasShadow ->
            {
                val view = DivView()
                KNode(view) {
                    attr {
                        flexDirectionColumn()
                        justifyContentCenter()
                        alignItemsCenter()
                        // Disable DivView optimization to ensure normal layout
                        setProp("flatLayer", false)
                        if (hasShadow) {
                            setProp(Attr.StyleConst.WRAPPER_BOX_SHADOW_VIEW, 1)
                        }
                    }
                }
            }
        }
        internal val LayoutConstructor: () -> KNode<DivView> = {
            val view = DivView()
            KNode(view) {
                attr {
                    flexDirectionColumn()
                    justifyContentCenter()
                    alignItemsCenter()
                    // Disable DivView optimization to ensure normal layout
                    setProp("flatLayer", false)
                }
            }
        }

        private fun DeclarativeBaseView<*, *>.getMatrix(): Matrix? = extProps["matrix"] as? Matrix
        private fun DeclarativeBaseView<*, *>.obtainMatrix(): Matrix = extProps.getOrPut("matrix") { Matrix() } as Matrix

        private var DeclarativeBaseView<*, *>.matrixChanged by extPropsVar("matrixChanged") { false }
        private var DeclarativeBaseView<*, *>.alpha by extPropsVar("alpha") { 1f }
        private var DeclarativeBaseView<*, *>.measuredSize by extPropsVar("measuredSize") {
            IntSize(
                0,
                0
            )
        }
        private var DeclarativeBaseView<*, *>.clipPath by extPropsVar("clipPath") { "" }
        private var DeclarativeBaseView<*, *>.borderRadius by extPropsVar("borderRadius") { FloatArray(4) }
        private var DeclarativeBaseView<*, *>.clip by extPropsVar("clip") { false }
        private var DeclarativeBaseView<*, *>.shadowElevation by extPropsVar("shadowElevation") { 0f }
        private var DeclarativeBaseView<*, *>.shadowColor by extPropsVar("shadowColor") { Color.Transparent }
        private var DeclarativeBaseView<*, *>.shadowHasSet by extPropsVar("shadowHasSet") { false }

        fun DeclarativeBaseView<*, *>.measuredSize(width: Int, height: Int) {
            measuredSize = IntSize(width, height)
        }

        fun DeclarativeBaseView<*, *>.translate(x: Float, y: Float) {
            if (x != 0f || y != 0f) {
                obtainMatrix().translate(x, y)
                matrixChanged = true
            }
        }

        fun DeclarativeBaseView<*, *>.rotate(x: Float, y: Float, z: Float) {
            if (x == 0f && y == 0f && z == 0f) {
                return
            }
            val matrix = obtainMatrix()
            if (x != 0f) {
                matrix.rotateX(x)
            }
            if (y != 0f) {
                matrix.rotateY(y)
            }
            if (z != 0f) {
                matrix.rotateZ(z)
            }
            matrixChanged = true
        }

        fun DeclarativeBaseView<*, *>.scale(x: Float, y: Float) {
            if (x != 1f || y != 1f) {
                obtainMatrix().scale(x, y)
                matrixChanged = true
            }
        }

        fun DeclarativeBaseView<*, *>.concat(matrix: Matrix) {
            if (!matrix.isIdentity()) {
                obtainMatrix().timesAssign(matrix)
                matrixChanged = true
            }
        }

        fun DeclarativeBaseView<*, *>.alpha(alpha: Float) {
            if (alpha != 1f) {
                this.alpha *= alpha
            }
        }

        private val CornerRadius.radius: Float get() = min(x, y)
        fun DeclarativeBaseView<*, *>.borderRadius(radius: RoundRect) {
            if (radius.isRect) {
                return
            }
            val array = borderRadius
            array[0] = max(array[0], radius.topLeftCornerRadius.radius)
            array[1] = max(array[1], radius.topRightCornerRadius.radius)
            array[2] = max(array[2], radius.bottomRightCornerRadius.radius)
            array[3] = max(array[3], radius.bottomLeftCornerRadius.radius)
        }

        fun DeclarativeBaseView<*, *>.clipPath(path: KuiklyPath) {
            val densityValue = getPager().pagerDensity()
            this.clipPath = PropValuePath().let {
                path.draw(it, densityValue)
                it.toPropValue()
            }
        }

        fun DeclarativeBaseView<*, *>.clip() {
            this.clip = true
        }

        fun DeclarativeBaseView<*, *>.shadow(elevation: Float, color: Color) {
            if (elevation > 0f) {
                shadowElevation = elevation
                shadowColor = color
            }
        }

        fun DeclarativeBaseView<*, *>.reset() {
            getMatrix()?.also {
                if (!it.isIdentity()) {
                    it.reset()
                    matrixChanged = true
                }
            }
            alpha = 1f
            borderRadius.fill(0f)
            clip = false
            shadowElevation = 0f
            shadowColor = Color.Transparent
            clipPath = ""
        }

        fun DeclarativeBaseView<*, *>.flush() {
            val attr = getViewAttr()
            if (alpha != 1f || attr.getProp(StyleConst.OPACITY) != null) {
                attr.opacity(alpha)
            }
            if (matrixChanged) {
                attr.applyTransform(measuredSize, getMatrix()!!)
                matrixChanged = false
            }
            val clipPath = clipPath
            if (clipPath.isNotEmpty() || attr.getProp(StyleConst.CLIP_PATH) != null) {
                attr.setProp(StyleConst.CLIP_PATH, clipPath)
            }
            val hasBorderRadius = borderRadius.any { it > 0f }
            val hasClip = clip
            if (clipPath.isEmpty() && (hasBorderRadius || hasClip || attr.getProp(StyleConst.BORDER_RADIUS) != null)) {
                if (hasBorderRadius) {
                    val array = borderRadius
                    val densityValue = getPager().pagerDensity()
                    attr.borderRadius(
                        topLeft = array[0] / densityValue,
                        topRight = array[1] / densityValue,
                        bottomRight = array[2] / densityValue,
                        bottomLeft = array[3] / densityValue
                    )
                } else {
                    // Use a very small borderRadius to implement clip effect, avoiding conflict with shadow
                    attr.borderRadius(if (hasClip) 0.001f else 0f)
                }
            }
            if (shadowElevation > 0f && shadowColor != Color.Transparent || shadowHasSet) {
                shadowHasSet = true
                val densityValue = getPager().pagerDensity()
                val elevationValue = shadowElevation / densityValue
                attr.boxShadow(BoxShadow(
                    offsetX = 0f,
                    offsetY = elevationValue * 0.5f,
                    shadowRadius = elevationValue,
                    shadowColor = shadowColor.toKuiklyColor()
                ))
            }
        }

        private fun Float.toDegrees(): Float = (this * 180_000 / PI).roundToInt() / 1000f

        private fun Attr.applyTransform(size: IntSize, matrix: Matrix) {
            val translateX = matrix[3, 0]
            val translateY = matrix[3, 1]
            var v00 = matrix[0, 0]
            var v01 = matrix[0, 1]
            var v02 = matrix[0, 2]
            var v10 = matrix[1, 0]
            var v11 = matrix[1, 1]
            var v12 = matrix[1, 2]
            var v20 = matrix[2, 0]
            var v21 = matrix[2, 1]
            var v22 = matrix[2, 2]
            // Calculate X-axis scaling
            var scaleX = sqrt(v00 * v00 + v01 * v01 + v02 * v02)
            // Normalize X-axis
            if (!scaleX.approximatelyEqual(0f)) {
                v00 /= scaleX
                v01 /= scaleX
                v02 /= scaleX
            } else {
                // When scaleX is zero, use default unit vector [1, 0, 0]
                scaleX = 0f
                v00 = 1f
                v01 = 0f
                v02 = 0f
            }
            // Calculate XY shear
            val xy = v00 * v10 + v01 * v11 + v02 * v12
            // Calculate XY orthogonal component
            v10 -= xy * v00
            v11 -= xy * v01
            v12 -= xy * v02
            // Calculate Y-axis scaling
            var scaleY = sqrt(v10 * v10 + v11 * v11 + v12 * v12)
            // Normalize Y-axis
            if (!scaleY.approximatelyEqual(0f)) {
                v10 /= scaleY
                v11 /= scaleY
                v12 /= scaleY
            } else {
                // When scaleY is zero, use default unit vector [0, 1, 0]
                scaleY = 0f
                v10 = 0f
                v11 = 1f
                v12 = 0f
            }
            // Calculate XZ shear
            val xz = v00 * v20 + v01 * v21 + v02 * v22
            // Calculate XZ orthogonal component
            v20 -= xz * v00
            v21 -= xz * v01
            v22 -= xz * v02
            // Calculate YZ shear
            val yz = v10 * v20 + v11 * v21 + v12 * v22
            // Calculate YZ orthogonal component
            v20 -= yz * v10
            v21 -= yz * v11
            v22 -= yz * v12
            // Calculate Z-axis scaling
            var scaleZ = sqrt(v20 * v20 + v21 * v21 + v22 * v22)
            // Normalize Z-axis
            if (!scaleZ.approximatelyEqual(0f)) {
                v20 /= scaleZ
                v21 /= scaleZ
                v22 /= scaleZ
            } else {
                // When scaleZ is zero, use default unit vector [0, 1, 0]
                scaleZ = 0f
                v20 = 0f
                v21 = 0f
                v22 = 1f
            }

            // Calculate rotation angles
            val rotateX = -atan2(v21, v22).toDegrees()
            val rotateY = -atan2(-v20, sqrt(v21 * v21 + v22 * v22)).toDegrees()
            val rotateZ = -atan2(v10, v00).toDegrees()
            // TODO: Skew component not considered, scale+rotate transformation may have issues
            transform(
                translate = Translate(translateX / size.width, translateY / size.height),
                scale = Scale(scaleX, scaleY),
                rotate = Rotate(rotateZ, rotateX, rotateY)
            )
        }
    }
}

private fun ViewContainer<*, *>.removeChildrenAt(index: Int, count: Int) {
    val parentView = realContainerView()
    for (i in index until index + count) {
        val childView = parentView.getChild(index)
        parentView.removeDomSubView(childView)
        parentView.removeChild(childView)
    }
}