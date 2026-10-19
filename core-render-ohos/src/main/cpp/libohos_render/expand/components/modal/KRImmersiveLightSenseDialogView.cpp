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

#include "libohos_render/expand/components/modal/KRImmersiveLightSenseDialogView.h"

#include <cstdlib>
#include <string>
#include <vector>

#include "libohos_render/utils/KRConvertUtil.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/utils/KRViewUtil.h"

namespace {
// Kuikly 通用属性键。这里只转发"通用 Modal 本来就有的能力"，
// 不引入 customStyle / gridColumnCount 这类鸿蒙独有的开关。
constexpr char kPropBorderRadius[] = "borderRadius";
constexpr char kPropBackgroundColor[] = "backgroundColor";
constexpr char kPropBorder[] = "border";

// Kuikly Border.toString() 的格式： "<lineWidth> <lineStyle> <color>"
// 例： "2 solid 4294967295"
void ParseBorder(const std::string &value, float &out_width, int32_t &out_style, uint32_t &out_color) {
    std::vector<std::string> parts;
    std::string token;
    for (char c : value) {
        if (c == ' ') {
            if (!token.empty()) {
                parts.push_back(token);
                token.clear();
            }
        } else {
            token.push_back(c);
        }
    }
    if (!token.empty()) {
        parts.push_back(token);
    }
    if (parts.size() >= 1) {
        out_width = static_cast<float>(atof(parts[0].c_str()));
    }
    if (parts.size() >= 2) {
        if (parts[1] == "dashed") {
            out_style = 1;
        } else if (parts[1] == "dotted") {
            out_style = 2;
        } else {
            out_style = 0;
        }
    }
    if (parts.size() >= 3) {
        out_color = kuikly::util::ConvertToHexColor(parts[2]);
    }
}
}  // namespace

void KRImmersiveLightSenseDialogView::ToSetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                                                const KRRenderCallback event_call_back) {
    if (prop_value == nullptr) {
        KRModalView::ToSetProp(prop_key, prop_value, event_call_back);
        return;
    }
    // 圆角：通用属性 borderRadius → 弹窗背板圆角
    if (prop_key == kPropBorderRadius) {
        auto radiuses = kuikly::util::ConverToBorderRadiuses(prop_value->toString());
        corner_radius_ = radiuses.topLeft;
        KR_LOG_INFO << "ImmLightTest: dialog cornerRadius=" << corner_radius_;
    }
    // 背景色：通用属性 backgroundColor → 弹窗背板背景色
    if (prop_key == kPropBackgroundColor) {
        has_backplane_bg_ = true;
        backplane_bg_color_ = kuikly::util::ConvertToHexColor(prop_value->toString());
    }
    // 边框：只映射到弹窗背板，不再下发给内容节点。
    // 内容节点与背板是两个矩形，两边都画会出现"两条边对不上"的双框。
    if (prop_key == kPropBorder) {
        has_border_ = true;
        ParseBorder(prop_value->toString(), border_width_, border_style_, border_color_);
        KR_LOG_INFO << "ImmLightTest: dialog border w=" << border_width_ << ", style=" << border_style_
                    << ", color=" << border_color_;
        return;
    }
    KRModalView::ToSetProp(prop_key, prop_value, event_call_back);
    if (prop_key == kPropBorderRadius) {
        // Kuikly 在 borderRadius != 0 时会给节点强制加裁剪（force_overflow_）。
        // 弹窗的内容节点被 NativeDialog 接管后布局尺寸是 0，一加裁剪子节点就全被裁掉，
        // 表现为"背板正常、内部全空"。圆角由弹窗背板 setCornerRadius 负责，
        // 内容节点这里必须把裁剪关掉。
        kuikly::util::UpdateNodeOverflow(GetNode(), 0);
    }
}

void KRImmersiveLightSenseDialogView::SetRenderViewFrame(const KRRect &frame) {
    // 尺寸：沿用 Kuikly 通用布局结果作为背板显式尺寸，业务不用学新属性
    if (frame.width > 0) {
        modal_width_ = frame.width;
    }
    if (frame.height > 0) {
        modal_height_ = frame.height;
    }
    KRModalView::SetRenderViewFrame(frame);
}

void KRImmersiveLightSenseDialogView::DidMoveToParentView() {
    // customStyle 固定 false：这是背板材质生效的前提（true 时背板透明、无材质落点）。
    // 作为组件语义固化，不开放给业务，避免"设了没效果"的坑。
    custom_style_ = false;
    // 弹窗天然居中，同样属于组件默认行为而非可配属性
    align_center_ = true;
    // 材质默认启用：业务不设置 systemMaterialOhos 也有沉浸光感（默认 REGULAR 档）
    if (!has_system_material_) {
        has_system_material_ = true;
    }
    // 兜底：若圆角先于此处下发并已触发裁剪，这里再关一次（原因见 SetProp）
    kuikly::util::UpdateNodeOverflow(GetNode(), 0);
    KRModalView::DidMoveToParentView();
}

void KRImmersiveLightSenseDialogView::ApplyExtraBackplaneStyle(ArkUI_NativeDialogHandle dialog) {
    if (!has_border_ || dialog == nullptr || border_width_ <= 0) {
        return;
    }
    const ArkUI_NativeDialogAPI_3 *api3 = kuikly::util::GetDialogApi3OrNull();
    if (api3 == nullptr) {
        KR_LOG_INFO << "ImmLightTest: dialog border skipped (API_3 unavailable)";
        return;
    }
    if (api3->setBorderWidth != nullptr) {
        KR_LOG_INFO << "ImmLightTest: dialog setBorderWidth ret="
                    << api3->setBorderWidth(dialog, border_width_, border_width_, border_width_, border_width_,
                                            ARKUI_LENGTH_METRIC_UNIT_VP);
    }
    if (api3->setBorderColor != nullptr) {
        api3->setBorderColor(dialog, border_color_, border_color_, border_color_, border_color_);
    }
    if (api3->setBorderStyle != nullptr) {
        api3->setBorderStyle(dialog, border_style_, border_style_, border_style_, border_style_);
    }
}
