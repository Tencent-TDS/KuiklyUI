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

#include "libohos_render/expand/components/input/KRTextEditorAreaView.h"

#include "libohos_render/utils/KRStringUtil.h"
#include "libohos_render/utils/KRViewUtil.h"

// 编译期 guard：同 KRTextEditorAreaView.h，运行期低 SDK header 下本类类型不存在，
// 为避免低版本上出现未声明类型、API 未声明等问题，本 TU 主体被同一宏整体
// guard。实现同文件同名类型在低版本只会被跳过。
#if KUIKLY_TEXT_EDITOR_AVAILABLE

void KRTextEditorAreaView::DidInit() {
    KRTextEditorFieldView::DidInit();
    // 多行模式：SingleLine 已由 IsSingleLine() == false 生效；此处可额外配置默认样式。
}

bool KRTextEditorAreaView::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                                   const KRRenderCallback event_call_back) {
    // 多行特有：lineHeight（对齐老 KRTextAreaView 支持）。
    // 设计要点：
    //   1. 仅持久化到 state_，由 ApplyTypingStyle / SetStyledText 统一从 state 推导，
    //      避免在 fontSize 等其它属性变更触发 ApplyTypingStyle 时把 lineHeight 误清掉。
    //   2. 立即重写已有文本 span（3.3-α 方案）——因为 typing style 只影响后续键入，
    //      不主动重写 span 的话当前已有文本视觉不会跟随变化。
    if (kuikly::util::isEqual(prop_key, kuikly::text_editor::kLineHeight)) {
        float new_lh = prop_value->toFloat();
        state_.line_height_ = new_lh;
        state_.line_height_set_ = new_lh > 0;
        if (state_.controller_) {
            // typing style：影响后续键入。
            kuikly::text_editor::ApplyTypingStyle(state_);
            // 已有文本：通过重写 SpanStyle 立即生效（含 LineHeightStyle）。
            kuikly::text_editor::SetStyledText(state_, state_.cached_text_);
        }
        return true;
    }
    return KRTextEditorFieldView::SetProp(prop_key, prop_value, event_call_back);
}

void KRTextEditorAreaView::ApplyKeyboardType(const std::string &type) {
    // 老 KRTextAreaView 仅支持 default/number/email，不支持 password。
    // TEXT_EDITOR 不暴露 keyboardType；这里同样打 warn 并降级为默认。
    //
    // 注意：曾尝试直接复用老控件的 UpdateInputNodeKeyboardType
    // （底层写 NODE_TEXT_INPUT_TYPE）在 TEXT_EDITOR 节点上 setAttribute，
    // 实测会 crash，不可跨节点作用域复用属性。
    if (type == "password") {
        KR_LOG_DEBUG << "KRTextEditorAreaView: multi-line does not support password keyboard";
        return;
    }
    if (type != "default" && !type.empty()) {
        KR_LOG_DEBUG << "KRTextEditorAreaView: keyboardType=" << type
                     << " is not supported on ARKUI_NODE_TEXT_EDITOR (API 24+), fallback to default";
    }
}

#endif  // KUIKLY_TEXT_EDITOR_AVAILABLE
