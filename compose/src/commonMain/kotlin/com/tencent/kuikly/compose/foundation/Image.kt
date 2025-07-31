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

package com.tencent.kuikly.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.key
import com.tencent.kuikly.compose.KuiklyApplier
import com.tencent.kuikly.compose.extension.shouldWrapShadowView
import com.tencent.kuikly.compose.foundation.layout.EmptyBoxMeasurePolicy
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.KuiklyPainter
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.ColorFilter
import com.tencent.kuikly.compose.ui.graphics.DefaultAlpha
import com.tencent.kuikly.compose.ui.graphics.painter.Painter
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.node.ComposeUiNode
import com.tencent.kuikly.compose.ui.node.KNode
import com.tencent.kuikly.compose.ui.semantics.Role
import com.tencent.kuikly.compose.ui.semantics.contentDescription
import com.tencent.kuikly.compose.ui.semantics.role
import com.tencent.kuikly.compose.ui.semantics.semantics
import com.tencent.kuikly.compose.ui.draw.paintInternal
import com.tencent.kuikly.compose.ui.materialize
import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.views.DivView
import com.tencent.kuikly.core.views.ImageView

/**
 * Creates a composable that lays out and draws a given [Painter]. This will attempt to size
 * the composable according to the [Painter]'s intrinsic size. However, an optional [Modifier]
 * parameter can be provided to adjust sizing or draw additional content (ex. background)
 *
 * **NOTE** a Painter might not have an intrinsic size, so if no LayoutModifier is provided
 * as part of the Modifier chain this might size the [Image] composable to a width and height
 * of zero and will not draw any content. This can happen for Painter implementations that
 * always attempt to fill the bounds like [ColorPainter]
 *
 * @sample com.tencent.kuikly.compose.foundation.samples.BitmapPainterSample
 *
 * @param painter to draw
 * @param contentDescription text used by accessibility services to describe what this image
 * represents. This should always be provided unless this image is used for decorative purposes,
 * and does not represent a meaningful action that a user can take. This text should be
 * localized, such as by using [com.tencent.kuikly.compose.ui.res.stringResource] or similar
 * @param modifier Modifier used to adjust the layout algorithm or draw decoration content (ex.
 * background)
 * @param alignment Optional alignment parameter used to place the [Painter] in the given
 * bounds defined by the width and height.
 * @param contentScale Optional scale parameter used to determine the aspect ratio scaling to be used
 * if the bounds are a different size from the intrinsic size of the [Painter]
 * @param alpha Optional opacity to be applied to the [Painter] when it is rendered onscreen
 * the default renders the [Painter] completely opaque
 * @param colorFilter Optional colorFilter to apply for the [Painter] when it is rendered onscreen
 */
@Composable
fun Image(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.Center,
    contentScale: ContentScale = ContentScale.Fit,
    alpha: Float = DefaultAlpha,
    colorFilter: ColorFilter? = null
) {
    val combined = currentComposer.materialize(modifier).then(
        if (contentDescription != null) {
            Modifier.semantics {
                this.contentDescription = contentDescription
                this.role = Role.Image
            }
        } else {
            Modifier
        }
    ).paintInternal(
        painter = painter,
        alignment = alignment,
        contentScale = contentScale,
        alpha = alpha,
        colorFilter = colorFilter
    )

    val compositeKeyHash = currentCompositeKeyHash
    val localMap = currentComposer.currentCompositionLocalMap

    val needWrap = needWrap(contentScale, alignment)

    val hasShadow = shouldWrapShadowView(combined)
    val hasPlaceholder = painter is KuiklyPainter && painter.placeHolder is KuiklyPainter
    val loadState = (painter as? KuiklyPainter)?.state?.collectAsState()

    key(needWrap, hasShadow, hasPlaceholder) {
        ReusableComposeNode<ComposeUiNode, KuiklyApplier>(
            factory = {
                val imageViewWrap = if (needWrap || hasPlaceholder) {
                    ImageViewWrap(hasPlaceholder)
                } else {
                    ImageView()
                }
                KNode(imageViewWrap) {
                    if (hasShadow) {
                        getViewAttr().setProp(Attr.StyleConst.WRAPPER_BOX_SHADOW_VIEW, 1)
                    }
                }
            },
            update = {
                set(EmptyBoxMeasurePolicy, ComposeUiNode.SetMeasurePolicy)
                set(localMap, ComposeUiNode.SetResolvedCompositionLocals)
                @OptIn(ExperimentalComposeUiApi::class)
                set(compositeKeyHash, ComposeUiNode.SetCompositeKeyHash)
                set(combined) {
                    this.modifier = combined
                }
                set(painter.intrinsicSize) {
                    (this as KNode<*>).invalidateDraw()
                }
                set(loadState?.value) {
                    (this as KNode<*>).invalidateDraw()
                }
            },
        )
    }
}

private fun needWrap(contentScale: ContentScale, alignment: Alignment): Boolean {
    return when (contentScale) {
        // 拉伸不需要包裹
        ContentScale.FillBounds -> false
        // 填充、适应+居中可以用ImageView的resizeMode实现，其它情况需要包裹
        ContentScale.Crop -> alignment != Alignment.Center
        ContentScale.Fit -> alignment != Alignment.Center
        ContentScale.FillHeight -> alignment != Alignment.Center && alignment != Alignment.TopCenter && alignment != Alignment.BottomCenter
        ContentScale.FillWidth -> alignment != Alignment.Center && alignment != Alignment.CenterStart && alignment != Alignment.CenterEnd

        // Inside、None会被裁切，需要包裹
        ContentScale.Inside,
        ContentScale.None -> true

        else -> true
    }
}

internal class ImageViewWrap(val placeholder: Boolean) : DivView() {
    lateinit var imageView: ImageView
        private set
    lateinit var placeholderView: ImageView
        private set
    override fun isRenderView() = true
    override fun didInit() {
        super.didInit()
        imageView = ImageView()
        addChild(imageView) {}
        if (placeholder) {
            placeholderView = ImageView()
            addChild(placeholderView) {}
        }
    }
}