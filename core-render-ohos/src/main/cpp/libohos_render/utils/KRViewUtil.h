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

#ifndef CORE_RENDER_OHOS_KRVIEWUTIL_H
#define CORE_RENDER_OHOS_KRVIEWUTIL_H

#include <ace/xcomponent/native_interface_xcomponent.h>
#include <arkui/native_animate.h>
#include <arkui/native_dialog.h>
#include <arkui/native_interface.h>
#include <arkui/native_node.h>
#include <multimedia/image_framework/image_pixel_map_mdk.h>
#include <native_drawing/drawing_font_collection.h>
#include <native_drawing/drawing_point.h>
#include <native_drawing/drawing_text_declaration.h>
#include <native_drawing/drawing_text_typography.h>
#include <sys/stat.h>
#include <unordered_set>
#include <vector>
#include "libohos_render/expand/components/base/KRBasePropsHandler.h"
#include "libohos_render/expand/components/base/animation/KRNodeAnimation.h"
// #include "libohos_render/expand/components/forward/KRForwardArkTSView.h"
#include "libohos_render/foundation/KRBorderRadiuses.h"
#include "libohos_render/foundation/KRPoint.h"
#include "libohos_render/foundation/KRRect.h"
#include "libohos_render/foundation/KRSize.h"
#include "libohos_render/utils/KRConvertUtil.h"
#include "libohos_render/utils/KRLinearGradientParser.h"
#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/utils/KRTransformParser.h"
#include "libohos_render/utils/animate/KRAnimateOption.h"
#include "libohos_render/utils/animate/KRAnimationUtils.h"

// forward-declare 以避免直接依赖 rawfile/raw_file_manager.h（该头文件在此仅需类型指针）。
struct NativeResourceManager;

#define KUIKLY_ENABLE_ARKUI_NODE_VALID_CHECK 1

// OH_Drawing_Lattice：native drawing 层的九宫格分割线对象，仅在
// SetArkUIImageCapInsetsWithLattice 的可选输出参数里出现；这里做前向声明
// 避免把 <native_drawing/drawing_lattice.h> 传递给所有引用本头文件的 TU。
struct OH_Drawing_Lattice;

class KRForwardArkTSView;
class KRForwardArkTSViewV2;
namespace kuikly {
namespace util {

class ArkUINativeNodeAPI {
    friend class ::KRForwardArkTSView;
    friend class ::KRForwardArkTSViewV2;

 public:
    ArkUI_NodeHandle createNode(ArkUI_NodeType type);
    void disposeNode(ArkUI_NodeHandle node);
    int32_t addChild(ArkUI_NodeHandle parent, ArkUI_NodeHandle child);
    int32_t insertChildAt(ArkUI_NodeHandle parent, ArkUI_NodeHandle child, int32_t position);
    int32_t removeChild(ArkUI_NodeHandle parent, ArkUI_NodeHandle child);
    int32_t setAttribute(ArkUI_NodeHandle node, ArkUI_NodeAttributeType attribute, const ArkUI_AttributeItem *item);
    int32_t setLengthMetricUnit(ArkUI_NodeHandle node, ArkUI_LengthMetricUnit unit);
    const ArkUI_AttributeItem *getAttribute(ArkUI_NodeHandle node, ArkUI_NodeAttributeType attribute);
    int32_t resetAttribute(ArkUI_NodeHandle node, ArkUI_NodeAttributeType attribute);
    int32_t registerNodeEvent(ArkUI_NodeHandle node, ArkUI_NodeEventType eventType, int32_t targetId, void *userData);
    void unregisterNodeEvent(ArkUI_NodeHandle node, ArkUI_NodeEventType eventType);
    void markDirty(ArkUI_NodeHandle node, ArkUI_NodeDirtyFlag dirtyFlag);
    uint32_t getTotalChildCount(ArkUI_NodeHandle node);
    ArkUI_NodeHandle getParent(ArkUI_NodeHandle node);
    ArkUI_NodeHandle getChildAt(ArkUI_NodeHandle node, int32_t position);
    int32_t registerNodeCustomEvent(ArkUI_NodeHandle node, ArkUI_NodeCustomEventType eventType, int32_t targetId,
                                    void *userData);
    void unregisterNodeCustomEvent(ArkUI_NodeHandle node, ArkUI_NodeCustomEventType eventType);
    int32_t addNodeEventReceiver(ArkUI_NodeHandle node, void (*eventReceiver)(ArkUI_NodeEvent *event));
    int32_t removeNodeEventReceiver(ArkUI_NodeHandle node, void (*eventReceiver)(ArkUI_NodeEvent *event));
    int32_t addNodeCustomEventReceiver(ArkUI_NodeHandle node, void (*eventReceiver)(ArkUI_NodeCustomEvent *event));
    int32_t removeNodeCustomEventReceiver(ArkUI_NodeHandle node, void (*eventReceiver)(ArkUI_NodeCustomEvent *event));
    static ArkUINativeNodeAPI *GetInstance();
#if KUIKLY_ENABLE_ARKUI_NODE_VALID_CHECK
    bool IsNodeAlive(ArkUI_NodeHandle node);
#endif
    
 private:
    ArkUINativeNodeAPI();
    ~ArkUINativeNodeAPI() = default;
    ArkUI_NativeNodeAPI_1 *impl_;

    void unregisterNodeCreatedFromArkTS(ArkUI_NodeHandle node);
    void registerNodeCreatedFromArkTS(ArkUI_NodeHandle node);
#if KUIKLY_ENABLE_ARKUI_NODE_VALID_CHECK
    std::unordered_set<ArkUI_NodeHandle> nodesAlive_;
    std::mutex mutex_;
#endif
};

ArkUINativeNodeAPI *GetNodeApi();

const ArkUI_NativeDialogAPI_1 *GetDialogNodeApi();

void UpdateNodeSize(ArkUI_NodeHandle node, float width, float height);

void UpdateNodeFrame(ArkUI_NodeHandle node, const KRRect &frame);

KRPoint GetNodePositionInWindow(ArkUI_NodeHandle node);

void UpdateNodeBackgroundColor(ArkUI_NodeHandle node, uint32_t hexColorValue);

void UpdateNodeBorderRadius(ArkUI_NodeHandle node, KRBorderRadiuses borderRadius);

void UpdateNodeOpacity(ArkUI_NodeHandle node, double opacity);

void UpdateNodeVisibility(ArkUI_NodeHandle node, int visibility);
// 1裁剪，0不裁剪
void UpdateNodeOverflow(ArkUI_NodeHandle node, int overflow);

// 阴影
void UpdateNodeBoxShadow(ArkUI_NodeHandle node, const std::string &css_box_shadow);

void SetTextShadow(OH_Drawing_TextShadow *shadow, const std::string &css_box_shadow);

void UpdateNodeZIndex(ArkUI_NodeHandle node, int zIndex);

void UpdateNodeHitTest(ArkUI_NodeHandle node, bool touchEnable);

void UpdateNodeHitTestMode(ArkUI_NodeHandle node, ArkUI_HitTestMode mode);

void UpdateNodeAccessibility(ArkUI_NodeHandle node, const std::string &accessibility);

/**
 * 设置无障碍角色。
 *
 * kotlin `AccessibilityRole` 枚举 → HarmonyOS 映射：
 *   button   → NODE_ACCESSIBILITY_ROLE = ARKUI_NODE_BUTTON
 *   text     → NODE_ACCESSIBILITY_ROLE = ARKUI_NODE_TEXT
 *   image    → NODE_ACCESSIBILITY_ROLE = ARKUI_NODE_IMAGE
 *   checkbox → NODE_ACCESSIBILITY_ROLE = ARKUI_NODE_CHECKBOX
 *   search   → NODE_ACCESSIBILITY_ROLE = ARKUI_NODE_TEXT_INPUT（降级，与 Android EditText 语义对齐）
 *   none     → NODE_ACCESSIBILITY_MODE = ARKUI_ACCESSIBILITY_MODE_DISABLED（不设 role，改设 mode）
 *
 * 注意：ArkUI_NodeType 枚举无 SEARCH/NONE 值，故 search 降级到 TEXT_INPUT，none 走 mode 通道。
 * 对 ArkTS 侧创建的 node 调用 setAttribute 会返回 106103（ARKUI_ERROR_CODE_ARKTS_NODE_NOT_SUPPORTED），
 * ArkTS 转发组件不走此路径，由 ets 层 KuiklyRenderBaseView 处理。
 */
void UpdateNodeAccessibilityRole(ArkUI_NodeHandle node, const std::string &roleStr);

/**
 * 设置无障碍可交互动作。
 *
 * kotlin `Attr.accessibilityInfo(clickable, longClickable)` 序列化为 "0/1 0/1" 位串，
 * 解析为 ArkUI_AccessibilityActionType 位或（ACTION_CLICK | ACTION_LONG_CLICK），
 * 写入 NODE_ACCESSIBILITY_ACTIONS。同时按当前 locale 从 core-render-ohos 的
 * string 资源读取"双击激活/双击并长按"提示文案，拼接写入 NODE_ACCESSIBILITY_DESCRIPTION，
 * 与 iOS 的 accessibilityHint 行为对齐。
 *
 * @param res_mgr rootView 提供的 NativeResourceManager；为空时跳过 DESCRIPTION 写入。
 */
void UpdateNodeAccessibilityActions(ArkUI_NodeHandle node, const std::string &infoStr,
                                    NativeResourceManager *res_mgr);

void UpdateNodeBorder(ArkUI_NodeHandle node, std::string borderStr);

void UpdateNodeBackgroundImage(ArkUI_NodeHandle nodeHandle, const std::string &cssBackgroundImage);

/**
 * 更新transform 带有anchor更新
 * @param nodeHandle
 * @param cssTransform
 * @param size // 尺寸，单位为px
 */
void UpdateNodeTransform(ArkUI_NodeHandle nodeHandle, const std::string &cssTransform, KRSize size);

void SetArkUIImageSrc(ArkUI_NodeHandle handle, const std::string &src);

void SetArkUIImageSrc(ArkUI_NodeHandle handle, ArkUI_DrawableDescriptor *drawable);

void ResetArkUIImageSrc(ArkUI_NodeHandle handle);

void SetArkUIIMageResizeMode(ArkUI_NodeHandle handle, const ArkUI_ObjectFit &image_fit);

void ResetArkUIImageResizeMode(ArkUI_NodeHandle handle);

void SetArkUIImageBlurRadius(ArkUI_NodeHandle handle, float radius);

void SetArkUIImageDraggable(ArkUI_NodeHandle handle, bool draggable);

void ResetArkUIImageBlurRadius(ArkUI_NodeHandle handle);

void SetArkUIImageTintColor(ArkUI_NodeHandle handle, const std::tuple<float, float, float, float> &hex_color);

void ResetArkUIImageTintColor(ArkUI_NodeHandle handle);

void SetArkUIImageColorFilter(ArkUI_NodeHandle handle, const std::vector<float> &matrix);

void ResetArkUIImageColorFilter(ArkUI_NodeHandle handle);

void SetArkUIImageCapInsets(ArkUI_NodeHandle handle, float top, float left, float bottom, float right);

/**
 * 设置 NODE_IMAGE_SOURCE_SIZE：图源解码后的宽高，**单位为 px**（对齐 ArkUI 官方
 * 文档语义）。用于把大图解码到与显示区域匹配的尺寸，避免过大位图浪费内存，也让
 * 后续 lattice 精确九宫格拉伸有更贴近视觉的采样源。传入非正值视为不设置。
 */
void SetArkUIImageSourceSize(ArkUI_NodeHandle handle, float width_px, float height_px);

/**
 * API 24+ 走 OH_Drawing_Lattice 精确九宫格；低版本 / 缺尺寸时回退到老的四值
 * NODE_IMAGE_RESIZABLE 路径。
 *
 * @param top/left/bottom/right 四个边距单位为**图片自身像素坐标**（对齐 iOS
 *        UIImage.resizableImageWithCapInsets 的 point 语义——鸿蒙图片没有 point
 *        概念，直接用图片像素最自然）。lattice xDivs/yDivs 原生就吃图片像素，
 *        因此内部不做任何 dpi 换算。fallback 到老四值路径时会自动 /dpi 转成 vp
 *        再下发 NODE_IMAGE_RESIZABLE（老接口的原生单位）。
 * @param image_width_px / image_height_px 原图像素尺寸，用于把边距转换成 lattice
 *        整数分割线并做越界校验。传 <=0 表示图片尺寸未知（如未加载完成），此时
 *        内部自动退回老四值路径以保持行为不变。
 * @param out_lattice 可选输出参数。若非空且本次成功创建了 OH_Drawing_Lattice，
 *        指针会通过 *out_lattice 交出，**销毁责任由调用方承担**（在合适时机调用
 *        OH_Drawing_LatticeDestroy）；否则 *out_lattice 置 nullptr。传 nullptr
 *        表示调用方不接管，函数内部将立即销毁 lattice（保底不泄漏，用于不关心
 *        lattice 生命周期的调用方）。
 *        背景：ArkUI setAttribute 是同步语义、内部会拷贝 lattice 分割线信息，
 *        但实测过早销毁 lattice 会引发崩溃，因此实际生命周期需要延长到组件销毁。
 */
void SetArkUIImageCapInsetsWithLattice(ArkUI_NodeHandle handle, float top, float left, float bottom,
                                       float right, float image_width_px, float image_height_px,
                                       ::OH_Drawing_Lattice **out_lattice = nullptr);

void ResetArkUIImageCapInsets(ArkUI_NodeHandle handle);

void ResetArkUIImageBlendMode(ArkUI_NodeHandle handle);

void SetArkUIScrollDirection(ArkUI_NodeHandle handle, bool direction_row);

void SetArkUIScrollPagingEnabled(ArkUI_NodeHandle handle, bool enable);

void SetArkUIBouncesEnabled(ArkUI_NodeHandle Handle, bool enable);

void SetArkUIShowScrollerIndicator(ArkUI_NodeHandle handle, bool enable);

void SetArkUINestedScroll(ArkUI_NodeHandle handle, ArkUI_ScrollNestedMode forward, ArkUI_ScrollNestedMode backward);

void ResetArkUINestedScroll(ArkUI_NodeHandle handle);

void SetArkUIScrollEnabled(ArkUI_NodeHandle handle, bool enable);

KRPoint GetArkUIScrollContentOffset(ArkUI_NodeHandle handle);

void SetArkUIContentOffset(ArkUI_NodeHandle handle, float offset_x, float offset_y, bool animate, int duration, int curve,
                           float damping = 0);

ArkUI_ScrollState GetArkUIScrollerState(ArkUI_NodeEvent *event, int scroll_state_index);

void SetArkUIStackNodeContentAlignment(ArkUI_NodeHandle handle, const ArkUI_Alignment &alignment);

void SetArkUIMargin(ArkUI_NodeHandle handle, float start, float top, float end, float bottom);

void SetArkUIPadding(ArkUI_NodeHandle handle, float start, float top, float end, float bottom);

void UpdateInputNodeFocusStatus(ArkUI_NodeHandle node, int32_t status);

void UpdateInputNodeFocusable(ArkUI_NodeHandle node, int32_t enable);

void UpdateInputNodeKeyboardType(ArkUI_NodeHandle node, ArkUI_TextInputType input_type);

void UpdateInputNodeEnterKeyType(ArkUI_NodeHandle node, ArkUI_EnterKeyType enter_key_type);

ArkUI_EnterKeyType GetInputNodeEnterKeyType(ArkUI_NodeHandle node);

void UpdateInputNodeMaxLength(ArkUI_NodeHandle node, int32_t max_length);
void ResetInputNodeMaxLength(ArkUI_NodeHandle node);

void UpdateInputNodeContentText(ArkUI_NodeHandle node, const std::string &text);

std::string GetInputNodeContentText(ArkUI_NodeHandle node);

void UpdateInputNodePlaceholder(ArkUI_NodeHandle node, const std::string &placeholder);

void UpdateInputNodePlaceholderColor(ArkUI_NodeHandle node, uint32_t placeholder_color);

// 光标颜色
void UpdateInputNodeCaretrColor(ArkUI_NodeHandle node, uint32_t caret_color);

// ARGB 颜色位操作常量
static constexpr uint32_t kColorAlphaShift = 24;
static constexpr uint32_t kColorAlphaMask = 0xFF;
static constexpr uint32_t kColorRGBMask = 0x00FFFFFF;

// 选中高亮色 alpha 上限（0x66 ≈ 40%），避免高亮完全覆盖文字，多端统一
static constexpr uint32_t kSelectionColorMaxAlpha = 0x66;

/// 限制选中色 alpha 不超过 kSelectionColorMaxAlpha，返回 clamped 后的颜色值
uint32_t ClampSelectionColorAlpha(uint32_t color);

// 选中颜色
void UpdateInputNodeSelectionColor(ArkUI_NodeHandle node, uint32_t color);

// TextArea 选中颜色
void UpdateTextAreaNodeSelectionColor(ArkUI_NodeHandle node, uint32_t color);

// 文本对齐
void UpdateInputNodeTextAlign(ArkUI_NodeHandle node, const std::string &text_align);

void UpdateInputNodePlaceholderFont(ArkUI_NodeHandle node, uint32_t font_size, ArkUI_FontWeight font_weight, bool fontSizeScaleFollowSystem, float font_size_px);

void UpdateInputNodeColor(ArkUI_NodeHandle node, uint32_t color);

uint32_t GetInputNodeSelectionStartPosition(ArkUI_NodeHandle node);

void UpdateInputNodeSelectionStartPosition(ArkUI_NodeHandle node, int32_t index);

std::pair<uint32_t, uint32_t> GetInputNodeSelectionRange(ArkUI_NodeHandle node);

void UpdateTextAreaNodeLineHeight(ArkUI_NodeHandle node, float lineHeight);

void SetNodeAnimation(std::weak_ptr<IKRRenderViewExport> view, std::string *animationStr);
#if 0
{
    auto viewInst = view.lock();
    if (viewInst == nullptr) {
        return;
    }

    // View复用时，清除掉动画
    if (animationStr == nullptr) {
        viewInst->RemoveAllAnimations();
        return;
    }

    // 触发当前设置动画
    if (animationStr->empty()) {
        viewInst->CommitAnimations();
        return;
    }

    // 新增动画
    auto animation = std::make_shared<KRNodeAnimation>(animationStr->c_str(), view);
    animation->onAnimationEndCallback = [view](std::shared_ptr<IKRNodeAnimation> animation,
            bool finished, const std::string &propKey, const std::string &animationKey) {
        auto viewInst = view.lock();
        if (viewInst == nullptr) {
            return;
        }
        viewInst->OnAnimationCompletion(animation, finished, propKey, animationKey);
        viewInst->RemoveAnimation(animation);
    };

    viewInst->AddAnimation(animation);
}
#endif
void UpdateLoadingProgressNodeColor(ArkUI_NodeHandle node, uint32_t hexColorValue);

void UpdateNodeClipPath(ArkUI_NodeHandle node, float width, float height, const std::string &pathCommand);

}  // namespace util
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_KRVIEWUTIL_H
