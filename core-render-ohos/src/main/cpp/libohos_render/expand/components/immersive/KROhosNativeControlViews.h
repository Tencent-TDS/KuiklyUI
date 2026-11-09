/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of the KuiklyUI;
 * you may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef CORE_RENDER_OHOS_KROHOSNATIVECONTROLVIEWS_H
#define CORE_RENDER_OHOS_KROHOSNATIVECONTROLVIEWS_H

#include "libohos_render/export/IKRRenderViewExport.h"

/**
 * 鸿蒙原生 Slider（ArkUI Slider），CAPI 直连。
 *
 * 为什么不用 ArkTS 转发（V2）：转发层会给普通控件套一层 ArkTS ↔ Kotlin 的桥，
 * 实测踩了两个坑 —— 转发节点的父尺寸在首次 build 时不可靠（百分比解析成巨值，
 * 控件被放大到铺满半屏）；事件回传的参数编码没有明确约定（框架按 JSON 解，
 * 裸字符串会抛 JSONException 并 abort 进程）。ActivityIndicator / Image / Input
 * 等原生控件都是 CAPI 直连，这里保持一致。
 *
 * 价值：官方规定 Slider / Toggle / Select / 弹窗类组件可**全页面生效**沉浸光感，
 * 不受「必须在 Navigation 标题栏或底部 TabBar 内」的位置限制。
 * 材质由基类 IKRRenderViewExport::ToSetProp 统一消费（NODE_SYSTEM_MATERIAL），
 * 本类不需要额外处理。
 */
class KROhosSliderView : public IKRRenderViewExport {
 public:
    ArkUI_NodeHandle CreateNode() override;
    void DidInit() override;
    bool SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                 const KRRenderCallback event_call_back = nullptr) override;
    void SetRenderViewFrame(const KRRect &frame) override;
    void OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) override;

 private:
    float current_value_ = 0.0f;
    /**
     * 值变化回调。事件必须以 KRRenderValueMap 回传，不能传裸字符串 ——
     * 框架侧按 JSON 解析，裸值会抛 JSONException 并 abort 进程。
     */
    KRRenderCallback value_changed_callback_;
};

/**
 * 鸿蒙原生 Switch（ArkUI Toggle，ToggleType.Switch），CAPI 直连。
 * 说明同上。
 */
class KROhosSwitchView : public IKRRenderViewExport {
 public:
    ArkUI_NodeHandle CreateNode() override;
    void DidInit() override;
    bool SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                 const KRRenderCallback event_call_back = nullptr) override;
    void SetRenderViewFrame(const KRRect &frame) override;
    void OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) override;

 private:
    /** 开关回调，回传格式要求同 KROhosSliderView */
    KRRenderCallback switch_changed_callback_;
};

#endif  // CORE_RENDER_OHOS_KROHOSNATIVECONTROLVIEWS_H
