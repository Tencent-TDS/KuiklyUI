/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

#define KUIKLY_ENABLE_ARKUI_NODE_VALID_CHECK 1

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
    const ArkUI_AttributeItem *getAttribute(ArkUI_NodeHandle node, ArkUI_NodeAttributeType attribute);
    int32_t resetAttribute(ArkUI_NodeHandle node, ArkUI_NodeAttributeType attribute);
    int32_t registerNodeEvent(ArkUI_NodeHandle node, ArkUI_NodeEventType eventType, int32_t targetId, void *userData);
    void unregisterNodeEvent(ArkUI_NodeHandle node, ArkUI_NodeEventType eventType);
    void markDirty(ArkUI_NodeHandle node, ArkUI_NodeDirtyFlag dirtyFlag);
    uint32_t getTotalChildCount(ArkUI_NodeHandle node);
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

void SetArkUIImageCapInsets(ArkUI_NodeHandle handle, float top, float left, float bottom, float right);

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

void SetArkUIContentOffset(ArkUI_NodeHandle handle, float offset_x, float offset_y, bool animate, int duration);

ArkUI_ScrollState GetArkUIScrollerState(ArkUI_NodeEvent *event, int scroll_state_index);

void SetArkUIStackNodeContentAlignment(ArkUI_NodeHandle handle, const ArkUI_Alignment &alignment);

void SetArkUIMargin(ArkUI_NodeHandle handle, float start, float top, float end, float bottom);

void SetArkUIPadding(ArkUI_NodeHandle handle, float start, float top, float end, float bottom);

void UpdateInputNodeFocusStatus(ArkUI_NodeHandle node, int32_t status);

void UpdateInputNodeFocusable(ArkUI_NodeHandle node, int32_t enable);

void UpdateInputNodeKeyboardType(ArkUI_NodeHandle node, ArkUI_TextInputType input_type);

void UpdateInputNodeEnterKeyType(ArkUI_NodeHandle node, ArkUI_EnterKeyType enter_key_type);

void UpdateInputNodeMaxLength(ArkUI_NodeHandle node, int32_t max_length);

void UpdateInputNodeContentText(ArkUI_NodeHandle node, const std::string &text);

std::string GetInputNodeContentText(ArkUI_NodeHandle node);

void UpdateInputNodePlaceholder(ArkUI_NodeHandle node, const std::string &placeholder);

void UpdateInputNodePlaceholderColor(ArkUI_NodeHandle node, uint32_t placeholder_color);

// 光标颜色
void UpdateInputNodeCaretrColor(ArkUI_NodeHandle node, uint32_t caret_color);

// 文本对齐
void UpdateInputNodeTextAlign(ArkUI_NodeHandle node, const std::string &text_align);

void UpdateInputNodePlaceholderFont(ArkUI_NodeHandle node, uint32_t font_size, ArkUI_FontWeight font_weight);

void UpdateInputNodeColor(ArkUI_NodeHandle node, uint32_t color);

uint32_t GetInputNodeSelectionStartPosition(ArkUI_NodeHandle node);

void UpdateInputNodeSelectionStartPosition(ArkUI_NodeHandle node, int32_t index);

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

}  // namespace util
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_KRVIEWUTIL_H
