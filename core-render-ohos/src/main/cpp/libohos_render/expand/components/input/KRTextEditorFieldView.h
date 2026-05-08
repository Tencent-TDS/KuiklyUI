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

#ifndef CORE_RENDER_OHOS_KRTEXTEDITORFIELDVIEW_H
#define CORE_RENDER_OHOS_KRTEXTEDITORFIELDVIEW_H

#include <cstddef>
#include <cstdint>

#include "libohos_render/expand/components/input/KRTextEditorCommon.h"
#include "libohos_render/export/IKRRenderViewExport.h"

/**
 * 全局开关：是否启用新的 ARKUI_NODE_TEXT_EDITOR 版输入控件。
 * 0（默认）=走老的 KRTextFieldView/KRTextAreaView；1=在 API>=24 时走新的
 * KRTextEditorFieldView/KRTextEditorAreaView。
 *
 * 仅在 ComponentsRegisterEntry 的 creator lambda 中读取，因此对已创建的 View
 * 不生效，只对「下次 Input/TextArea 创建」生效。
 *
 * 写入点位于宿主侧（如 ohosApp 的 napi 模块 libkuikly_entry.so），通过链接
 * libkuikly.so 调用下列 C API 完成跨 so 传值——内部值维护在 libkuikly.so 的
 * 一个静态变量中，ComponentsRegisterEntry 从同一 so 读取，保证一致性。
 * 注意：切忌再用 inline 变量 + 跨 so 访问，会因每个 so 各有一份副本而失效。
 */
extern "C" __attribute__((visibility("default"))) void KRSetUseNewTextInputComponent(int value);
extern "C" __attribute__((visibility("default"))) int KRGetUseNewTextInputComponent();

/**
 * 基于 ARKUI_NODE_TEXT_EDITOR（OpenHarmony API 24 新增）的单行输入实现，与老
 * KRTextFieldView（基于 ARKUI_NODE_TEXT_INPUT）并列存在。
 * 运行时选择见 ComponentsRegisterEntry.h：API 24+ 走本实现，否则走老实现。
 */
class KRTextEditorFieldView : public IKRRenderViewExport {
 public:
    ArkUI_NodeHandle CreateNode() override;
    void DidInit() override;
    void OnDestroy() override;
    bool SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                 const KRRenderCallback event_call_back = nullptr) override;
    void OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) override;
    void CallMethod(const std::string &method, const KRAnyValue &params,
                    const KRRenderCallback &callback) override;

 protected:
    // 多行派生类通过以下 hook 改变默认行为
    virtual bool IsSingleLine() const {
        return true;
    }
    // 是否拦截换行符（单行拦截、多行允许）
    virtual bool InterceptNewline() const {
        return true;
    }

    kuikly::text_editor::KRTextEditorState state_;

    // controller 首次绑定（DidInit 里调用）
    void InitControllerIfNeeded();

    // 文本 / 光标操作
    void SetContentText(const std::string &text);
    // 内部静默写入（不触发 textDidChange）。长度限制等内部链路使用，
    // 由外层统一收尾发一次回调，避免双回调。
    void SetContentTextSilent(const std::string &text);
    std::string GetContentText();
    uint32_t GetSelectionStartPosition();
    void SetSelectionStartPosition(uint32_t index);

    // Focus/Blur
    void Focus();
    void Blur();
    void GetCursorIndex(const KRRenderCallback &callback);
    void SetCursorIndex(uint32_t index);

    // 事件分发入口
    void OnTextDidChanged(ArkUI_NodeEvent *event);
    void OnInputFocus(ArkUI_NodeEvent *event);
    void OnInputBlur(ArkUI_NodeEvent *event);
    void OnInputReturn(ArkUI_NodeEvent *event);
    void OnWillChangeText(ArkUI_NodeEvent *event);
    void OnPasteText(ArkUI_NodeEvent *event);

    // max-length 过滤
    bool LimitInputContentTextInMaxLength();
    void NotifyTextLengthBeyondLimit();
    void SetupLengthInputFilter();
    void DoResetMaxLength();

    // keyboardType 映射（单/多行对 password 支持不同）
    virtual void ApplyKeyboardType(const std::string &type);

    // returnKeyType 映射
    void ApplyReturnKeyType(const std::string &type);
};

#endif  // CORE_RENDER_OHOS_KRTEXTEDITORFIELDVIEW_H
