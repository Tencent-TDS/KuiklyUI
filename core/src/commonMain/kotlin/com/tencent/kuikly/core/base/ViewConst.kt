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

package com.tencent.kuikly.core.base

object ViewConst {
    const val TYPE = "type"
    const val REF = "ref"
    const val STYLE = "style"
    const val ATTR = "attr"
    const val EVENT = "events"
    const val CHILDREN = "children"

    // 用户Pager#registerViewCreator中viewClassName所用
    const val TYPE_VIEW_CLASS_NAME = "DivView"
    const val TYPE_TEXT_CLASS_NAME = "TextView"
    const val TYPE_IMAGE_CLASS_NAME = "ImageView"
    const val TYPE_RICH_TEXT_CLASS_NAME = "RichTextView"

    // 映射到Native组件类名
    const val TYPE_VIEW = "KRView"
    const val TYPE_IMAGE = "KRImageView"
    const val TYPE_WRAPPER_IMAGE = "KRWrapperImageView"
    const val TYPE_RICH_TEXT = "KRRichTextView"
    const val TYPE_GRADIENT_RICH_TEXT = "KRGradientRichTextView"
    const val TYPE_LIST = "KRListView"
    const val TYPE_SCROLLER = "KRScrollView"
    const val TYPE_ACTIVITY_INDICATOR = "KRActivityIndicatorView"
    const val TYPE_CANVAS = "KRCanvasView"
    const val TYPE_HOVER = "KRHoverView"
    const val TYPE_TEXT_FIELD = "KRTextFieldView"
    const val TYPE_MASK = "KRMaskView"
    const val TYPE_TEXT_AREA = "KRTextAreaView"
    const val TYPE_SCROLL_CONTENT_VIEW = "KRScrollContentView"
    const val TYPE_BLUR_VIEW = "KRBlurView"
    const val TYPE_PAG_VIEW = "KRPAGView"
    const val TYPE_APNG_VIEW = "KRAPNGView"
    const val TYPE_VIDEO_VIEW = "KRVideoView"
    const val TYPE_MODAL_VIEW = "KRModalView"

}