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

#include "libohos_render/expand/components/ohosnative/KROhosNativeControlViews.h"

#include <memory>
#include <string>

#include "libohos_render/foundation/KRCommon.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/utils/KRViewUtil.h"

namespace {
// Kotlin 侧 OhosSliderAttr / OhosSwitchAttr 下发的属性键
constexpr char kPropValue[] = "value";
constexpr char kPropMin[] = "min";
constexpr char kPropMax[] = "max";
constexpr char kPropIsOn[] = "isOn";
// Kotlin 侧 OhosSliderEvent / OhosSwitchEvent 注册的事件键
constexpr char kEventValueChanged[] = "valueChanged";
constexpr char kEventSwitchChanged[] = "switchChanged";

void SetFloatAttribute(ArkUI_NodeHandle node, ArkUI_NodeAttributeType type, float value) {
    // ArkUI_AttributeItem.value 是 const ArkUI_NumberValue*（数组），不是标量
    ArkUI_NumberValue number = {.f32 = value};
    ArkUI_AttributeItem item = {.value = &number, .size = 1};
    kuikly::util::GetNodeApi()->setAttribute(node, type, &item);
}

void SetBoolAttribute(ArkUI_NodeHandle node, ArkUI_NodeAttributeType type, bool value) {
    ArkUI_NumberValue number = {.i32 = value ? 1 : 0};
    ArkUI_AttributeItem item = {.value = &number, .size = 1};
    kuikly::util::GetNodeApi()->setAttribute(node, type, &item);
}
}  // namespace

/**
 * 必须重写：基类 IKRRenderViewExport::SetRenderViewFrame 是空实现，
 * frame 不会自动落到原生节点上。不写这一步，节点既没有尺寸也没有位置，
 * 表现为"属性都下发成功了但屏幕上什么都看不到"。
 */
void KROhosSliderView::SetRenderViewFrame(const KRRect &frame) {
    IKRRenderViewExport::SetRenderViewFrame(frame);
    kuikly::util::UpdateNodeFrame(GetNode(), frame);
    // 诊断：确认 Kuikly 算出的 frame 是否到达（全 0 说明上层没算或没下发）
    KR_LOG_INFO << "ImmLightTest: [slider] frame=" << frame.width << "x" << frame.height
                << "@(" << frame.x << "," << frame.y << ")";
}

// ------------------------------------------------------------------ Slider

ArkUI_NodeHandle KROhosSliderView::CreateNode() {
    KR_LOG_INFO << "ImmLightTest: [slider] createNode";
    return kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_SLIDER);
}

void KROhosSliderView::DidInit() {
    KR_LOG_INFO << "ImmLightTest: [slider] didInit, node=" << GetNode();
    RegisterEvent(NODE_SLIDER_EVENT_ON_CHANGE);
    // OutSet 样式（滑块在轨道上），与 Kuikly 自绘 Slider 的观感一致
    SetFloatAttribute(GetNode(), NODE_SLIDER_STYLE, 0.0f);
    // 默认区间 0~100，业务可通过 min/max 覆盖
    SetFloatAttribute(GetNode(), NODE_SLIDER_MIN_VALUE, 0.0f);
    SetFloatAttribute(GetNode(), NODE_SLIDER_MAX_VALUE, 100.0f);
}

bool KROhosSliderView::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                               const KRRenderCallback event_call_back) {
    KR_LOG_INFO << "ImmLightTest: [slider] setProp key=" << prop_key
                << ", hasCallback=" << (event_call_back != nullptr);
    // 事件注册：以回调形式下发，此时 prop_value 为空
    if (prop_key == kEventValueChanged) {
        value_changed_callback_ = event_call_back;
        KR_LOG_INFO << "ImmLightTest: [slider] valueChanged event registered";
        return true;
    }
    if (prop_value == nullptr) {
        return IKRRenderViewExport::SetProp(prop_key, prop_value, event_call_back);
    }
    if (prop_key == kPropValue) {
        current_value_ = prop_value->toFloat();
        SetFloatAttribute(GetNode(), NODE_SLIDER_VALUE, current_value_);
        return true;
    }
    if (prop_key == kPropMin) {
        SetFloatAttribute(GetNode(), NODE_SLIDER_MIN_VALUE, prop_value->toFloat());
        return true;
    }
    if (prop_key == kPropMax) {
        SetFloatAttribute(GetNode(), NODE_SLIDER_MAX_VALUE, prop_value->toFloat());
        return true;
    }
    return IKRRenderViewExport::SetProp(prop_key, prop_value, event_call_back);
}

void KROhosSliderView::OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) {
    if (event_type != NODE_SLIDER_EVENT_ON_CHANGE) {
        return;
    }
    // 从事件里取当前值（index 0 是 value）。
    // API 是 out 参数形式：OH_ArkUI_NodeEvent_GetNumberValue(event, index, &value)
    ArkUI_NumberValue number = {0};
    OH_ArkUI_NodeEvent_GetNumberValue(event, 0, &number);
    current_value_ = number.f32;
    KR_LOG_INFO << "ImmLightTest: ohos slider onChange value=" << current_value_;
    KR_LOG_INFO << "ImmLightTest: [slider] onChange callback=" << (value_changed_callback_ != nullptr);
    if (value_changed_callback_) {
        // 必须以 map 回传：框架按 JSON 解析，传裸字符串会抛 JSONException 并 abort 进程
        KRRenderValueMap map;
        map["value"] = NewKRRenderValue(std::to_string(current_value_));
        // KRRenderValue 构造函数是 protected，必须用工厂函数 NewKRRenderValue 包 map
        value_changed_callback_(NewKRRenderValue(map));
    }
}

/** 同 Slider：基类 SetRenderViewFrame 是空的，必须显式把 frame 落到节点 */
void KROhosSwitchView::SetRenderViewFrame(const KRRect &frame) {
    IKRRenderViewExport::SetRenderViewFrame(frame);
    kuikly::util::UpdateNodeFrame(GetNode(), frame);
    KR_LOG_INFO << "ImmLightTest: [switch] frame=" << frame.width << "x" << frame.height
                << "@(" << frame.x << "," << frame.y << ")";
}

// ------------------------------------------------------------------ Switch（Toggle）

ArkUI_NodeHandle KROhosSwitchView::CreateNode() {
    KR_LOG_INFO << "ImmLightTest: [switch] createNode";
    return kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_TOGGLE);
}

void KROhosSwitchView::DidInit() {
    KR_LOG_INFO << "ImmLightTest: [switch] didInit, node=" << GetNode();
    // 注意：Toggle 的事件枚举是 NODE_TOGGLE_ON_CHANGE（不是 *_EVENT_ON_CHANGE）
    RegisterEvent(NODE_TOGGLE_ON_CHANGE);
    SetBoolAttribute(GetNode(), NODE_TOGGLE_VALUE, false);
}

bool KROhosSwitchView::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                               const KRRenderCallback event_call_back) {
    KR_LOG_INFO << "ImmLightTest: [switch] setProp key=" << prop_key
                << ", hasCallback=" << (event_call_back != nullptr);
    // 事件注册：以回调形式下发，此时 prop_value 为空
    if (prop_key == kEventSwitchChanged) {
        switch_changed_callback_ = event_call_back;
        KR_LOG_INFO << "ImmLightTest: [switch] switchChanged event registered";
        return true;
    }
    if (prop_value == nullptr) {
        return IKRRenderViewExport::SetProp(prop_key, prop_value, event_call_back);
    }
    if (prop_key == kPropIsOn) {
        const bool on = prop_value->toBool() || prop_value->toString() == "1";
        SetBoolAttribute(GetNode(), NODE_TOGGLE_VALUE, on);
        return true;
    }
    return IKRRenderViewExport::SetProp(prop_key, prop_value, event_call_back);
}

void KROhosSwitchView::OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) {
    if (event_type != NODE_TOGGLE_ON_CHANGE) {
        return;
    }
    ArkUI_NumberValue number = {0};
    OH_ArkUI_NodeEvent_GetNumberValue(event, 0, &number);
    const bool is_on = number.i32 != 0;
    KR_LOG_INFO << "ImmLightTest: [switch] onChange isOn=" << (is_on ? 1 : 0)
                << ", callback=" << (switch_changed_callback_ != nullptr);
    if (switch_changed_callback_) {
        // 同 Slider：必须以 map 回传
        KRRenderValueMap map;
        map["isOn"] = NewKRRenderValue(is_on ? "1" : "0");
        // 同 Slider：构造函数 protected，用工厂函数
        switch_changed_callback_(NewKRRenderValue(map));
    }
}
