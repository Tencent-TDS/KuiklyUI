/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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
import com.tencent.kuikly.compose.foundation.gestures.Orientation
import com.tencent.kuikly.compose.ui.Modifier
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
import com.tencent.kuikly.compose.extension.intEqual
import com.tencent.kuikly.compose.gestures.KuiklyScrollInfo
import com.tencent.kuikly.compose.layout.resetViewVisible
import com.tencent.kuikly.compose.node.extPropsVar
import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.Attr.StyleConst
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.DeclarativeBaseView
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

    // 构造下kuikly的节点树
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
        view.flush(density)
    }

    override fun invalidateDraw() {
        if (!drawInvalidated) {
            drawInvalidated = true
            parent?.invalidateDraw()
        }
    }

    private fun shouldUpdateStickyNode(newSize: IntSize): Boolean {
        if (view !is HoverView) return true
        val curSize = IntSize(
            width = ((view.renderView?.currentFrame?.width ?: 0f) * density.density).toInt(),
            height = ((view.renderView?.currentFrame?.height ?: 0f) * density.density).toInt(),
        )
        if (curSize != newSize) return true

        // Use foldedParent to get the real parent including virtual nodes
        val itemKey = (foldedParent as? KNode<*>)?.lazyItemKey
        val scrollNode = parent as? KNode<*>
        val kuiklyInfo = scrollNode?.view?.extProps?.get(KuiklyInfoKey) as? KuiklyScrollInfo
        val currentStickyKey = kuiklyInfo?.stickyItemKey

        if (itemKey != null && itemKey == currentStickyKey) {
            return false
        }
        return true
    }

    override fun onWillStartMeasure() {
        super.onWillStartMeasure()
        kuiklyCoordinates = null
    }

    override fun updateKuiklyViewFrame(coordinator: LayoutCoordinates) {

        val curCoordinator = kuiklyCoordinates ?: innerCoordinator
        val shouldUpdateSticky = shouldUpdateStickyNode(curCoordinator.size)
        if (!shouldUpdateSticky) {
            return
        }

        resetViewVisible()

        val ksScrollSubView = view.parent is VirtualNodeView
        val parentNode = parent as? KNode<*>
        val parentCoordinator = parentNode?.kuiklyCoordinates ?: parentNode?.innerCoordinator
        var pos = parentCoordinator?.localPositionOf(curCoordinator, Offset.Zero) ?: Offset.Zero

        // scrollview上的子节点，父亲是virtual节点
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

        val densityValue = density.density

        var newFrame = Frame(
            x = pos.x / densityValue,
            y = pos.y / densityValue,
            width = curCoordinator.size.width.toFloat() / densityValue,
            height = curCoordinator.size.height.toFloat() / densityValue,
        )

        // modalView特殊处理
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

    private fun DeclarativeBaseView<*, *>.updateFrame(newFrame: Frame) {
        val curFrame = renderView?.currentFrame ?: Frame.zero
        if (!curFrame.intEqual(newFrame)) {
            val densityFrame = Frame(
                x = newFrame.x,
                y = newFrame.y,
                width =  newFrame.width,
                height = newFrame.height
            )

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
                        // 禁用DivView优化 保证布局正常
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
                    // 禁用DivView优化 保证布局正常
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
        private var DeclarativeBaseView<*, *>.borderRadius by extPropsVar("borderRadius") { FloatArray(4) }
        private var DeclarativeBaseView<*, *>.clip by extPropsVar("clip") { false }
        private var DeclarativeBaseView<*, *>.shadowElevation by extPropsVar("shadowElevation") { 0f }
        private var DeclarativeBaseView<*, *>.shadowColor by extPropsVar("shadowColor") { Color.Transparent }

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
        }

        fun DeclarativeBaseView<*, *>.flush(density: Density) {
            val attr = getViewAttr()
            if (alpha != 1f || attr.getProp(StyleConst.OPACITY) != null) {
                attr.opacity(alpha)
            }
            if (matrixChanged) {
                attr.applyTransform(measuredSize, getMatrix()!!)
                matrixChanged = false
            }
            val hasBorderRadius = borderRadius.any { it > 0f }
            val hasClip = clip
            if (hasBorderRadius || hasClip || attr.getProp(StyleConst.BORDER_RADIUS) != null) {
                if (hasBorderRadius) {
                    val array = borderRadius
                    with(density) {
                        attr.borderRadius(
                            topLeft = array[0].toDp().value,
                            topRight = array[1].toDp().value,
                            bottomRight = array[2].toDp().value,
                            bottomLeft = array[3].toDp().value
                        )
                    }
                } else {
                    // 利用一个很小的borderRadius来实现clip效果，避免和shadow冲突
                    attr.borderRadius(if (hasClip) 0.001f else 0f)
                }
            }
            if (shadowElevation > 0f && shadowColor != Color.Transparent || attr.getProp(StyleConst.BOX_SHADOW) != null) {
                val elevationValue = with(density) { shadowElevation.toDp().value }
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
            // 计算X轴缩放
            var scaleX = sqrt(v00 * v00 + v01 * v01 + v02 * v02)
            // 归一化X轴
            v00 /= scaleX
            v01 /= scaleX
            v02 /= scaleX
            // 计算XY错切
            val xy = v00 * v10 + v01 * v11 + v02 * v12
            // 计算XY正交分量
            v10 -= xy * v00
            v11 -= xy * v01
            v12 -= xy * v02
            // 计算Y轴缩放
            var scaleY = sqrt(v10 * v10 + v11 * v11 + v12 * v12)
            // 归一化Y轴
            v10 /= scaleY
            v11 /= scaleY
            v12 /= scaleY
            // 计算XZ错切
            val xz = v00 * v20 + v01 * v21 + v02 * v22
            // 计算XZ正交分量
            v20 -= xz * v00
            v21 -= xz * v01
            v22 -= xz * v02
            // 计算YZ错切
            val yz = v10 * v20 + v11 * v21 + v12 * v22
            // 计算YZ正交分量
            v20 -= yz * v10
            v21 -= yz * v11
            v22 -= yz * v12
            // 计算Z轴缩放
            var scaleZ = sqrt(v20 * v20 + v21 * v21 + v22 * v22)
            // 归一化Z轴
            v20 /= scaleZ
            v21 /= scaleZ
            v22 /= scaleZ

            // 计算旋转角度
            val rotateX = -atan2(v21, v22).toDegrees()
            val rotateY = -atan2(-v20, sqrt(v21 * v21 + v22 * v22)).toDegrees()
            val rotateZ = -atan2(v10, v00).toDegrees()
            // todo 未考虑skew分量，scale+rotate变换会有问题
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