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

#include "libohos_render/utils/KRViewUtil.h"

#include <arkui/drawable_descriptor.h>
#include <deviceinfo.h>
#include <multimedia/image_framework/image/pixelmap_native.h>

#include "libohos_render/export/IKRRenderViewExport.h"
#include "libohos_render/utils/KRThreadChecker.h"

// ============================================================================
// OH_Drawing_Lattice 精确九宫格（API 24 引入）—— 弱符号声明块
//
// **必须放在全局命名空间**：C++ 里在 namespace 内出现的 `struct X;` 或
// `struct X **p` 会**在当前 namespace 里新建同名前向声明**（elaborated type
// specifier 规则），导致 KRViewUtil.h（在全局命名空间前向声明 ::OH_Drawing_Lattice）
// 与 KRViewUtil.cpp 里如果放到 kuikly::util 内声明就会产生
// kuikly::util::OH_Drawing_Lattice 这个**独立类型符号**，跨 TU 的函数签名类型
// 因此不一致，最终链接期 undefined symbol（这一坑已在真实构建中踩过）。
// 故这里统一在**全局作用域**做前向声明与 weak 函数声明，保证与
// KRViewUtil.h 里 ::OH_Drawing_Lattice 完全一致。
//
// 其他说明：
//   * 弱符号 OH_Drawing_LatticeCreate / _Destroy：运行系统 API < 24 时符号地址
//     为 nullptr，dlopen .so 不会因未解析符号而失败；运行时以
//     OH_GetSdkApiVersion() >= 24 && 符号非空 双重判定。
//   * 不 #include <native_drawing/drawing_lattice.h>：低版本 SDK header 里没有
//     该文件，直接 include 会破坏低版本编译；这里前向自声明所需的最小类型/函数。
//   * 先在 extern "C" 之外用 C++ 语法声明 opaque enum OH_Drawing_LatticeRectType：
//     指定底层类型 int32_t，与 SDK <native_drawing/drawing_lattice.h> 中
//     typedef enum { DEFAULT, TRANSPARENT, FIXED_COLOR } 的 ABI 严格一致，避免把
//     DEFAULT / TRANSPARENT / FIXED_COLOR 引入全局命名空间造成宏冲突
//     （如 Windows GDI 的 TRANSPARENT 宏）。
// ============================================================================
enum OH_Drawing_LatticeRectType : int32_t;

extern "C" {
// OH_Drawing_Lattice 是不透明结构体，前向声明即可满足指针使用。
struct OH_Drawing_Lattice;
// OH_Drawing_Rect 同为不透明结构体（drawing_types.h），此处只需指针语义。
struct OH_Drawing_Rect;

// OH_Drawing_ErrorCode 也是枚举（drawing_error_code.h 里定义），此处按 SDK 一致的
// 32 位有符号整型返回值来声明——弱符号只关心链接期名字/参数签名匹配，返回类型只要
// ABI 一致即可。
int32_t OH_Drawing_LatticeCreate(const int *xDivs, const int *yDivs, uint32_t xCount, uint32_t yCount,
                                 const struct OH_Drawing_Rect *bounds,
                                 const enum OH_Drawing_LatticeRectType *rectTypes, uint32_t rectTypeCount,
                                 const uint32_t *colors, uint32_t colorCount,
                                 struct OH_Drawing_Lattice **lattice) __attribute__((weak));
int32_t OH_Drawing_LatticeDestroy(struct OH_Drawing_Lattice *lattice) __attribute__((weak));

// OH_Drawing_RectCreate / Destroy 自 API 11 就已存在（drawing_rect.h @since 11），
// 早于我们要求的 API 24，链接一定成功；不加弱符号也可以，加上更保险且和 lattice
// 保持声明风格一致。之所以不 #include <native_drawing/drawing_rect.h>，是为了避免
// 编译期依赖 SDK header 结构，与 lattice 保持"最小前向声明"策略一致。
struct OH_Drawing_Rect *OH_Drawing_RectCreate(float left, float top, float right, float bottom)
    __attribute__((weak));
void OH_Drawing_RectDestroy(struct OH_Drawing_Rect *rect) __attribute__((weak));
}  // extern "C"

namespace kuikly {
namespace util {

void SetNodeAnimation(std::weak_ptr<IKRRenderViewExport> view, std::string *animationStr) {
    KREnsureMainThread();

    auto strongView = view.lock();
    if (strongView == nullptr) {
        return;
    }
    auto viewInst = strongView->GetBasePropsHandler();
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
    animation->onAnimationEndCallback = [view](std::shared_ptr<IKRNodeAnimation> animation, bool finished,
                                               const std::string &propKey, const std::string &animationKey) {
        auto strongView = view.lock();
        if (strongView == nullptr) {
            return;
        }
        auto viewInst = strongView->GetBasePropsHandler();
        if (viewInst == nullptr) {
            return;
        }
        viewInst->OnAnimationCompletion(animation, finished, propKey, animationKey);
        viewInst->RemoveAnimation(animation);
    };

    viewInst->AddAnimation(animation);
}

ArkUINativeNodeAPI::ArkUINativeNodeAPI() {
    OH_ArkUI_GetModuleInterface(ARKUI_NATIVE_NODE, ArkUI_NativeNodeAPI_1, impl_);
}

ArkUINativeNodeAPI *ArkUINativeNodeAPI::GetInstance() {
    static ArkUINativeNodeAPI *instance_ = nullptr;
    static std::once_flag flag;
    std::call_once(flag, []() { instance_ = new ArkUINativeNodeAPI(); });
    return instance_;
}

void ArkUINativeNodeAPI::unregisterNodeCreatedFromArkTS(ArkUI_NodeHandle node) {
    KREnsureMainThread();
#if KUIKLY_ENABLE_ARKUI_NODE_VALID_CHECK
    {
        std::lock_guard<std::mutex> guard(mutex_);
        nodesAlive_.erase(node);
    }
#endif
}

void ArkUINativeNodeAPI::registerNodeCreatedFromArkTS(ArkUI_NodeHandle node) {
    KREnsureMainThread();
#if KUIKLY_ENABLE_ARKUI_NODE_VALID_CHECK
    {
        std::lock_guard<std::mutex> guard(mutex_);
        nodesAlive_.emplace(node);
    }
#endif
}
#if KUIKLY_ENABLE_ARKUI_NODE_VALID_CHECK
bool ArkUINativeNodeAPI::IsNodeAlive(ArkUI_NodeHandle node) {
    KREnsureMainThread();

    std::lock_guard<std::mutex> guard(mutex_);
    return nodesAlive_.find(node) != nodesAlive_.end();
}

#define KUIKLY_CHECK_NODE_OR_RETURN(NODE)                                                                              \
    do {                                                                                                               \
        if (!IsNodeAlive(NODE)) {                                                                                      \
            KR_LOG_ERROR << "Node DEAD";                                                                               \
            assert(false);                                                                                             \
            return;                                                                                                    \
        }                                                                                                              \
    \
} while (0)

#define KUIKLY_CHECK_NODE_OR_RETURN_ERROR(NODE)                                                                        \
    do {                                                                                                               \
        if (!IsNodeAlive(NODE)) {                                                                                      \
            KR_LOG_ERROR << "Node DEAD";                                                                               \
            assert(false);                                                                                             \
            return ARKUI_ERROR_CODE_PARAM_INVALID;                                                                     \
        }                                                                                                              \
    \
} while (0)

#define KUIKLY_CHECK_NODE_OR_RETURN_NULL(NODE)                                                                         \
    do {                                                                                                               \
        if (!IsNodeAlive(NODE)) {                                                                                      \
            KR_LOG_ERROR << "Node DEAD";                                                                               \
            assert(false);                                                                                             \
            return nullptr;                                                                                            \
        }                                                                                                              \
    \
} while (0)

#define KUIKLY_CHECK_NODE_OR_RETURN_ZERO(NODE)                                                                         \
    do {                                                                                                               \
        if (!IsNodeAlive(NODE)) {                                                                                      \
            KR_LOG_ERROR << "Node DEAD";                                                                               \
            assert(false);                                                                                             \
            return 0;                                                                                                  \
        }                                                                                                              \
    \
} while (0)

#else

#define KUIKLY_CHECK_NODE_OR_RETURN(NODE)
#define KUIKLY_CHECK_NODE_OR_RETURN_ERROR(NODE)
#define KUIKLY_CHECK_NODE_OR_RETURN_NULL(NODE)
#define KUIKLY_CHECK_NODE_OR_RETURN_ZERO(NODE)
#endif

ArkUI_NodeHandle ArkUINativeNodeAPI::createNode(ArkUI_NodeType type) {
    KREnsureMainThread();
    ArkUI_NodeHandle node = impl_->createNode(type);
#if KUIKLY_ENABLE_ARKUI_NODE_VALID_CHECK
    {
        std::lock_guard<std::mutex> guard(mutex_);
        nodesAlive_.emplace(node);
    }
#endif

    return node;
}

void ArkUINativeNodeAPI::disposeNode(ArkUI_NodeHandle node) {
    KREnsureMainThread();
#if KUIKLY_ENABLE_ARKUI_NODE_VALID_CHECK
    {
        std::lock_guard<std::mutex> guard(mutex_);
        if (nodesAlive_.find(node) == nodesAlive_.end()) {
            return;
        }
        nodesAlive_.erase(node);
    }
#endif
    impl_->disposeNode(node);
}
int32_t ArkUINativeNodeAPI::addChild(ArkUI_NodeHandle parent, ArkUI_NodeHandle child) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN_ERROR(parent);
    KUIKLY_CHECK_NODE_OR_RETURN_ERROR(child);

    return impl_->addChild(parent, child);
}
int32_t ArkUINativeNodeAPI::insertChildAt(ArkUI_NodeHandle parent, ArkUI_NodeHandle child, int32_t position) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN_ERROR(parent);
    KUIKLY_CHECK_NODE_OR_RETURN_ERROR(child);

    return impl_->insertChildAt(parent, child, position);
}
int32_t ArkUINativeNodeAPI::removeChild(ArkUI_NodeHandle parent, ArkUI_NodeHandle child) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN_ERROR(parent);
    KUIKLY_CHECK_NODE_OR_RETURN_ERROR(child);

    return impl_->removeChild(parent, child);
}
int32_t ArkUINativeNodeAPI::setAttribute(ArkUI_NodeHandle node, ArkUI_NodeAttributeType attribute,
                                         const ArkUI_AttributeItem *item) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN_ERROR(node);
    return impl_->setAttribute(node, attribute, item);
}

int32_t ArkUINativeNodeAPI::setLengthMetricUnit(ArkUI_NodeHandle node, ArkUI_LengthMetricUnit unit) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN_ERROR(node);
    return impl_->setLengthMetricUnit(node, unit);
}

const ArkUI_AttributeItem *ArkUINativeNodeAPI::getAttribute(ArkUI_NodeHandle node, ArkUI_NodeAttributeType attribute) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN_NULL(node);
    return impl_->getAttribute(node, attribute);
}

int32_t ArkUINativeNodeAPI::resetAttribute(ArkUI_NodeHandle node, ArkUI_NodeAttributeType attribute) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN_ERROR(node);
    return impl_->resetAttribute(node, attribute);
}
int32_t ArkUINativeNodeAPI::registerNodeEvent(ArkUI_NodeHandle node, ArkUI_NodeEventType eventType, int32_t targetId,
                                              void *userData) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN_ERROR(node);
    return impl_->registerNodeEvent(node, eventType, targetId, userData);
}
void ArkUINativeNodeAPI::unregisterNodeEvent(ArkUI_NodeHandle node, ArkUI_NodeEventType eventType) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN(node);
    return impl_->unregisterNodeEvent(node, eventType);
}

void ArkUINativeNodeAPI::markDirty(ArkUI_NodeHandle node, ArkUI_NodeDirtyFlag dirtyFlag) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN(node);
    impl_->markDirty(node, dirtyFlag);
}
uint32_t ArkUINativeNodeAPI::getTotalChildCount(ArkUI_NodeHandle node) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN_ZERO(node);
    return impl_->getTotalChildCount(node);
}

ArkUI_NodeHandle ArkUINativeNodeAPI::getParent(ArkUI_NodeHandle node) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN_NULL(node);
    return impl_->getParent(node);
}

ArkUI_NodeHandle ArkUINativeNodeAPI::getChildAt(ArkUI_NodeHandle node, int32_t position) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN_NULL(node);
    return impl_->getChildAt(node, position);
}

int32_t ArkUINativeNodeAPI::registerNodeCustomEvent(ArkUI_NodeHandle node, ArkUI_NodeCustomEventType eventType,
                                                    int32_t targetId, void *userData) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN_ERROR(node);
    return impl_->registerNodeCustomEvent(node, eventType, targetId, userData);
}
void ArkUINativeNodeAPI::unregisterNodeCustomEvent(ArkUI_NodeHandle node, ArkUI_NodeCustomEventType eventType) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN(node);
    return impl_->unregisterNodeCustomEvent(node, eventType);
}
int32_t ArkUINativeNodeAPI::addNodeEventReceiver(ArkUI_NodeHandle node, void (*eventReceiver)(ArkUI_NodeEvent *event)) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN_ERROR(node);
    return impl_->addNodeEventReceiver(node, eventReceiver);
}
int32_t ArkUINativeNodeAPI::removeNodeEventReceiver(ArkUI_NodeHandle node,
                                                    void (*eventReceiver)(ArkUI_NodeEvent *event)) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN_ERROR(node);
    return impl_->removeNodeEventReceiver(node, eventReceiver);
}
int32_t ArkUINativeNodeAPI::addNodeCustomEventReceiver(ArkUI_NodeHandle node,
                                                       void (*eventReceiver)(ArkUI_NodeCustomEvent *event)) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN_ERROR(node);
    return impl_->addNodeCustomEventReceiver(node, eventReceiver);
}
int32_t ArkUINativeNodeAPI::removeNodeCustomEventReceiver(ArkUI_NodeHandle node,
                                                          void (*eventReceiver)(ArkUI_NodeCustomEvent *event)) {
    KREnsureMainThread();
    KUIKLY_CHECK_NODE_OR_RETURN_ERROR(node);
    return impl_->removeNodeCustomEventReceiver(node, eventReceiver);
}

ArkUINativeNodeAPI *GetNodeApi() {
    return ArkUINativeNodeAPI::GetInstance();
}

const ArkUI_NativeDialogAPI_1 *GetDialogNodeApi() {
    static ArkUI_NativeDialogAPI_1 *gDialogNodeApi = nullptr;
    if (!gDialogNodeApi) {
        OH_ArkUI_GetModuleInterface(ARKUI_NATIVE_DIALOG, ArkUI_NativeDialogAPI_1, gDialogNodeApi);
    }
    return gDialogNodeApi;
}

void UpdateNodeSize(ArkUI_NodeHandle node, float width, float height) {
    auto nodeAPI = GetNodeApi();
    ArkUI_NumberValue wValue[] = {width};
    ArkUI_NumberValue hValue[] = {height};
    ArkUI_AttributeItem widthItem = {wValue, sizeof(wValue) / sizeof(ArkUI_NumberValue)};
    ArkUI_AttributeItem heightItem = {hValue, sizeof(hValue) / sizeof(ArkUI_NumberValue)};
    nodeAPI->setAttribute(node, NODE_WIDTH, &widthItem);
    nodeAPI->setAttribute(node, NODE_HEIGHT, &heightItem);
}

void UpdateNodeFrame(ArkUI_NodeHandle node, const KRRect &frame) {
    auto nodeAPI = GetNodeApi();
    ArkUI_NumberValue position_value[] = {frame.x, frame.y};
    ArkUI_AttributeItem position_item = {position_value, 2};
    ArkUI_NumberValue wValue[] = {frame.width};
    ArkUI_NumberValue hValue[] = {frame.height};
    ArkUI_AttributeItem widthItem = {wValue, 1};
    ArkUI_AttributeItem heightItem = {hValue, 1};
    nodeAPI->setAttribute(node, NODE_WIDTH, &widthItem);
    nodeAPI->setAttribute(node, NODE_HEIGHT, &heightItem);
    nodeAPI->setAttribute(node, NODE_POSITION, &position_item);
}

KRPoint GetNodePositionInWindow(ArkUI_NodeHandle node) {
    if (!node) {
        return {};
    }

    ArkUI_IntOffset globalOffset;
    int32_t ret = OH_ArkUI_NodeUtils_GetLayoutPositionInWindow(node, &globalOffset);
    if (ret != ARKUI_ERROR_CODE_NO_ERROR) {
        KR_LOG_ERROR << "Failed to get node position in window, error code: " << ret;
        return {};
    }

    float px_x = static_cast<float>(globalOffset.x);
    float px_y = static_cast<float>(globalOffset.y);

    double dpi = KRConfig::GetDpi();

    return KRPoint{static_cast<float>(px_x / dpi), static_cast<float>(px_y / dpi)};
}

void UpdateNodeBackgroundColor(ArkUI_NodeHandle node, uint32_t hexColorValue) {
    auto nodeAPI = GetNodeApi();
    ArkUI_NumberValue value[] = {{.u32 = hexColorValue}};
    ArkUI_AttributeItem bgColorItem = {value, 1};
    nodeAPI->setAttribute(node, NODE_BACKGROUND_COLOR, &bgColorItem);

    // nodeAPI->setAttribute(node, NODE_BORDER_RADIUS)
}

void UpdateNodeBorderRadius(ArkUI_NodeHandle node, KRBorderRadiuses borderRadius) {
    auto nodeAPI = GetNodeApi();
    ArkUI_NumberValue value[] = {{.f32 = borderRadius.topLeft},
                                 {.f32 = borderRadius.topRight},
                                 {.f32 = borderRadius.bottomLeft},
                                 {.f32 = borderRadius.bottomRight}};
    ArkUI_AttributeItem borderRadiusItem = {value, 4};
    nodeAPI->setAttribute(node, NODE_BORDER_RADIUS, &borderRadiusItem);
}

void UpdateNodeOpacity(ArkUI_NodeHandle node, double opacity) {
    auto nodeAPI = GetNodeApi();
    ArkUI_NumberValue value[] = {static_cast<float>(opacity)};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    nodeAPI->setAttribute(node, NODE_OPACITY, &item);
}

void UpdateNodeVisibility(ArkUI_NodeHandle node, int visibility) {
    auto nodeAPI = GetNodeApi();
    int show = (visibility != 0) ? 0 : 2;
    ArkUI_NumberValue value[] = {{.i32 = show}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    nodeAPI->setAttribute(node, NODE_VISIBILITY, &item);
}

void UpdateNodeOverflow(ArkUI_NodeHandle node, int overflow) {
    auto nodeAPI = GetNodeApi();
    ArkUI_NumberValue value[] = {{.i32 = overflow}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    nodeAPI->setAttribute(node, NODE_CLIP, &item);
}

void UpdateNodeBoxShadow(ArkUI_NodeHandle node, const std::string &css_box_shadow) {
    auto nodeAPI = GetNodeApi();
    auto splits = ConvertSplit(css_box_shadow, " ");
    auto dpi = KRConfig::GetDpi();
    float x = ConvertToFloat(splits[0]) * dpi;
    float y = ConvertToFloat(splits[1]) * dpi;
    float radius = ConvertToFloat(splits[2]) * dpi;
    uint32_t color = ConvertToHexColor(splits[3]);
    int fill = splits.size() > 4 && splits[4] == "0" ? 0 : 1;
    ArkUI_NumberValue value[] = {
        {.f32 = radius},
        {.i32 = 0},
        {.f32 = x},
        {.f32 = y},
        {.i32 = ARKUI_SHADOW_TYPE_COLOR},
        {.u32 = color},
        {.i32 = fill}
    };
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    nodeAPI->setAttribute(node, NODE_CUSTOM_SHADOW, &item);
}

void SetTextShadow(OH_Drawing_TextShadow *shadow, const std::string &css_box_shadow) {
    auto splits = ConvertSplit(css_box_shadow, " ");
    float x = ConvertToFloat(splits[0]);
    float y = ConvertToFloat(splits[1]);
    float radius = ConvertToFloat(splits[2]);
    uint32_t color = ConvertToHexColor(splits[3]);
    auto offset = OH_Drawing_PointCreate(x, y);
    OH_Drawing_SetTextShadow(shadow, color, offset, radius);
    OH_Drawing_PointDestroy(offset);
}

void UpdateNodeZIndex(ArkUI_NodeHandle node, int zIndex) {
    auto nodeAPI = GetNodeApi();
    ArkUI_NumberValue value[] = {{.i32 = zIndex}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    nodeAPI->setAttribute(node, NODE_Z_INDEX, &item);
}

void UpdateNodeHitTest(ArkUI_NodeHandle node, bool touchEnable) {
    auto nodeAPI = GetNodeApi();
    int mode = touchEnable ? 1 : 0;
    ArkUI_NumberValue value[] = {{.i32 = mode}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    nodeAPI->setAttribute(node, NODE_ENABLED, &item);
}

void UpdateNodeHitTestMode(ArkUI_NodeHandle node, ArkUI_HitTestMode mode) {
    auto nodeAPI = GetNodeApi();
    // int mode = touchEnable ? ARKUI_HIT_TEST_MODE_DEFAULT : ARKUI_HIT_TEST_MODE_TRANSPARENT;
    ArkUI_NumberValue value[] = {{.i32 = mode}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    nodeAPI->setAttribute(node, NODE_HIT_TEST_BEHAVIOR, &item);
}

static uint32_t GetNodeHitTestMode(ArkUI_NodeHandle node) {
    auto item = GetNodeApi()->getAttribute(node, NODE_HIT_TEST_BEHAVIOR);
    return item ? item->value[0].i32 : 0;
}

void UpdateNodeAccessibility(ArkUI_NodeHandle node, const std::string &accessibility) {
    auto nodeAPI = GetNodeApi();
    ArkUI_AttributeItem textItem = {.string = accessibility.c_str()};
    nodeAPI->setAttribute(node, NODE_ACCESSIBILITY_TEXT, &textItem);
}

void UpdateNodeBorder(ArkUI_NodeHandle node, std::string borderStr) {
    auto nodeAPI = GetNodeApi();
    auto splits = ConvertSplit(borderStr, " ");
    auto boderWidth = ConvertToFloat(splits[0]);
    ArkUI_NumberValue value[] = {{.f32 = boderWidth}, {.f32 = boderWidth}, {.f32 = boderWidth}, {.f32 = boderWidth}};
    ArkUI_AttributeItem borderWidthItem = {value, 4};
    nodeAPI->setAttribute(node, NODE_BORDER_WIDTH, &borderWidthItem);
    {
        auto hexColor = ConvertToHexColor(splits[2]);
        ArkUI_NumberValue value[] = {{.u32 = hexColor}, {.u32 = hexColor}, {.u32 = hexColor}, {.u32 = hexColor}};
        ArkUI_AttributeItem borderColorItem = {value, 4};
        nodeAPI->setAttribute(node, NODE_BORDER_COLOR, &borderColorItem);
    }
    {
        auto style = ConverToBorderStyle(splits[1]);  // style

        ArkUI_NumberValue value[] = {{.u32 = style}, {.u32 = style}, {.u32 = style}, {.u32 = style}};
        ArkUI_AttributeItem borderStyleItem = {value, 4};
        nodeAPI->setAttribute(node, NODE_BORDER_STYLE, &borderStyleItem);
    }
}

void UpdateNodeBackgroundImage(ArkUI_NodeHandle nodeHandle, const std::string &cssBackgroundImage) {
    auto nodeAPI = GetNodeApi();
    auto linearGradient = std::make_shared<KRLinearGradientParser>();
    if (linearGradient->ParseFromCssLinearGradient(cssBackgroundImage)) {
        // 获取 colors 和 locations
        const std::vector<uint32_t> &colors = linearGradient->GetColors();
        const std::vector<float> &locations = linearGradient->GetLocations();

        // 创建 C 风格数组
        unsigned int colorsArray[colors.size()];
        float stopsArray[locations.size()];

        // 填充数组
        for (size_t i = 0; i < colors.size(); ++i) {
            colorsArray[i] = colors[i];
        }
        for (size_t i = 0; i < locations.size(); ++i) {
            if (i == locations.size() - 1) {
                stopsArray[i] = 1.0;
            } else {
                stopsArray[i] = locations[i];
            }
        }
        // 创建 ArkUI_ColorStop 结构
        ArkUI_ColorStop colorStop = {colorsArray, stopsArray, static_cast<int>(colors.size())};
        ArkUI_ColorStop *ptr = &colorStop;
        ArkUI_NumberValue value[] = {{}, {.i32 = linearGradient->GetArkUIDirection()}, {.i32 = false}};
        ArkUI_AttributeItem item = {
            .value = value, .size = sizeof(value) / sizeof(ArkUI_NumberValue), .object = reinterpret_cast<void *>(ptr)};
        nodeAPI->setAttribute(nodeHandle, NODE_LINEAR_GRADIENT, &item);
    }
}

// 旋转转换结果结构体
struct RotationResult {
    float axis_x;
    float axis_y;
    float axis_z;
    float total_angle_deg;
};

/*
 * 将欧拉角转换为轴角表示
 * @param x_deg X轴旋转角度（度）
 * @param y_deg Y轴旋转角度（度）
 * @param z_deg Z轴旋转角度（度）
 * @return RotationResult 包含旋转轴和总旋转角度的结构体
 */
static RotationResult ConvertEulerToAxisAngle(double x_deg, double y_deg, double z_deg) {
    constexpr double EPSILON = 1e-10;
    constexpr double DEG_TO_RAD = M_PI / 180.0;
    
    // 处理纯Z轴旋转的特殊情况，直接返回Z轴和角度
    if (std::abs(x_deg) < EPSILON && std::abs(y_deg) < EPSILON) {
        return {0.0f, 0.0f, 1.0f, static_cast<float>(z_deg)};
    }
    // 只绕X轴的特殊情况
    if (std::abs(y_deg) < EPSILON && std::abs(z_deg) < EPSILON) {
        return {1.0f, 0.0f, 0.0f, static_cast<float>(x_deg)};
    }
    // 只绕Y轴的特殊情况
    if (std::abs(x_deg) < EPSILON && std::abs(z_deg) < EPSILON) {
        return {0.0f, 1.0f, 0.0f, static_cast<float>(y_deg)};
    }
    
    // 角度转弧度
    double rx_rad = x_deg * DEG_TO_RAD;
    double ry_rad = y_deg * DEG_TO_RAD;
    double rz_rad = z_deg * DEG_TO_RAD;
    // 计算半角三角函数
    double cx = cos(rx_rad / 2), sx = sin(rx_rad / 2);
    double cy = cos(ry_rad / 2), sy = sin(ry_rad / 2);
    double cz = cos(rz_rad / 2), sz = sin(rz_rad / 2);
    // 计算四元数分量 (X → Y → Z 顺序)
    double w = cz * cy * cx + sz * sy * sx;
    double x = cz * cy * sx - sz * sy * cx;
    double y = cz * sy * cx + sz * cy * sx;
    double z = sz * cy * cx - cz * sy * sx;
    // 归一化处理
    double norm = std::sqrt(w*w + x*x + y*y + z*z);
    if (norm < EPSILON) {
        return {0.0f, 0.0f, 1.0f, 0.0f}; // 零旋转默认值
    }
    w /= norm; x /= norm; y /= norm; z /= norm;
    // 提取旋转角度（弧度→角度）
    double total_angle_rad = 2 * std::acos(std::clamp(w, -1.0, 1.0));
    double total_angle_deg = total_angle_rad * 180.0 / M_PI;
    // 零旋转特殊处理
    if (total_angle_rad < EPSILON) {
        return {0.0f, 0.0f, 1.0f, 0.0f};
    }
    // 计算旋转轴
    double inv_sin = 1.0 / std::sin(total_angle_rad / 2);
    return {
        static_cast<float>(x * inv_sin),
        static_cast<float>(y * inv_sin),
        static_cast<float>(z * inv_sin),
        static_cast<float>(total_angle_deg)
    };
}

/**
 * 更新transform 带有anchor更新
 * @param nodeHandle 节点句柄
 * @param cssTransform CSS变换字符串
 * @param size 元素尺寸（单位px）
 */
void UpdateNodeTransform(ArkUI_NodeHandle nodeHandle, 
						 const std::string &cssTransform, 
                         KRSize size) {
    auto nodeAPI = GetNodeApi();
    auto transform = std::make_shared<KRTransformParser>();
    
    if (!transform->ParseFromCssTransform(cssTransform)) {
        return;
    }
    // 设置变换中心点
    ArkUI_NumberValue transformCenterValue[] = {
        0, 0, 0, 
        static_cast<float>(transform->anchor_x_),
        static_cast<float>(transform->anchor_y_)
    };
    ArkUI_AttributeItem transformCenterItem = {
        transformCenterValue, 
        sizeof(transformCenterValue) / sizeof(ArkUI_NumberValue)
    };
    nodeAPI->setAttribute(nodeHandle, NODE_TRANSFORM_CENTER, &transformCenterItem);
    // 处理平移变换（转换为px单位）
    auto matrix = transform->GetMatrixWithNoRotate();
    matrix[12] *= size.width;   // X轴平移
    matrix[13] *= size.height;  // Y轴平移
    // 设置变换矩阵
    std::array<ArkUI_NumberValue, 16> transformValue;
    for (int i = 0; i < 16; i++) {
        transformValue[i] = {.f32 = static_cast<float>(matrix[i])};
    }
    ArkUI_AttributeItem transformItem = {transformValue.data(), transformValue.size()};
    nodeAPI->setAttribute(nodeHandle, NODE_TRANSFORM, &transformItem);
    
    // 处理旋转变换（欧拉角→轴角）
    RotationResult rotation = ConvertEulerToAxisAngle(
        transform->rotate_x_angle_,
        transform->rotate_y_angle_,
        transform->rotate_angle_
    );
    // 设置旋转属性
    ArkUI_NumberValue rotateValue[] = {
        rotation.axis_x,
        rotation.axis_y,
        rotation.axis_z,
        rotation.total_angle_deg,
        0.0f  // perspective默认值
    };
    ArkUI_AttributeItem rotateItem = {
        rotateValue, 
        sizeof(rotateValue) / sizeof(ArkUI_NumberValue)
    };
    nodeAPI->setAttribute(nodeHandle, NODE_ROTATE, &rotateItem);
}

void SetArkUIImageSrc(ArkUI_NodeHandle handle, const std::string &src) {
    if (!handle) {
        return;
    }

    auto nodeApi = GetNodeApi();
    ArkUI_AttributeItem src_attr_item = {.string = src.c_str()};
    nodeApi->setAttribute(handle, NODE_IMAGE_SRC, &src_attr_item);
}

void SetArkUIImageSrc(ArkUI_NodeHandle handle, ArkUI_DrawableDescriptor *drawable) {
    if (!handle) {
        return;
    }
    
    auto nodeApi = GetNodeApi();
    ArkUI_AttributeItem src_attr_item = {.object = drawable};
    nodeApi->setAttribute(handle, NODE_IMAGE_SRC, &src_attr_item);
}

void ResetArkUIImageSrc(ArkUI_NodeHandle handle) {
    if (!handle) {
        return;
    }

    GetNodeApi()->resetAttribute(handle, NODE_IMAGE_SRC);
}

void SetArkUIIMageResizeMode(ArkUI_NodeHandle handle, const ArkUI_ObjectFit &image_fit) {
    if (!handle) {
        return;
    }

    ArkUI_NumberValue value[] = {{.i32 = image_fit}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(handle, NODE_IMAGE_OBJECT_FIT, &item);
}

void ResetArkUIImageResizeMode(ArkUI_NodeHandle handle) {
    if (!handle) {
        return;
    }

    GetNodeApi()->resetAttribute(handle, NODE_IMAGE_OBJECT_FIT);
}

void SetArkUIImageBlurRadius(ArkUI_NodeHandle handle, float radius) {
    if (!handle) {
        return;
    }

    ArkUI_NumberValue value[] = {{.f32 = radius}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(handle, NODE_BLUR, &item);
}

void SetArkUIImageDraggable(ArkUI_NodeHandle handle, bool draggable) {
    ArkUI_NumberValue value[] = {{.i32 = static_cast<int32_t>(draggable)}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue), nullptr, nullptr};
    GetNodeApi()->setAttribute(handle, NODE_IMAGE_DRAGGABLE, &item);
}

void ResetArkUIImageBlurRadius(ArkUI_NodeHandle handle) {
    if (!handle) {
        return;
    }

    GetNodeApi()->resetAttribute(handle, NODE_BLUR);
}

void SetArkUIImageTintColor(ArkUI_NodeHandle handle, const std::tuple<float, float, float, float> &hex_color) {
    if (!handle) {
        return;
    }

    float a = std::get<0>(hex_color); // alpha
    float r = std::get<1>(hex_color); // red
    float g = std::get<2>(hex_color); // green
    float b = std::get<3>(hex_color); // blue

    ArkUI_NumberValue value[] = {
        {.f32 = 1 - a}, {.f32 = 0}, {.f32 = 0}, {.f32 = 0}, {.f32 = r * a},
        {.f32 = 0}, {.f32 = 1 - a}, {.f32 = 0}, {.f32 = 0}, {.f32 = g * a},
        {.f32 = 0}, {.f32 = 0}, {.f32 = 1 - a}, {.f32 = 0}, {.f32 = b * a},
        {.f32 = 0}, {.f32 = 0}, {.f32 = 0}, {.f32 = 1}, {.f32 = 0}
    };
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(handle, NODE_IMAGE_COLOR_FILTER, &item);
}

void ResetArkUIImageTintColor(ArkUI_NodeHandle handle) {
    if (!handle) {
        return;
    }
    GetNodeApi()->resetAttribute(handle, NODE_IMAGE_COLOR_FILTER);
}

void SetArkUIImageColorFilter(ArkUI_NodeHandle handle, const std::vector<float> &matrix) {
    if (!handle || matrix.size() < 20) {
        return;
    }
    ArkUI_NumberValue value[20];
    for (int i = 0; i < 20; ++i) {
        value[i].f32 = matrix[i];
    }
    ArkUI_AttributeItem item = {value, 20};
    GetNodeApi()->setAttribute(handle, NODE_IMAGE_COLOR_FILTER, &item);
}

void ResetArkUIImageColorFilter(ArkUI_NodeHandle handle) {
    if (!handle) {
        return;
    }
    GetNodeApi()->resetAttribute(handle, NODE_IMAGE_COLOR_FILTER);
}

void SetArkUIImageCapInsets(ArkUI_NodeHandle handle, float top, float left, float bottom, float right) {
    if (!handle) {
        return;
    }

    ArkUI_NumberValue value[] = {{.f32 = left}, {.f32 = top}, {.f32 = right}, {.f32 = bottom}};

    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(handle, NODE_IMAGE_RESIZABLE, &item);
}

void SetArkUIImageSourceSize(ArkUI_NodeHandle handle, float width_px, float height_px) {
    if (!handle) {
        return;
    }
    if (width_px <= 0.f || height_px <= 0.f) {
        return;
    }
    ArkUI_NumberValue value[] = {{.i32 = static_cast<int>(width_px)}, {.i32 = static_cast<int>(height_px)}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(handle, NODE_IMAGE_SOURCE_SIZE, &item);
}

// ============================================================================
// OH_Drawing_Lattice 精确九宫格（API 24 引入）
//
// 老路径（SetArkUIImageCapInsets 四值）由 NODE_IMAGE_RESIZABLE 接收 left/top/right/
// bottom（单位 vp），系统内部按四边构造隐式九宫格。实测在图片自身分辨率与目标绘制
// 尺寸差异较大、或用户 vp 边距接近图片一半宽/高时，鸿蒙旧实现会出现中间可拉伸区
// 计算错乱、边角像素被拉伸等问题（本 issue 的直接触发点）。
//
// API 24 起 NODE_IMAGE_RESIZABLE 的 .object 字段支持传入 OH_Drawing_Lattice，
// 可以按图片像素坐标显式指定 xDivs/yDivs 分割线，鸿蒙侧走的是新的 lattice
// 精确路径，行为对齐 Skia SkCanvas::drawImageLattice，与 iOS/Android 一致。
//
// 兼容策略与弱符号声明**已上移到文件顶部全局命名空间**（见文件顶部注释块），
// 目的是让所有 TU 看到的 ::OH_Drawing_Lattice 是同一符号，避免因 namespace 内
// elaborated type specifier 规则引入独立类型导致跨 TU 链接失败。这里仅保留
// 相关 API 的运行时判定与使用逻辑。
// ============================================================================

// 判定当前运行进程是否可以走 lattice 路径。
// 双门控：系统 API 版本 >= 24 && 弱符号被动态链接器解析成功。
static bool IsArkUIImageLatticeAvailable() {
    static const bool available = []() {
        // 部分鸿蒙发行版 / 模拟器可能 API 数值 >=24 但对应符号尚未导出，因此不能
        // 只信 OH_GetSdkApiVersion()；下面的符号非空判定才是"运行时事实"。
        if (OH_GetSdkApiVersion() < 24) {
            return false;
        }
        return &OH_Drawing_LatticeCreate != nullptr && &OH_Drawing_LatticeDestroy != nullptr;
    }();
    return available;
}

void SetArkUIImageCapInsetsWithLattice(ArkUI_NodeHandle handle, float top, float left, float bottom,
                                       float right, float image_width_px, float image_height_px,
                                       ::OH_Drawing_Lattice **out_lattice) {
    // 默认输出 nullptr，所有 fallback / 失败 / 无 out_lattice 分支都保持该初始值。
    if (out_lattice) {
        *out_lattice = nullptr;
    }
    if (!handle) {
        return;
    }

    // ------------------------------------------------------------------
    // 单位约定（重要！与之前实现相比语义已修正）：
    //   * top/left/bottom/right 四个参数**直接就是"图片自身像素坐标"下的偏移量**，
    //     即 lattice xDivs/yDivs 需要的原生单位。与 iOS UIImage.resizableImageWithCap
    //     Insets 的 capInsets(UIEdgeInsets in points) 语义对齐——上层协议约定的
    //     capInsets 就是"图片自身独立坐标"，鸿蒙的图片没有 iOS 的 point 概念，直接
    //     对应"图片像素"最自然。
    //   * **不再** 做 `vp * dpi` 的换算。之前把参数当 vp、再乘 dpi 转屏幕像素当分割
    //     线的做法是错的——屏幕 dpi 与图片自身像素坐标没有必然关系，会导致上层传
    //     的 "10" 变成 "10 * dpi"（如 30 px）落到错误位置，正是"拉伸不对"的根因。
    //   * 老路径 fallback（SetArkUIImageCapInsets → NODE_IMAGE_RESIZABLE 四值）
    //     期望的单位是 **vp**（ArkUI 原生 API 规定），fallback 时需要按图片像素 →
    //     vp 反向换算（除以 dpi）后再下发。
    // ------------------------------------------------------------------

    const double dpi = KRConfig::GetDpi();
    // fallback 到老路径用的 vp 值（图片像素 / dpi）。抽出来避免每个 return 分支重复。
    auto old_path_fallback = [&]() {
        SetArkUIImageCapInsets(handle, static_cast<float>(top / dpi), static_cast<float>(left / dpi),
                               static_cast<float>(bottom / dpi), static_cast<float>(right / dpi));
    };

    // 只要具备任一"退化条件"就走老四值路径：
    //   1. 系统 <API 24 或 lattice 符号未解析；
    //   2. 图片像素尺寸未知（未加载完成时 image_width_px/image_height_px 为 0/负数）。
    // 图片尺寸未知时无法确保 lattice 分割线不越界，宁可先按老路径显示（视觉退化但
    // 不崩），加载完成后调用方会带上尺寸再调一次，届时才升级到 lattice。
    if (!IsArkUIImageLatticeAvailable() || image_width_px <= 0 || image_height_px <= 0) {
        old_path_fallback();
        return;
    }

    // 四值直接就是"图片像素坐标"，四舍五入到整数（lattice xDivs/yDivs 要求 int）
    // 并夹到 [0, image_size] 区间，避免越界或反向。
    auto round_i = [](float v) -> int {
        return static_cast<int>(v + 0.5f);
    };
    auto clamp_int = [](int v, int lo, int hi) -> int {
        if (v < lo) {
            return lo;
        }
        if (v > hi) {
            return hi;
        }
        return v;
    };

    const int img_w = static_cast<int>(image_width_px + 0.5f);
    const int img_h = static_cast<int>(image_height_px + 0.5f);
    int left_px = clamp_int(round_i(left), 0, img_w);
    int right_px = clamp_int(img_w - round_i(right), 0, img_w);
    int top_px = clamp_int(round_i(top), 0, img_h);
    int bottom_px = clamp_int(img_h - round_i(bottom), 0, img_h);

    // 保证 xDivs / yDivs 严格递增（否则 lattice 语义未定义）。若因边距过大导致
    // 两条分割线交叉（left+right >= imgW），退回老路径避免生成非法 lattice。
    if (left_px >= right_px || top_px >= bottom_px) {
        old_path_fallback();
        return;
    }

    const int x_divs[2] = {left_px, right_px};
    const int y_divs[2] = {top_px, bottom_px};

    // 显式构造 bounds：官方文档说 bounds=nullptr 时"默认为原图矩形"，理论上等价，
    // 但用户实测简化用法拉伸不稳定；这里按 drawing_lattice.h 示例严格显式传入，
    // 消除任何"nullptr 简化路径"上潜在的实现差异。
    // 注意：bounds 的值必须是整数（doc："The value must be an integer and is rounded
    // down"），我们本来就用 int，安全。
    struct OH_Drawing_Rect *bounds = nullptr;
    if (&OH_Drawing_RectCreate != nullptr) {
        bounds = OH_Drawing_RectCreate(0.f, 0.f, static_cast<float>(img_w), static_cast<float>(img_h));
    }

    // 显式构造 rectTypes：9 格全 DEFAULT（枚举值 0），与"nullptr + count=0"简化用法
    // 等价。使用 opaque enum 类型 OH_Drawing_LatticeRectType（与 SDK header 严格一致，
    // 底层 int32_t，值 0 即 DEFAULT——官方文档中该枚举首个成员即 DEFAULT）。
    // 注意：文档要求"rectTypes != nullptr 时，rectTypeCount 必须等于 (xCount+1)*(yCount+1)"，
    // 我们 xCount=yCount=2 → 9，严格对应。
    // 说明：lattice 的"固定 / 拉伸"由**格子位置的奇偶性**决定，不由 rectTypes 决定；
    // rectTypes 只控制"这个格子怎么填"（DEFAULT=画原图 / TRANSPARENT=透明 / FIXED_COLOR
    // =纯色）。所以九宫格拉伸 9 格都是 DEFAULT。
    constexpr auto kLatticeRectTypeDefault = static_cast<OH_Drawing_LatticeRectType>(0);  // = DEFAULT
    const OH_Drawing_LatticeRectType rect_types[9] = {
        kLatticeRectTypeDefault, kLatticeRectTypeDefault, kLatticeRectTypeDefault,  // 第一行
        kLatticeRectTypeDefault, kLatticeRectTypeDefault, kLatticeRectTypeDefault,  // 第二行
        kLatticeRectTypeDefault, kLatticeRectTypeDefault, kLatticeRectTypeDefault,  // 第三行
    };

    struct OH_Drawing_Lattice *lattice = nullptr;
    // 官方文档 (drawing_lattice.h)：
    //   "the lattices on both even columns and even rows are fixed, and they are
    //    drawn at their original size ...; the lattices that are not on even columns
    //    and even rows are scaled to accommodate the remaining space."
    // xCount=yCount=2 → 3x3=9 格：四角(偶,偶)固定、中心(奇,奇)双向拉伸、四边(偶,奇)
    // 或(奇,偶)单向拉伸，正好对应九宫格 capInsets 语义。
    int32_t ret = OH_Drawing_LatticeCreate(x_divs, y_divs, 2, 2, bounds, nullptr, 0, nullptr, 0, &lattice);
    if (ret != 0 /* OH_DRAWING_SUCCESS */ || !lattice) {
        // 兜底：lattice 创建失败（理论上不会发生，比如参数越界）退回老路径而不是留空。
        if (lattice) {
            OH_Drawing_LatticeDestroy(lattice);
        }
        if (bounds && &OH_Drawing_RectDestroy != nullptr) {
            OH_Drawing_RectDestroy(bounds);
        }
        old_path_fallback();
        return;
    }
    

    // 关键 fix：**只**传 .object=lattice，.value 必须置空。
    // NODE_IMAGE_RESIZABLE 官方文档虽然把 .value（四个 vp 值）与 .object（Lattice）列
    // 在同一属性下，但二者是**互斥**的两种设置模式。若同时提供 .value 与 .object，
    // 鸿蒙内部会优先按 .value 走老四值 RESIZABLE 路径，把 .object 上的 lattice 忽略
    // 掉——表现就是"看起来设了 lattice，实际拉伸仍然错误"。
    // 因此这里明确 value=nullptr, size=0，让 ArkUI 无歧义地走 .object=Lattice 分支。
    ArkUI_AttributeItem item = {};
    item.value = nullptr;
    item.size = 0;
    item.object = lattice;
    GetNodeApi()->setAttribute(handle, NODE_IMAGE_RESIZABLE, &item);
    
    ArkUI_NumberValue autoResizeValue[] = {{.i32 = 1}};
    ArkUI_AttributeItem autoResizeItem = {.value = autoResizeValue, .size = 1};
    GetNodeApi()->setAttribute(handle, NODE_IMAGE_AUTO_RESIZE, &autoResizeItem);
    //SetArkUIIMageResizeMode(handle, ARKUI_OBJECT_FIT_FILL);
    
    GetNodeApi()->markDirty(handle, ArkUI_NodeDirtyFlag::NODE_NEED_RENDER);

    // 关键：setAttribute 虽为同步语义，但实测立即 OH_Drawing_LatticeDestroy 会引发崩溃
    // （鉴定 ArkUI 内部并非完全拷贝，或重绘管线异步引用了 lattice 数据）。
    // 因此本函数不再在尾部销毁 lattice，而是把所有权交给调用方（通过
    // out_lattice 输出）；调用方在它确定不会再被使用的时机（例如自身实例
    // 销毁时）统一释放。若调用方传 out_lattice == nullptr，则表示不接管，
    // 本函数立即销毁——保底不泄漏，代价是可能降低稳定性（旧行为）。
    // bounds 只在本函数内使用（供 OH_Drawing_LatticeCreate 读取），setAttribute 后即可
    // 释放：官方文档明确 bounds 仅用于创建时描述图片矩形，不参与后续绘制。
    if (out_lattice) {
        *out_lattice = lattice;
    } else {
        // 历史调用方不接管：保底销毁（可能崩溃，但不泄漏）。
        OH_Drawing_LatticeDestroy(lattice);
    }
    if (bounds && &OH_Drawing_RectDestroy != nullptr) {
        OH_Drawing_RectDestroy(bounds);
    }
}

void ResetArkUIImageCapInsets(ArkUI_NodeHandle handle) {
    if (!handle) {
        return;
    }
    GetNodeApi()->resetAttribute(handle, NODE_IMAGE_RESIZABLE);
}

void ResetArkUIImageBlendMode(ArkUI_NodeHandle handle) {
    if (!handle) {
        return;
    }
    GetNodeApi()->resetAttribute(handle, NODE_BLEND_MODE);
}

void SetArkUIScrollDirection(ArkUI_NodeHandle handle, bool direction_row) {
    if (!handle) {
        return;
    }

    auto scrollDirection = direction_row ? ArkUI_ScrollDirection::ARKUI_SCROLL_DIRECTION_HORIZONTAL
                                         : ArkUI_ScrollDirection::ARKUI_SCROLL_DIRECTION_VERTICAL;
    ArkUI_NumberValue value[] = {{.i32 = scrollDirection}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(handle, NODE_SCROLL_SCROLL_DIRECTION, &item);
}

void SetArkUIScrollPagingEnabled(ArkUI_NodeHandle handle, bool enable) {
    if (!handle) {
        return;
    }

    ArkUI_NumberValue value[] = {{.i32 = enable}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(handle, NODE_SCROLL_ENABLE_PAGING, &item);
}

void SetArkUIBouncesEnabled(ArkUI_NodeHandle Handle, bool enable) {
    if (!Handle) {
        return;
    }

    ArkUI_EdgeEffect edgeEffect =
        enable ? ArkUI_EdgeEffect::ARKUI_EDGE_EFFECT_SPRING : ArkUI_EdgeEffect::ARKUI_EDGE_EFFECT_NONE;
    ArkUI_NumberValue value[] = {{.i32 = edgeEffect}, {.i32 = 1}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(Handle, NODE_SCROLL_EDGE_EFFECT, &item);
}

void SetArkUIShowScrollerIndicator(ArkUI_NodeHandle handle, bool enable) {
    if (!handle) {
        return;
    }

    auto display_mode = enable ? ARKUI_SCROLL_BAR_DISPLAY_MODE_AUTO : ARKUI_SCROLL_BAR_DISPLAY_MODE_OFF;
    ArkUI_NumberValue value[] = {{.i32 = display_mode}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(handle, NODE_SCROLL_BAR_DISPLAY_MODE, &item);
}

void SetArkUINestedScroll(ArkUI_NodeHandle handle, ArkUI_ScrollNestedMode forward, ArkUI_ScrollNestedMode backward) {
    if (!handle) {
        return;
    }

    ArkUI_NumberValue value[] = {{.i32 = forward}, {.i32 = backward}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(value[0])};
    GetNodeApi()->setAttribute(handle, NODE_SCROLL_NESTED_SCROLL, &item);
}

void ResetArkUINestedScroll(ArkUI_NodeHandle handle) {
    if (!handle) {
        return;
    }

    GetNodeApi()->resetAttribute(handle, NODE_SCROLL_NESTED_SCROLL);
}

void SetArkUIScrollEnabled(ArkUI_NodeHandle handle, bool enable) {
    if (!handle) {
        return;
    }

    ArkUI_NumberValue value[] = {{.i32 = enable}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(handle, NODE_SCROLL_ENABLE_SCROLL_INTERACTION, &item);
}

KRPoint GetArkUIScrollContentOffset(ArkUI_NodeHandle handle) {
    if (!handle) {
        return KRPoint();
    }

    auto item = GetNodeApi()->getAttribute(handle, NODE_SCROLL_OFFSET);
    return item ? KRPoint{item->value[0].f32, item->value[1].f32} : KRPoint();
}

void SetArkUIContentOffset(ArkUI_NodeHandle handle, float offset_x, float offset_y, bool animate, int duration, int curve,
                           float damping) {
    if (!handle) {
        return;
    }

    if (duration < 0) {
        duration = 0;
    }
    int durationForArkUI = duration;
    int enableDefaultSpringAnimation = animate ? 1 : 0;
    if (duration > 0 && animate) {
        if (curve == 0 && damping == 1.0f) {
            // Align with Android: use the platform default scroll animation when no extra spring effect is needed.
            durationForArkUI = 0;
            enableDefaultSpringAnimation = 1;
        } else {
            // Default spring animation should be disabled when custom animation duration is specified,
            // otherwise custom animation duration will not take effect.
            enableDefaultSpringAnimation = 0;
        }
    }
    ArkUI_NumberValue value[] = {
        {.f32 = offset_x},
        {.f32 = offset_y},
        {.i32 = durationForArkUI},
        {.i32 = curve == 0 ? ARKUI_CURVE_EASE : ARKUI_CURVE_LINEAR},
        {.i32 = enableDefaultSpringAnimation},  // whether to enable the default spring animation
        {.i32 = 1},                             // whether scrolling can cross the boundary
        {.i32 = 1}                              // whether the component can stop at an overscrolled position (API 20+)
    };
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(handle, NODE_SCROLL_OFFSET, &item);
}

ArkUI_ScrollState GetArkUIScrollerState(ArkUI_NodeEvent *event, int scroll_state_index) {
    if (!event) {
        return ArkUI_ScrollState::ARKUI_SCROLL_STATE_IDLE;
    }
    auto component_event_data = OH_ArkUI_NodeEvent_GetNodeComponentEvent(event);
    return static_cast<ArkUI_ScrollState>(component_event_data->data[scroll_state_index].i32);
}

void SetArkUIStackNodeContentAlignment(ArkUI_NodeHandle handle, const ArkUI_Alignment &alignment) {
    if (!handle) {
        return;
    }

    ArkUI_NumberValue value[] = {{.i32 = alignment}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(handle, NODE_STACK_ALIGN_CONTENT, &item);
}

void SetArkUIMargin(ArkUI_NodeHandle handle, float start, float top, float end, float bottom) {
    if (!handle) {
        return;
    }

    ArkUI_NumberValue value[] = {{.f32 = top}, {.f32 = end}, {.f32 = bottom}, {.f32 = start}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(handle, NODE_MARGIN, &item);
}

void SetArkUIPadding(ArkUI_NodeHandle handle, float start, float top, float end, float bottom) {
    if (!handle) {
        return;
    }
    std::array<ArkUI_NumberValue, 4> value = {static_cast<float>(top), static_cast<float>(end),
                                              static_cast<float>(bottom), static_cast<float>(start)};
    ArkUI_AttributeItem item = {value.data(), value.size()};
    GetNodeApi()->setAttribute(handle, NODE_PADDING, &item);
}

void UpdateInputNodeFocusStatus(ArkUI_NodeHandle node, int32_t status) {
    ArkUI_NumberValue value[] = {{.i32 = status}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(node, NODE_FOCUS_STATUS, &item);
}

void UpdateInputNodeFocusable(ArkUI_NodeHandle node, int32_t enable) {
    ArkUI_NumberValue value[] = {{.i32 = enable}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(node, NODE_FOCUSABLE, &item);
}

void UpdateInputNodeKeyboardType(ArkUI_NodeHandle node, ArkUI_TextInputType input_type) {
    ArkUI_NumberValue value[] = {{.i32 = input_type}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(node, NODE_TEXT_INPUT_TYPE, &item);
}

void UpdateInputNodeEnterKeyType(ArkUI_NodeHandle node, ArkUI_EnterKeyType enter_key_type) {
    ArkUI_NumberValue value[] = {{.i32 = enter_key_type}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(node, NODE_TEXT_INPUT_ENTER_KEY_TYPE, &item);
}

ArkUI_EnterKeyType GetInputNodeEnterKeyType(ArkUI_NodeHandle node) {
    auto item = GetNodeApi()->getAttribute(node, NODE_TEXT_INPUT_ENTER_KEY_TYPE);
    return item ? static_cast<ArkUI_EnterKeyType>(item->value[0].i32) : ARKUI_ENTER_KEY_TYPE_NEW_LINE;
}

void UpdateInputNodeMaxLength(ArkUI_NodeHandle node, int32_t max_length) {
    ArkUI_NumberValue value[] = {{.i32 = max_length}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(node, NODE_TEXT_INPUT_MAX_LENGTH, &item);
}

void ResetInputNodeMaxLength(ArkUI_NodeHandle node) {
    GetNodeApi()->resetAttribute(node, NODE_TEXT_INPUT_MAX_LENGTH);
}

void UpdateInputNodeContentText(ArkUI_NodeHandle node, const std::string &text) {
    ArkUI_AttributeItem item = {.string = text.c_str()};
    GetNodeApi()->setAttribute(node, NODE_TEXT_INPUT_TEXT, &item);
}

std::string GetInputNodeContentText(ArkUI_NodeHandle node) {
    auto item = GetNodeApi()->getAttribute(node, NODE_TEXT_INPUT_TEXT);
    return item ? item->string : "";
}

void UpdateInputNodePlaceholder(ArkUI_NodeHandle node, const std::string &placeholder) {
    ArkUI_AttributeItem item = {.string = placeholder.c_str()};
    GetNodeApi()->setAttribute(node, NODE_TEXT_INPUT_PLACEHOLDER, &item);
}

void UpdateInputNodePlaceholderColor(ArkUI_NodeHandle node, uint32_t placeholder_color) {
    ArkUI_NumberValue value = {.u32 = placeholder_color};
    ArkUI_AttributeItem item = {&value, 1};
    GetNodeApi()->setAttribute(node, NODE_TEXT_INPUT_PLACEHOLDER_COLOR, &item);
}

void UpdateInputNodeCaretrColor(ArkUI_NodeHandle node, uint32_t caret_color) {
    ArkUI_NumberValue value = {.u32 = caret_color};
    ArkUI_AttributeItem item = {&value, 1};
    GetNodeApi()->setAttribute(node, NODE_TEXT_INPUT_CARET_COLOR, &item);
}

uint32_t ClampSelectionColorAlpha(uint32_t color) {
    uint32_t alpha = (color >> kColorAlphaShift) & kColorAlphaMask;
    if (alpha > kSelectionColorMaxAlpha) {
        color = (kSelectionColorMaxAlpha << kColorAlphaShift) | (color & kColorRGBMask);
    }
    return color;
}

void UpdateInputNodeSelectionColor(ArkUI_NodeHandle node, uint32_t color) {
    color = ClampSelectionColorAlpha(color);
    ArkUI_NumberValue value = {.u32 = color};
    ArkUI_AttributeItem item = {&value, 1};
    GetNodeApi()->setAttribute(node, NODE_TEXT_INPUT_SELECTED_BACKGROUND_COLOR, &item);
}

void UpdateTextAreaNodeSelectionColor(ArkUI_NodeHandle node, uint32_t color) {
    color = ClampSelectionColorAlpha(color);
    ArkUI_NumberValue value = {.u32 = color};
    ArkUI_AttributeItem item = {&value, 1};
    GetNodeApi()->setAttribute(node, NODE_TEXT_AREA_SELECTED_BACKGROUND_COLOR, &item);
}

void UpdateInputNodeTextAlign(ArkUI_NodeHandle node, const std::string &text_align) {
    ArkUI_NumberValue value[] = {{.i32 = static_cast<int32_t>(ConvertToArkUITextAlign(text_align))}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(node, NODE_TEXT_ALIGN, &item);
}

void UpdateInputNodePlaceholderFont(ArkUI_NodeHandle node, uint32_t font_size, ArkUI_FontWeight font_weight, bool fontSizeScaleFollowSystem, float font_size_px) {
    ArkUI_NumberValue fontWeight = {.i32 = font_weight};
    ArkUI_NumberValue tempStyle = {.i32 = ARKUI_FONT_STYLE_NORMAL};
    std::array<ArkUI_NumberValue, 3> value = {{{.f32 = static_cast<float>(font_size)}, tempStyle, fontWeight}};
    ArkUI_AttributeItem item = {value.data(), value.size()};
    GetNodeApi()->setAttribute(node, NODE_TEXT_INPUT_PLACEHOLDER_FONT, &item);
    GetNodeApi()->setAttribute(node, NODE_FONT_SIZE, &item);
    {
        // 如果禁用输入框内字体缩放需要设置为px
        float font_size_temp = font_size;
        if (!fontSizeScaleFollowSystem) {
            GetNodeApi()->setLengthMetricUnit(node, ARKUI_LENGTH_METRIC_UNIT_PX);
            font_size_temp = font_size_px;
        }
        ArkUI_NumberValue valueSize[] = {static_cast<float>(font_size_temp)};
        ArkUI_AttributeItem itemSize = {valueSize, sizeof(valueSize) / sizeof(ArkUI_NumberValue)};
        GetNodeApi()->setAttribute(node, NODE_FONT_SIZE, &itemSize);
        if (!fontSizeScaleFollowSystem) {
            GetNodeApi()->setLengthMetricUnit(node, ARKUI_LENGTH_METRIC_UNIT_DEFAULT);
        }
    }
    {
        ArkUI_NumberValue fontWeight = {.i32 = font_weight};
        ArkUI_NumberValue valueWeight[] = {fontWeight};
        ArkUI_AttributeItem itemWeight = {valueWeight, 1};
        GetNodeApi()->setAttribute(node, NODE_FONT_WEIGHT, &itemWeight);
    }
}

void UpdateInputNodeColor(ArkUI_NodeHandle node, uint32_t color) {
    ArkUI_NumberValue preparedColorValue[] = {{.u32 = color}};
    ArkUI_AttributeItem colorItem = {preparedColorValue, sizeof(preparedColorValue) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(node, NODE_FONT_COLOR, &colorItem);
}

uint32_t GetInputNodeSelectionStartPosition(ArkUI_NodeHandle node) {
    // ArkUI_NumberValue preparedColorValue[] = {{.u32 = color}};
    // ArkUI_AttributeItem colorItem = {preparedColorValue, sizeof(preparedColorValue) / sizeof(ArkUI_NumberValue)};
    auto item = GetNodeApi()->getAttribute(node, NODE_TEXT_INPUT_TEXT_SELECTION);
    return item ? item->value[0].i32 : 0;
}

void UpdateInputNodeSelectionStartPosition(ArkUI_NodeHandle node, int32_t index) {
    std::array<ArkUI_NumberValue, 2> value = {{{.i32 = index}, {.i32 = index}}};
    ArkUI_AttributeItem item = {value.data(), value.size()};
    GetNodeApi()->setAttribute(node, NODE_TEXT_INPUT_TEXT_SELECTION, &item);
}

std::pair<uint32_t, uint32_t> GetInputNodeSelectionRange(ArkUI_NodeHandle node) {
    auto item = GetNodeApi()->getAttribute(node, NODE_TEXT_INPUT_TEXT_SELECTION);
    if (item && item->size >= 2) {
        return {item->value[0].i32, item->value[1].i32};
    }
    return {0, 0};
}

void UpdateTextAreaNodeLineHeight(ArkUI_NodeHandle node, float lineHeight) {
    ArkUI_NumberValue value[] = {lineHeight};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    // NODE_TEXT_AREA_LINE_HEIGHT 属性未来版本待支持
    GetNodeApi()->setAttribute(node, NODE_TEXT_LINE_HEIGHT, &item);
}

void UpdateLoadingProgressNodeColor(ArkUI_NodeHandle node, uint32_t hexColorValue) {
    auto nodeAPI = GetNodeApi();
    ArkUI_NumberValue value[] = {{.u32 = hexColorValue}};
    ArkUI_AttributeItem colorItem = {value, 1};
    nodeAPI->setAttribute(node, NODE_LOADING_PROGRESS_COLOR, &colorItem);
}

void UpdateNodeClipPath(ArkUI_NodeHandle node, float width, float height, const std::string &pathCommand) {
    auto nodeAPI = GetNodeApi();
    if (pathCommand.empty()) {
        nodeAPI->resetAttribute(node, NODE_CLIP_SHAPE);
        return;
    }
    ArkUI_NumberValue value[] = {
        {.i32 = ARKUI_CLIP_TYPE_PATH},
        {.f32 = width},
        {.f32 = height}
    };
    ArkUI_AttributeItem clipPathItem = {
        .value = value,
        .size = 3,
        .string = pathCommand.c_str()
    };
    nodeAPI->setAttribute(node, NODE_CLIP_SHAPE, &clipPathItem);
}

}  // namespace util
}  // namespace kuikly
