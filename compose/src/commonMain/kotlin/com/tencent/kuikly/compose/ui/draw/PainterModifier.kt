/*
 * Copyright 2020 The Android Open Source Project
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

import com.tencent.kuikly.compose.coil3.AsyncImagePainter
import com.tencent.kuikly.compose.foundation.ImageViewWrap
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.KuiklyPainter
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Size
import com.tencent.kuikly.compose.ui.geometry.isSpecified
import com.tencent.kuikly.compose.ui.graphics.BlendModeColorFilter
import com.tencent.kuikly.compose.ui.graphics.ColorFilter
import com.tencent.kuikly.compose.ui.graphics.DefaultAlpha
import com.tencent.kuikly.compose.ui.graphics.drawscope.ContentDrawScope
import com.tencent.kuikly.compose.ui.graphics.isSupported
import com.tencent.kuikly.compose.ui.graphics.painter.Painter
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.layout.IntrinsicMeasurable
import com.tencent.kuikly.compose.ui.layout.IntrinsicMeasureScope
import com.tencent.kuikly.compose.ui.layout.Measurable
import com.tencent.kuikly.compose.ui.layout.MeasureResult
import com.tencent.kuikly.compose.ui.layout.MeasureScope
import com.tencent.kuikly.compose.ui.layout.times
import com.tencent.kuikly.compose.ui.node.DrawModifierNode
import com.tencent.kuikly.compose.ui.node.KNode.Companion.alpha
import com.tencent.kuikly.compose.ui.node.KNode.Companion.clip
import com.tencent.kuikly.compose.ui.node.LayoutModifierNode
import com.tencent.kuikly.compose.ui.node.ModifierNodeElement
import com.tencent.kuikly.compose.ui.node.invalidateDraw
import com.tencent.kuikly.compose.ui.node.invalidateLayer
import com.tencent.kuikly.compose.ui.node.invalidateMeasurement
import com.tencent.kuikly.compose.ui.node.requireDensity
import com.tencent.kuikly.compose.ui.platform.InspectorInfo
import com.tencent.kuikly.compose.ui.unit.Constraints
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.unit.constrainHeight
import com.tencent.kuikly.compose.ui.unit.constrainWidth
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.views.ImageConst
import com.tencent.kuikly.core.views.ImageView
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Paint the content using [painter].
 *
 * @param sizeToIntrinsics `true` to size the element relative to [Painter.intrinsicSize]
 * @param alignment specifies alignment of the [painter] relative to content
 * @param contentScale strategy for scaling [painter] if its size does not match the content size
 * @param alpha opacity of [painter]
 * @param colorFilter optional [ColorFilter] to apply to [painter]
 *
 * @sample com.tencent.kuikly.compose.ui.samples.PainterModifierSample
 */
fun Modifier.paint(
    painter: Painter,
    sizeToIntrinsics: Boolean = true,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Inside,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
): Modifier {
    // Kuikly modified: implementation moved to `paintInternal` due to it can only be used with Image
    KLog.e("Kuikly.Compose", "paint can only be used with Image")
    return this
}

internal fun Modifier.paintInternal(
    painter: Painter,
    sizeToIntrinsics: Boolean = true,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Inside,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
) = this then PainterElement(
    painter = painter,
    sizeToIntrinsics = sizeToIntrinsics,
    alignment = alignment,
    contentScale = contentScale,
    alpha = alpha,
    colorFilter = colorFilter
)

/**
 * Customized [ModifierNodeElement] for painting content using [painter].
 *
 * @param painter used to paint content
 * @param sizeToIntrinsics `true` to size the element relative to [Painter.intrinsicSize]
 * @param alignment specifies alignment of the [painter] relative to content
 * @param contentScale strategy for scaling [painter] if its size does not match the content size
 * @param alpha opacity of [painter]
 * @param colorFilter optional [ColorFilter] to apply to [painter]
 *
 * @sample com.tencent.kuikly.compose.ui.samples.PainterModifierSample
 */
private data class PainterElement(
    val painter: Painter,
    val sizeToIntrinsics: Boolean,
    val alignment: Alignment,
    val contentScale: ContentScale,
    val alpha: Float,
    val colorFilter: ColorFilter?
) : ModifierNodeElement<PainterNode>() {
    override fun create(): PainterNode {
        return PainterNode(
            painter = painter,
            sizeToIntrinsics = sizeToIntrinsics,
            alignment = alignment,
            contentScale = contentScale,
            alpha = alpha,
            colorFilter = colorFilter,
        )
    }

    override fun update(node: PainterNode) {
        if (painter is KuiklyPainter) {
            painter.updateFromReuse(node.painter)
        }

        val intrinsicsChanged = node.sizeToIntrinsics != sizeToIntrinsics ||
            (sizeToIntrinsics && node.painter.intrinsicSize != painter.intrinsicSize)

        node.painter = painter
        node.sizeToIntrinsics = sizeToIntrinsics
        node.alignment = alignment
        node.contentScale = contentScale
        node.alpha = alpha
        node.colorFilter = colorFilter

        // Only remeasure if intrinsics have changed.
        if (intrinsicsChanged) {
            node.invalidateMeasurement()
        }
        // redraw because one of the node properties has changed.
        node.invalidateDraw()
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "paint"
        properties["painter"] = painter
        properties["sizeToIntrinsics"] = sizeToIntrinsics
        properties["alignment"] = alignment
        properties["contentScale"] = contentScale
        properties["alpha"] = alpha
        // properties["colorFilter"] = colorFilter
    }
}

/**
 * [DrawModifier] used to draw the provided [Painter] followed by the contents
 * of the component itself
 *
 *
 * IMPORTANT NOTE: This class sets [com.tencent.kuikly.compose.ui.Modifier.Node.shouldAutoInvalidate]
 * to false which means it MUST invalidate both draw and the layout. It invalidates both in the
 * [PainterElement.update] method through [LayoutModifierNode.invalidateLayer]
 * (invalidates draw) and [LayoutModifierNode.invalidateLayout] (invalidates layout).
 */
private class PainterNode(
    var painter: Painter,
    var sizeToIntrinsics: Boolean,
    var alignment: Alignment = Alignment.Center,
    var contentScale: ContentScale = ContentScale.Inside,
    var alpha: Float = DefaultAlpha,
    var colorFilter: ColorFilter? = null
) : LayoutModifierNode, Modifier.Node(), DrawModifierNode {

    /**
     * Helper property to determine if we should size content to the intrinsic
     * size of the Painter or not. This is only done if [sizeToIntrinsics] is true
     * and the Painter has an intrinsic size
     */
    private val useIntrinsicSize: Boolean
        get() = sizeToIntrinsics && painter.intrinsicSize.isSpecified

    override val shouldAutoInvalidate: Boolean
        get() = false

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        val placeable = measurable.measure(modifyConstraints(constraints))
        return layout(placeable.width, placeable.height) {
            placeable.placeRelative(0, 0)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        return if (useIntrinsicSize) {
            val constraints = modifyConstraints(Constraints(maxHeight = height))
            val layoutWidth = measurable.minIntrinsicWidth(height)
            max(constraints.minWidth, layoutWidth)
        } else {
            measurable.minIntrinsicWidth(height)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        return if (useIntrinsicSize) {
            val constraints = modifyConstraints(Constraints(maxHeight = height))
            val layoutWidth = measurable.maxIntrinsicWidth(height)
            max(constraints.minWidth, layoutWidth)
        } else {
            measurable.maxIntrinsicWidth(height)
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        return if (useIntrinsicSize) {
            val constraints = modifyConstraints(Constraints(maxWidth = width))
            val layoutHeight = measurable.minIntrinsicHeight(width)
            max(constraints.minHeight, layoutHeight)
        } else {
            measurable.minIntrinsicHeight(width)
        }
    }

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurable: IntrinsicMeasurable,
        width: Int
    ): Int {
        return if (useIntrinsicSize) {
            val constraints = modifyConstraints(Constraints(maxWidth = width))
            val layoutHeight = measurable.maxIntrinsicHeight(width)
            max(constraints.minHeight, layoutHeight)
        } else {
            measurable.maxIntrinsicHeight(width)
        }
    }

    private fun calculateScaledSize(dstSize: Size): Size {
        return if (!useIntrinsicSize) {
            dstSize
        } else {
            val srcWidth = if (!painter.intrinsicSize.hasSpecifiedAndFiniteWidth()) {
                dstSize.width
            } else {
                painter.intrinsicSize.width
            }

            val srcHeight = if (!painter.intrinsicSize.hasSpecifiedAndFiniteHeight()) {
                dstSize.height
            } else {
                painter.intrinsicSize.height
            }

            val srcSize = Size(srcWidth, srcHeight)
            if (dstSize.width != 0f && dstSize.height != 0f) {
                srcSize * contentScale.computeScaleFactor(srcSize, dstSize)
            } else {
                Size.Zero
            }
        }
    }

    private fun modifyConstraints(constraints: Constraints): Constraints {
        val hasBoundedDimens = constraints.hasBoundedWidth && constraints.hasBoundedHeight
        val hasFixedDimens = constraints.hasFixedWidth && constraints.hasFixedHeight
        if ((!useIntrinsicSize && hasBoundedDimens) || hasFixedDimens) {
            // If we have fixed constraints or we are not attempting to size the
            // composable based on the size of the Painter, do not attempt to
            // modify them. Otherwise rely on Alignment and ContentScale
            // to determine how to position the drawing contents of the Painter within
            // the provided bounds
            return constraints.copy(
                minWidth = constraints.maxWidth,
                minHeight = constraints.maxHeight
            )
        }

        val intrinsicSize = painter.intrinsicSize
        val intrinsicWidth =
            if (intrinsicSize.hasSpecifiedAndFiniteWidth()) {
                intrinsicSize.width.roundToInt()
            } else {
                constraints.minWidth
            }

        val intrinsicHeight =
            if (intrinsicSize.hasSpecifiedAndFiniteHeight()) {
                intrinsicSize.height.roundToInt()
            } else {
                constraints.minHeight
            }

        // Scale the width and height appropriately based on the given constraints
        // and ContentScale
        val constrainedWidth = constraints.constrainWidth(intrinsicWidth)
        val constrainedHeight = constraints.constrainHeight(intrinsicHeight)
        val scaledSize = calculateScaledSize(
            Size(constrainedWidth.toFloat(), constrainedHeight.toFloat())
        )

        // For both width and height constraints, consume the minimum of the scaled width
        // and the maximum constraint as some scale types can scale larger than the maximum
        // available size (ex ContentScale.Crop)
        // In this case the larger of the 2 dimensions is used and the aspect ratio is
        // maintained. Even if the size of the composable is smaller, the painter will
        // draw its content clipped
        val minWidth = constraints.constrainWidth(scaledSize.width.roundToInt())
        val minHeight = constraints.constrainHeight(scaledSize.height.roundToInt())
        return constraints.copy(minWidth = minWidth, minHeight = minHeight)
    }

    override fun ContentDrawScope.draw(view: DeclarativeBaseView<*, *>?) {
        val (isWrap, imageView, placeholder) = view?.asImageView() ?: let {
            drawContent()
            return
        }
        if (!isWrap && drawSimple(imageView)) {
            drawContent()
            return
        }

        if (painter.showPlaceholder()) {
            drawPainter(painter.requirePlaceholder(), view, placeholder)
        } else {
            placeholder?.getViewAttr()?.visibility(false)
        }
        drawPainter(painter, view, imageView)
        // Maintain the same pattern as Modifier.drawBehind to allow chaining of DrawModifiers
        drawContent()
    }

    private fun drawSimple(imageView: ImageView): Boolean {
        when (contentScale) {
            ContentScale.FillBounds -> imageView.getViewAttr().resizeStretch()
            ContentScale.Crop -> imageView.getViewAttr().resizeCover()
            ContentScale.Fit -> imageView.getViewAttr().resizeContain()
            else -> return false
        }
        applyCommon(imageView)
        if (painter.showPlaceholder()) {
            applyPainter(painter.requirePlaceholder(), imageView, imageView)
        }
        applyPainter(painter, imageView, imageView)
        return true
    }

    private fun Size.hasSpecifiedAndFiniteWidth() = this != Size.Unspecified && width.isFinite()
    private fun Size.hasSpecifiedAndFiniteHeight() = this != Size.Unspecified && height.isFinite()

    private inline fun DeclarativeBaseView<*, *>.asImageView(): Triple<Boolean, ImageView, ImageView?>? {
        if (this is ImageView) {
            return Triple(false, this, null)
        }
        if (this is ImageViewWrap) {
            val placeholder = if (this.placeholder) this.placeholderView else null
            return Triple(true, this.imageView, placeholder)
        }
        return null
    }

    private fun ContentDrawScope.drawPainter(
        painter: Painter,
        view: DeclarativeBaseView<*, *>,
        imageView: ImageView?
    ) {
        val intrinsicSize = painter.intrinsicSize
        val srcWidth = if (intrinsicSize.hasSpecifiedAndFiniteWidth()) {
            intrinsicSize.width
        } else {
            size.width
        }

        val srcHeight = if (intrinsicSize.hasSpecifiedAndFiniteHeight()) {
            intrinsicSize.height
        } else {
            size.height
        }

        val srcSize = Size(srcWidth, srcHeight)

        // Compute the offset to translate the content based on the given alignment
        // and size to draw based on the ContentScale parameter
        val scaledSize = if (size.width != 0f && size.height != 0f) {
            srcSize * contentScale.computeScaleFactor(srcSize, size)
        } else {
            Size.Zero
        }

        val alignedPosition = alignment.align(
            IntSize(scaledSize.width.roundToInt(), scaledSize.height.roundToInt()),
            IntSize(size.width.roundToInt(), size.height.roundToInt()),
            layoutDirection
        )

        val dx = alignedPosition.x.toFloat()
        val dy = alignedPosition.y.toFloat()

        // Only translate the current drawing position while delegating the Painter to draw
        // with scaled size.
        // Individual Painter implementations should be responsible for scaling their drawing
        // content accordingly to fit within the drawing area.
//        translate(dx, dy) {
//            with(painter) {
//                draw(size = scaledSize, alpha = alpha, colorFilter = colorFilter)
//            }
//        }
        imageView?.getViewAttr()?.apply {
            visibility(painter.visible())
            if (scaledSize.width <= 0 || scaledSize.height <= 0) {
                resizeStretch()
                // 因为0宽高的图片不会加载，所以指定一个1x1的大小
                imageView.setFrameToRenderView(Frame(-1f, -1f, 1f, 1f))
            } else if (/*isWrap*/view != imageView) {
                // 使用wrap时，裁切到view的宽高
                view.clip()
                // 使用wrap时，ImageView设置为scaledSize宽高
                resizeStretch()
                with(requireDensity()) {
                    imageView.setFrameToRenderView(
                        Frame(
                            dx.toDp().value,
                            dy.toDp().value,
                            scaledSize.width.toDp().value,
                            scaledSize.height.toDp().value
                        )
                    )
                }
            } else if (scaledSize.width - size.width < 1e-6 && scaledSize.height - size.height < 1e-6) {
                // 非wrap时，如果图片宽高小于等于显示区域，使用contain模式
                resizeContain()
            } else {
                // 非wrap时，如果图片宽高大于显示区域，使用cover模式
                resizeCover()
            }
        }
        imageView?.also(this@PainterNode::applyCommon)
        applyPainter(painter, view, imageView)
    }

    private fun applyPainter(
        painter: Painter,
        view: DeclarativeBaseView<*, *>,
        imageView: ImageView?
    ) {
        if (!painter.applyAlpha(alpha)) {
            view.alpha(alpha)
        }
        painter.applyTo(imageView ?: view)
    }

    private fun Painter.showPlaceholder(): Boolean {
        return (this is KuiklyPainter) && placeHolder != null && state.value.let {
            it is AsyncImagePainter.State.Empty || it is AsyncImagePainter.State.Loading
        }
    }

    private inline fun Painter.requirePlaceholder(): Painter = (this as KuiklyPainter).placeHolder!!

    private fun Painter.visible(): Boolean {
        return if (this is AsyncImagePainter) {
            intrinsicSize.isSpecified ||
            state.value is AsyncImagePainter.State.Success ||
            state.value is AsyncImagePainter.State.Error
        } else {
            true
        }
    }

    private fun applyCommon(imageView: ImageView) {
        val colorFilter = this.colorFilter
        imageView.getViewAttr().apply {
            if (colorFilter is BlendModeColorFilter && colorFilter.blendMode.isSupported()) {
                tintColor(colorFilter.color.toKuiklyColor())
            } else if (getProp(ImageConst.TINT_COLOR) != null) {
                tintColor(null)
            }
        }
    }

    override fun toString(): String =
        "PainterModifier(" +
            "painter=$painter, " +
            "sizeToIntrinsics=$sizeToIntrinsics, " +
            "alignment=$alignment, " +
            "alpha=$alpha"//, " +
            // "colorFilter=$colorFilter)"
}
