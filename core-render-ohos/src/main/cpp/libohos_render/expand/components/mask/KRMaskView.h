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

#ifndef CORE_RENDER_OHOS_KRMASKVIEW_H
#define CORE_RENDER_OHOS_KRMASKVIEW_H

#include "libohos_render/expand/components/view/KRView.h"
#include "libohos_render/export/IKRRenderViewExport.h"

/// 遮罩视图：使用 BlendMode SRC_IN 实现，目标内容只在遮罩 alpha > 0 的区域显示
class KRMaskView : public KRView {
    
public:
    void DidInit() override;
    void DidInsertSubRenderView(const std::shared_ptr<IKRRenderViewExport> &sub_render_view, int index) override;
    
private:
    void SetNodeBlendMode(ArkUI_NodeHandle node, ArkUI_BlendMode blendMode, ArkUI_BlendApplyType applyType);
};

#endif // CORE_RENDER_OHOS_KRMASKVIEW_H
