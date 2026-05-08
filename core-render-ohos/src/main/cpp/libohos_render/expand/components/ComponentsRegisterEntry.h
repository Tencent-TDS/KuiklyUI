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
#include "libohos_render/expand/components/input/KRTextEditorAreaView.h"
#include "libohos_render/expand/components/input/KRTextEditorFieldView.h"
#include "libohos_render/expand/components/input/KRTextFieldView.h"
#include "libohos_render/expand/components/mask/KRMaskView.h"
#include "libohos_render/expand/components/modal/KRModalView.h"
#include "libohos_render/expand/components/richtext/KRRichTextShadow.h"
#include "libohos_render/expand/components/richtext/KRRichTextView.h"
#include "libohos_render/expand/components/richtext/gradient_richtext/KRGradientRichTextShadow.h"
#include "libohos_render/expand/components/richtext/gradient_richtext/KRGradientRichTextView.h"
#include "libohos_render/expand/components/scroller/KRScrollerView.h"
#include "libohos_render/expand/components/view/KRView.h"
#include "libohos_render/export/IKRRenderViewExport.h"
#include "libohos_render/view/IKRRenderView.h"

#include <deviceinfo.h>

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

    // 运行期 SDK API 版本，注册期查询一次即可。
    // 仅在编译期 SDK header >= 24（TEXT_EDITOR 相关 API 可用）时才考虑新控件，
    // 否则即便运行设备版本足够高、开关打开，新控件的 CreateNode/DidInit 等也都是空壳，
    // 会导致输入框不工作——此处用预处理保护，彻底规避"低 header 编译 + 高版本设备"的错位风险。
#ifndef KUIKLY_TEXT_EDITOR_UNAVAILABLE
    const int gApiVersion = OH_GetSdkApiVersion();
    // single line - text input (单行输入框)
    // 需同时满足：1）运行期 SDK API 版本 >= 24（ARKUI_NODE_TEXT_EDITOR 可用）；
    //          2）全局开关 KRGetUseNewTextInputComponent() == 1。
    // 两者任一不满足都回退到老的 KRTextFieldView，保证默认行为与历史一致。
    IKRRenderViewExport::RegisterViewCreator("KRTextFieldView", [gApiVersion] {
        if (gApiVersion >= 24 && KRGetUseNewTextInputComponent() == 1) {
            return std::static_pointer_cast<IKRRenderViewExport>(
                std::make_shared<KRTextEditorFieldView>());
        }
        return std::static_pointer_cast<IKRRenderViewExport>(std::make_shared<KRTextFieldView>());
    });

    // multi-line text input
    IKRRenderViewExport::RegisterViewCreator("KRTextAreaView", [gApiVersion] {
        if (gApiVersion >= 24 && KRGetUseNewTextInputComponent() == 1) {
            return std::static_pointer_cast<IKRRenderViewExport>(
                std::make_shared<KRTextEditorAreaView>());
        }
        return std::static_pointer_cast<IKRRenderViewExport>(std::make_shared<KRTextAreaView>());
    });
#else
    // 编译期 SDK header < 24，TEXT_EDITOR 相关类型均为空壳，直接注册老控件。
    IKRRenderViewExport::RegisterViewCreator("KRTextFieldView",
                                             [] { return std::make_shared<KRTextFieldView>(); });
    IKRRenderViewExport::RegisterViewCreator("KRTextAreaView",
                                             [] { return std::make_shared<KRTextAreaView>(); });
#endif

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
    
    // Mask
    IKRRenderViewExport::RegisterViewCreator("KRMaskView", [] { return std::make_shared<KRMaskView>(); });
}

#endif  // CORE_RENDER_OHOS_COMPONENTSREGISTERENTRY_H
