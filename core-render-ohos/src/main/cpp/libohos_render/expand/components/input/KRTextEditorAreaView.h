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

#ifndef CORE_RENDER_OHOS_KRTEXTEDITORAREAVIEW_H
#define CORE_RENDER_OHOS_KRTEXTEDITORAREAVIEW_H

#include "libohos_render/expand/components/input/KRTextEditorFieldView.h"

/**
 * 基于 ARKUI_NODE_TEXT_EDITOR 的多行输入实现。
 * 与 KRTextEditorFieldView 共享绝大多数代码，仅差异：
 *   * 不强制 SingleLine
 *   * 不拦截换行符
 *   * keyboardType == "password" 打 warn 并降级（对齐老 KRTextAreaView 语义）
 *   * 多一个 lineHeight 属性（与老 KRTextAreaView 一致）
 */
class KRTextEditorAreaView : public KRTextEditorFieldView {
 public:
    void DidInit() override;
    bool SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                 const KRRenderCallback event_call_back = nullptr) override;

 protected:
    bool IsSingleLine() const override {
        return false;
    }
    bool InterceptNewline() const override {
        return false;
    }
    void ApplyKeyboardType(const std::string &type) override;
};

#endif  // CORE_RENDER_OHOS_KRTEXTEDITORAREAVIEW_H
