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
#include "libohos_render/expand/components/mask/KRMaskView.h"

#include <arkui/native_type.h>
#include <arkui/native_node.h>
#include "libohos_render/utils/KRViewUtil.h"

using namespace kuikly::util;

void KRMaskView::DidInit() {
    KRView::DidInit();
    
    // 开启裁剪，防止子节点内容溢出 MaskView 边界
    ArkUI_NumberValue clipValue[] = {{.i32 = 1}};
    ArkUI_AttributeItem clipItem = {clipValue, 1};
    
    GetNodeApi()->setAttribute(GetNode(), NODE_CLIP, &clipItem);
    
    // OFFSCREEN 让所有子节点先渲染到同一离屏缓冲区再混合，确保设置 BlendMode 在KTView上后，其子view也可以响应混合的效果
    SetNodeBlendMode(GetNode(), ARKUI_BLEND_MODE_SRC_OVER, BLEND_APPLY_TYPE_OFFSCREEN);
}

void KRMaskView::SetNodeBlendMode(ArkUI_NodeHandle node, ArkUI_BlendMode blendMode, ArkUI_BlendApplyType applyType) {
    if (!node) {
        return;
    }

    ArkUI_NumberValue blendValue[] = {{.i32 = blendMode}, {.i32 = applyType}};
    ArkUI_AttributeItem blendItem = {blendValue, sizeof(blendValue) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(node, NODE_BLEND_MODE, &blendItem);
}

void KRMaskView::DidInsertSubRenderView(const std::shared_ptr<IKRRenderViewExport> &sub_render_view, int index) {
    if (!sub_render_view) {
        return;   
    }
    // index=0: maskFromView 容器（遮罩源，提供 alpha 通道）, 不做处理
    
    // index=1: maskToView 容器（目标内容），设置 SRC_IN 混合模式
    if (index == 1) {
        SetNodeBlendMode(sub_render_view->GetNode(), ARKUI_BLEND_MODE_SRC_IN, BLEND_APPLY_TYPE_OFFSCREEN);
    }
}

