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

#include "libohos_render/expand/components/ActivityIndicator/KRActivityIndicatorView.h"

constexpr char *kStyle = "style";
ArkUI_NodeHandle KRActivityIndicatorView::CreateNode() {
    return kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_LOADING_PROGRESS);
}

bool KRActivityIndicatorView::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                                      const KRRenderCallback event_call_back) {
    if (kuikly::util::isEqual(prop_key, kStyle)) {  // 样式
        SetStyle(prop_value->toString());
        return true;
    }

    return IKRRenderViewExport::SetProp(prop_key, prop_value, event_call_back);
}

void KRActivityIndicatorView::SetStyle(const std::string &style) {
    if (style == "white") {
        kuikly::util::UpdateLoadingProgressNodeColor(GetNode(), 0xFFFFFFFF);
    } else {
        kuikly::util::UpdateLoadingProgressNodeColor(GetNode(), 0xFF979797);
    }
}

void KRActivityIndicatorView::SetRenderViewFrame(const KRRect &frame) {
    IKRRenderViewExport::SetRenderViewFrame(frame);
    auto scaleInset = 4;
    kuikly::util::UpdateNodeFrame(GetNode(), KRRect(frame.x - scaleInset, frame.y - scaleInset, frame.width + 2 * scaleInset,
                                      frame.height + 2 * scaleInset));
}
