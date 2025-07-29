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

#ifndef CORE_RENDER_OHOS_COMPONENTSREGISTERENTRY_H
#define CORE_RENDER_OHOS_COMPONENTSREGISTERENTRY_H

#include "libohos_render/expand/components/ActivityIndicator/KRActivityIndicatorAnimationView.h"
#include "libohos_render/expand/components/apng/KRApngView.h"
#include "libohos_render/expand/components/canvas/KRCanvasView.h"
#include "libohos_render/expand/components/forward/KRForwardArkTSView.h"
#include "libohos_render/expand/components/forward/KRForwardArkTSViewV2.h"
#include "libohos_render/expand/components/hover/KRHoverView.h"
#include "libohos_render/expand/components/image/KRImageView.h"
#include "libohos_render/expand/components/image/KRImageViewWrapper.h"
#include "libohos_render/expand/components/input/KRTextAreaView.h"
#include "libohos_render/expand/components/input/KRTextFieldView.h"
#include "libohos_render/expand/components/modal/KRModalView.h"
#include "libohos_render/expand/components/richtext/KRRichTextShadow.h"
#include "libohos_render/expand/components/richtext/KRRichTextView.h"
#include "libohos_render/expand/components/richtext/gradient_richtext/KRGradientRichTextShadow.h"
#include "libohos_render/expand/components/richtext/gradient_richtext/KRGradientRichTextView.h"
#include "libohos_render/expand/components/scroller/KRScrollerView.h"
#include "libohos_render/expand/components/view/KRView.h"
#include "libohos_render/export/IKRRenderViewExport.h"
#include "libohos_render/view/IKRRenderView.h"

/**
 * 内置组件均在此注册生成实例闭包
 */
static void ComponentsRegisterEntry() {
    // 注册通用转发ArkTS侧View组件
    IKRRenderViewExport::RegisterForwardArkTSViewCreator([] { return std::make_shared<KRForwardArkTSView>(); });
    IKRRenderViewExport::RegisterForwardArkTSViewCreatorV2([] { return std::make_shared<KRForwardArkTSViewV2>(); });

    IKRRenderViewExport::RegisterViewCreator("KRView", [] { return std::make_shared<KRView>(); });
    IKRRenderViewExport::RegisterViewCreator("KRImageView", [] { return std::make_shared<KRImageView>(); });

    IKRRenderViewExport::RegisterViewCreator("KRWrapperImageView",
                                             [] { return std::make_shared<KRImageViewWrapper>(); });

    IKRRenderViewExport::RegisterViewCreator("KRRichTextView", [] { return std::make_shared<KRRichTextView>(); });

    IKRRenderShadowExport::RegisterShadowCreator("KRRichTextView",
                                                 [] { return std::make_shared<KRGradientRichTextShadow>(); });

    IKRRenderViewExport::RegisterViewCreator("KRGradientRichTextView",
                                             [] { return std::make_shared<KRGradientRichTextView>(); });

    IKRRenderShadowExport::RegisterShadowCreator("KRGradientRichTextView",
                                                 [] { return std::make_shared<KRGradientRichTextShadow>(); });

    IKRRenderViewExport::RegisterViewCreator("KRListView", [] { return std::make_shared<KRScrollerView>(); });
    IKRRenderViewExport::RegisterViewCreator("KRScrollView", [] { return std::make_shared<KRScrollerView>(); });
    IKRRenderViewExport::RegisterViewCreator("KRScrollContentView",
                                             [] { return std::make_shared<KRScrollerContentView>(); });
    // single line - text input (单行输入框)
    IKRRenderViewExport::RegisterViewCreator("KRTextFieldView", [] { return std::make_shared<KRTextFieldView>(); });

    // multi-line text input
    IKRRenderViewExport::RegisterViewCreator("KRTextAreaView", [] { return std::make_shared<KRTextAreaView>(); });

    // modal
    IKRRenderViewExport::RegisterViewCreator("KRModalView", [] { return std::make_shared<KRModalView>(); });

    // 活动指示器
    IKRRenderViewExport::RegisterViewCreator("KRActivityIndicatorView",
                                             [] { return std::make_shared<KRActivityIndicatorAnimationView>(); });

    // Hover置顶
    IKRRenderViewExport::RegisterViewCreator("KRHoverView", [] { return std::make_shared<KRHoverView>(); });

    // APNG
    IKRRenderViewExport::RegisterViewCreator("KRAPNGView", [] { return std::make_shared<KRApngView>(); });
    IKRRenderViewExport::RegisterViewCreator("HRAPNGView", [] { return std::make_shared<KRApngView>(); });

    // canvas
    IKRRenderViewExport::RegisterViewCreator("KRCanvasView", [] { return std::make_shared<KRCanvasView>(); });
}

#endif  // CORE_RENDER_OHOS_COMPONENTSREGISTERENTRY_H
