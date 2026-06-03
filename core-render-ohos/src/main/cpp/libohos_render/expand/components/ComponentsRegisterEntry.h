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
#include "libohos_render/expand/components/input/KRTextEditorSwitch.h"
#if KUIKLY_TEXT_EDITOR_AVAILABLE
#include "libohos_render/expand/components/input/KRTextEditorAreaView.h"
#include "libohos_render/expand/components/input/KRTextEditorFieldView.h"
#endif
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
    // 注册策略（β 方案 + γ-2 弱链接补丁）：
    //   * KRTextFieldView 永远走老实现，与开关/SDK 版本都无关。
    //     保证单行输入场景零变化，避免新控件仅仅为了 emoji 就头上一堆 API 24 依赖。
    //   * KRTextAreaView 受开关与运行时 API 双重门控：
    //       1）KRIsTextEditorRuntimeAvailable() == 1（关键 API 24 弱符号已被
    //          动态链接器解析成功）；
    //       2）全局开关 KRGetUseNewTextInputComponent() == 1；
    //     两者任一不满足都回退到老 KRTextAreaView，保证默认行为与历史一致。
    //
    // 为何不再直接用 OH_GetSdkApiVersion() >= 24？
    //   * 单纯版本号判断在加载阶段已经太晚——loader 解析 .so 重定位时若发现 API 24
    //     符号未解析（系统真实是 API 20），整个 libkuikly.so 加载就会失败：
    //       "Error relocating /data/storage/.../libkuikly.so:
    //          OH_ArkUI_TextEditorStyledStringController_Create: symbol not found"
    //   * 修复要点：把 API 24 符号声明为 __attribute__((weak))（声明集中在
    //     KRTextEditorCommon.h 顶部），未解析时取 nullptr，loader 不会失败；
    //     注册分支改用 KRIsTextEditorRuntimeAvailable() 判 nullptr → 走老控件。
    //
    // 同时叠加编译期 guard：只有编译期 SDK header >= 24（TEXT_EDITOR 相关 API 可用）
    // 才考虑新控件，否则新控件的 CreateNode/DidInit 等都被裁剪，为避免"低 header 编译
    // + 高版本设备"错位下选中空壳类型，这里仍用预处理包护。
#if KUIKLY_TEXT_EDITOR_AVAILABLE
    // KRTextFieldView（单行输入）：永远走老实现。
    IKRRenderViewExport::RegisterViewCreator("KRTextFieldView",
                                             [] { return std::make_shared<KRTextFieldView>(); });

    // KRTextAreaView（多行输入）：双重门控后选择新/老实现。
    // 运行时弱符号探测在注册期一次性完成，避免每次创建实例都走 nullptr 判断。
    const int gTextEditorAvailable = KRIsTextEditorRuntimeAvailable();
    IKRRenderViewExport::RegisterViewCreator("KRTextAreaView", [gTextEditorAvailable] {
        if (gTextEditorAvailable == 1 && KRGetUseNewTextInputComponent() == 1) {
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
