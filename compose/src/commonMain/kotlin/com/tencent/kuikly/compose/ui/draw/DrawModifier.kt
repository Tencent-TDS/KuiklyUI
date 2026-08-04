/*
 * Copyright 2019 The Android Open Source Project
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

package com.tencent.kuikly.compose.ui.draw

import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Size
import com.tencent.kuikly.compose.ui.graphics.Canvas
import com.tencent.kuikly.compose.ui.graphics.drawscope.ContentDrawScope
import com.tencent.kuikly.compose.ui.graphics.drawscope.DrawScope
import com.tencent.kuikly.compose.ui.internal.JvmDefaultWithCompatibility
import com.tencent.kuikly.compose.ui.node.ModifierNodeElement
import com.tencent.kuikly.compose.ui.node.Nodes
import com.tencent.kuikly.compose.ui.node.ObserverModifierNode
import com.tencent.kuikly.compose.ui.node.invalidateDraw
import com.tencent.kuikly.compose.ui.node.observeReads
import com.tencent.kuikly.compose.ui.node.requireCoordinator
import com.tencent.kuikly.compose.ui.node.requireDensity
import com.tencent.kuikly.compose.ui.node.requireLayoutDirection
import com.tencent.kuikly.compose.ui.platform.InspectorInfo
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.LayoutDirection
import com.tencent.kuikly.compose.ui.unit.toSize
import com.tencent.kuikly.compose.ui.node.DrawModifierNode
import com.tencent.kuikly.compose.ui.node.OwnerScope
import com.tencent.kuikly.compose.ui.node.invalidateDraw
import com.tencent.kuikly.compose.ui.node.requireLayoutNode
import com.tencent.kuikly.compose.ui.node.requireOwner
import com.tencent.kuikly.compose.ui.KuiklyCanvas
import com.tencent.kuikly.compose.ui.graphics.drawscope.CanvasDrawScope
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.views.CanvasView

/**
 * A [Modifier.Element] that draws into the space of the layout.
 */
@JvmDefaultWithCompatibility
interface DrawModifier : Modifier.Element {

    fun ContentDrawScope.draw()
}

/**
 * [DrawModifier] implementation that supports building a cache of objects
 * to be referenced across draw calls
 */
@JvmDefaultWithCompatibility
interface DrawCacheModifier : DrawModifier {

    /**
     * Callback invoked to re-build objects to be re-used across draw calls.
     * This is useful to conditionally recreate objects only if the size of the
     * drawing environment changes, or if state parameters that are inputs
     * to objects change. This method is guaranteed to be called before
     * [DrawModifier.draw].
     *
     * @param params The params to be used to build the cache.
     */
    fun onBuildCache(params: BuildDrawCacheParams)
}

/**
 * The set of parameters which could be used to build the drawing cache.
 *
 * @see DrawCacheModifier.onBuildCache
 */
interface BuildDrawCacheParams {
    /**
     * The current size of the drawing environment
     */
    val size: Size

    /**
     * The current layout direction.
     */
    val layoutDirection: LayoutDirection

    /**
     * The current screen density to provide the ability to convert between
     */
    val density: Density
}

/**
 * Draw into a [Canvas] behind the modified content.
 */
fun Modifier.drawBehind(
    onDraw: DrawScope.() -> Unit
) = this then DrawBehindElement(onDraw)

private data class DrawBehindElement(
    val onDraw: DrawScope.() -> Unit
) : ModifierNodeElement<DrawBackgroundModifier>() {
    override fun create() = DrawBackgroundModifier(onDraw)

    override fun update(node: DrawBackgroundModifier) {
        node.onDraw = onDraw
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawBehind"
        properties["onDraw"] = onDraw
    }
}

internal class DrawBackgroundModifier(
    var onDraw: DrawScope.() -> Unit
) : Modifier.Node(), DrawModifierNode, OwnerScope {

    /**
     * drawBehind 用的背景绘制层。
     * 文字等组件要画背景时，挂一个独立的背景视图到父容器最底层来实现。
     *
     * 注意：这条背景是挂在父容器里的（不是画在文字内部）。
     * 正常从上往下、从左往右排布不受影响；
     * 以后想完全对齐官方做法，需要把背景画进宿主自己内部。
     */
    private var bgCanvasView: CanvasView? = null

    /**
     * 背景 CanvasView 专用的 DrawScope（canvas 绑到 bgCanvasView）。
     * 复用同一个 CanvasDrawScope 以复用 Paint 对象，与 LayoutNodeDrawScope 同思路。
     */
    private val bgDrawScope = CanvasDrawScope()

    /**
     * 记录上一次真正下发给背景 CanvasView 的 frame（dp）。
     * 当本次 frame 与上次完全一致时跳过重复的跨端 setFrame 调用。
     * NaN 表示尚未下发过。
     */
    private var lastBgX: Float = Float.NaN
    private var lastBgY: Float = Float.NaN
    private var lastBgW: Float = Float.NaN
    private var lastBgH: Float = Float.NaN

    override fun ContentDrawScope.draw(view: DeclarativeBaseView<*, *>?) {
        if (view is CanvasView) {
            requireOwner().snapshotObserver.observeReads(
                this@DrawBackgroundModifier,
                DrawModifierNode::invalidateDraw
            ) {
                onDraw()
            }
        } else if (view != null) {
            // 非 CanvasView 宿主：走背景 CanvasView 通道
            ensureBackgroundCanvasView(view)
            // 用 DrawScope.size（= 宿主完整布局尺寸，含多行）而非 renderView.currentFrame
            //（后者对 RichTextView 只返一行高）
            // 与 CanvasView 分支一致，用 observeReads 包裹，使 draw 闭包内读取的
            // snapshot state 变化时能触发重绘（否则仅依赖重组会漏掉部分场景）
            requireOwner().snapshotObserver.observeReads(
                this@DrawBackgroundModifier,
                DrawModifierNode::invalidateDraw
            ) {
                drawIntoBackgroundCanvasView(view, size)
            }
        } else {
            KLog.e("Kuikly.Compose", "drawBehind expect CanvasView, but got $view")
        }
        drawContent()
    }

    /**
     * 惰性创建背景 CanvasView 并加到宿主的父容器（绝对定位，初始位置=宿主位置）。
     * 实际尺寸/位置由 [drawIntoBackgroundCanvasView] 每次 draw 手动 setFrame 同步
     *（flex 不会给 draw 期间注入的 absolute 子 view 分 frame）。
     */
    private fun ensureBackgroundCanvasView(hostView: DeclarativeBaseView<*, *>) {
        if (bgCanvasView != null) return
        val parent = hostView.parent as? ViewContainer<*, *> ?: run {
            KLog.e("Kuikly.Compose", "drawBehind bgCanvas: host has no ViewContainer parent")
            return
        }
        // RichTextView 等自测量组件的 flexNode.layoutFrame 为 0，真位置在 renderView.currentFrame
        val frame = hostView.renderView?.currentFrame ?: return

        val bg = CanvasView()
        var addedToParent = false
        try {
            parent.addChild(bg, {
                // absolutePosition 设 positionType=ABSOLUTE + 初始 top/left；
                // 尺寸不在此设（flex 不分 frame）
                getViewAttr().absolutePosition(top = frame.y, left = frame.x)
            }, 0)
            addedToParent = true
            parent.insertDomSubView(bg, 0)
            bgCanvasView = bg
            resetDebounceState()
        } catch (e: Throwable) {
            KLog.e("Kuikly.Compose", "drawBehind bgCanvas: ensure failed: ${e.message}")
            // 半挂状态回滚：addChild 成功但后续步骤失败时，onDetach 无法通过 bgCanvasView
            // 触达 bg，会造成 view 泄漏；这里显式清掉已挂的 bg。
            if (addedToParent) {
                runCatching { parent.removeDomSubView(bg) }
                runCatching { parent.removeChild(bg) }
            }
        }
    }

    private fun resetDebounceState() {
        lastBgX = Float.NaN
        lastBgY = Float.NaN
        lastBgW = Float.NaN
        lastBgH = Float.NaN
    }

    /**
     * 把背景 CanvasView 定位到宿主同帧（无额外 padding，与官方语义对齐），并用 KuiklyCanvas +
     * CanvasDrawScope 把 onDraw 跑进 bg。绕过 CanvasView.draw() 的
     * flexNode.layoutFrame.isDefaultValue() 检查（flex 不给注入子分 frame，该检查恒 true）。
     */
    private fun drawIntoBackgroundCanvasView(
        hostView: DeclarativeBaseView<*, *>,
        scopeSize: Size
    ) {
        val bg = bgCanvasView ?: return
        val bgRender = bg.renderView ?: return
        // 位置用 renderView.currentFrame.x/y（宿主在父容器中的坐标，dp）；
        // 尺寸用 scopeSize（= 宿主完整布局尺寸含多行，px），不再用 currentFrame 的 height
        //（后者对 RichTextView 只返一行高）。
        val posFrame = hostView.renderView?.currentFrame ?: return
        if (scopeSize.width <= 0f || scopeSize.height <= 0f) return
        try {
            // 对齐官方语义：drawBehind 的 DrawScope.size 必须严格等于组件自身布局
            // 尺寸，无任何偏移补丁。下划线"不穿字"由调用方在 lambda 内自行决定
            // 绘制 y 坐标（如画在 size.height），框架不替宿主加底部 padding。
            val density = requireDensity().density
            val bgWidthDp = scopeSize.width / density
            val bgHeightDp = scopeSize.height / density
            // frame 与上次完全一致时跳过 setFrame；重复下发不会改变视觉结果。
            val frameUnchanged = bgWidthDp == lastBgW && bgHeightDp == lastBgH &&
                posFrame.x == lastBgX && posFrame.y == lastBgY
            if (!frameUnchanged) {
                bgRender.setFrame(posFrame.x, posFrame.y, bgWidthDp, bgHeightDp)
                lastBgX = posFrame.x
                lastBgY = posFrame.y
                lastBgW = bgWidthDp
                lastBgH = bgHeightDp
            }
            val bgCanvas = KuiklyCanvas()
            bgCanvas.view = bg
            val drawBlock = onDraw
            bgDrawScope.draw(
                requireDensity(),
                requireLayoutDirection(),
                bgCanvas,
                Size(scopeSize.width, scopeSize.height)
            ) {
                drawBlock()
            }
        } catch (e: Throwable) {
            KLog.e("Kuikly.Compose", "drawBehind bgCanvas draw failed: ${e.message}")
        }
    }

    override fun onDetach() {
        bgCanvasView?.let { bg ->
            (bg.parent as? ViewContainer<*, *>)?.let { parent ->
                runCatching { parent.removeDomSubView(bg) }
                runCatching { parent.removeChild(bg) }
            }
        }
        bgCanvasView = null
        super.onDetach()
    }

    override val isValidOwnerScope: Boolean get() = isAttached
}

/**
 * Draw into a [DrawScope] with content that is persisted across
 * draw calls as long as the size of the drawing area is the same or
 * any state objects that are read have not changed. In the event that
 * the drawing area changes, or the underlying state values that are being read
 * change, this method is invoked again to recreate objects to be used during drawing
 *
 * For example, a [com.tencent.kuikly.compose.ui.graphics.LinearGradient] that is to occupy the full
 * bounds of the drawing area can be created once the size has been defined and referenced
 * for subsequent draw calls without having to re-allocate.
 *
 * @sample com.tencent.kuikly.compose.ui.samples.DrawWithCacheModifierSample
 * @sample com.tencent.kuikly.compose.ui.samples.DrawWithCacheModifierStateParameterSample
 * @sample com.tencent.kuikly.compose.ui.samples.DrawWithCacheContentSample
 */
fun Modifier.drawWithCache(
    onBuildDrawCache: CacheDrawScope.() -> DrawResult
) = this then DrawWithCacheElement(onBuildDrawCache)

private data class DrawWithCacheElement(
    val onBuildDrawCache: CacheDrawScope.() -> DrawResult
) : ModifierNodeElement<CacheDrawModifierNodeImpl>() {
    override fun create(): CacheDrawModifierNodeImpl {
        return CacheDrawModifierNodeImpl(CacheDrawScope(), onBuildDrawCache)
    }

    override fun update(node: CacheDrawModifierNodeImpl) {
        node.block = onBuildDrawCache
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawWithCache"
        properties["onBuildDrawCache"] = onBuildDrawCache
    }
}

fun CacheDrawModifierNode(
    onBuildDrawCache: CacheDrawScope.() -> DrawResult
): CacheDrawModifierNode {
    return CacheDrawModifierNodeImpl(CacheDrawScope(), onBuildDrawCache)
}

/**
 * Expands on the [com.tencent.kuikly.compose.ui.node.DrawModifierNode] by adding the ability to invalidate
 * the draw cache for changes in things like shapes and bitmaps (see Modifier.border for a usage
 * examples).
 */
sealed interface CacheDrawModifierNode : DrawModifierNode {
    fun invalidateDrawCache()
}

private class CacheDrawModifierNodeImpl(
    private val cacheDrawScope: CacheDrawScope,
    block: CacheDrawScope.() -> DrawResult
) : Modifier.Node(), CacheDrawModifierNode, ObserverModifierNode, BuildDrawCacheParams {

    private var isCacheValid = false
    var block: CacheDrawScope.() -> DrawResult = block
        set(value) {
            field = value
            invalidateDrawCache()
        }

    init {
        cacheDrawScope.cacheParams = this
    }

    override val density: Density get() = requireDensity()
    override val layoutDirection: LayoutDirection get() = requireLayoutDirection()
    override val size: Size get() = requireCoordinator(Nodes.LayoutAware).size.toSize()

    override fun onMeasureResultChanged() {
        invalidateDrawCache()
    }

    override fun onObservedReadsChanged() {
        invalidateDrawCache()
    }

    override fun invalidateDrawCache() {
        isCacheValid = false
        cacheDrawScope.drawResult = null
        invalidateDraw()
    }

    private fun getOrBuildCachedDrawBlock(): DrawResult {
        if (!isCacheValid) {
            cacheDrawScope.apply {
                drawResult = null
                observeReads { block() }
                checkNotNull(drawResult) {
                    "DrawResult not defined, did you forget to call onDraw?"
                }
            }
            isCacheValid = true
        }
        return cacheDrawScope.drawResult!!
    }

    override fun ContentDrawScope.draw() {
        getOrBuildCachedDrawBlock().block(this)
    }
}

/**
 * Handle to a drawing environment that enables caching of content based on the resolved size.
 * Consumers define parameters and refer to them in the captured draw callback provided in
 * [onDrawBehind] or [onDrawWithContent].
 *
 * [onDrawBehind] will draw behind the layout's drawing contents however, [onDrawWithContent] will
 * provide the ability to draw before or after the layout's contents
 */
class CacheDrawScope internal constructor() : Density {
    internal var cacheParams: BuildDrawCacheParams = EmptyBuildDrawCacheParams
    internal var drawResult: DrawResult? = null

    /**
     * Provides the dimensions of the current drawing environment
     */
    val size: Size get() = cacheParams.size

    /**
     * Provides the [LayoutDirection].
     */
    val layoutDirection: LayoutDirection get() = cacheParams.layoutDirection

    /**
     * Issue drawing commands to be executed before the layout content is drawn
     */
    fun onDrawBehind(block: DrawScope.() -> Unit): DrawResult = onDrawWithContent {
        block()
        drawContent()
    }

    /**
     * Issue drawing commands before or after the layout's drawing contents
     */
    fun onDrawWithContent(block: ContentDrawScope.() -> Unit): DrawResult {
        return DrawResult(block).also { drawResult = it }
    }

    override val density: Float
        get() = cacheParams.density.density

    override val fontScale: Float
        get() = cacheParams.density.fontScale
}

private object EmptyBuildDrawCacheParams : BuildDrawCacheParams {
    override val size: Size = Size.Unspecified
    override val layoutDirection: LayoutDirection = LayoutDirection.Ltr
    override val density: Density = Density(1f, 1f)
}

/**
 * Holder to a callback to be invoked during draw operations. This lambda
 * captures and reuses parameters defined within the CacheDrawScope receiver scope lambda.
 */
class DrawResult internal constructor(internal var block: ContentDrawScope.() -> Unit)

/**
 * Creates a [DrawModifier] that allows the developer to draw before or after the layout's
 * contents. It also allows the modifier to adjust the layout's canvas.
 */
fun Modifier.drawWithContent(
    onDraw: ContentDrawScope.() -> Unit
): Modifier = this then DrawWithContentElement(onDraw)

private data class DrawWithContentElement(
    val onDraw: ContentDrawScope.() -> Unit
) : ModifierNodeElement<DrawWithContentModifier>() {
    override fun create() = DrawWithContentModifier(onDraw)

    override fun update(node: DrawWithContentModifier) {
        node.onDraw = onDraw
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "drawWithContent"
        properties["onDraw"] = onDraw
    }
}

private class DrawWithContentModifier(
    var onDraw: ContentDrawScope.() -> Unit
) : Modifier.Node(), DrawModifierNode {

    override fun ContentDrawScope.draw() {
        onDraw()
    }
}
