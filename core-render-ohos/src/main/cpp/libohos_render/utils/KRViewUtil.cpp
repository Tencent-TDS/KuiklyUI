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

#include "libohos_render/utils/KRViewUtil.h"

#include "libohos_render/export/IKRRenderViewExport.h"
#include "libohos_render/utils/KRThreadChecker.h"

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
    float x = ConvertToDouble(splits[0]);
    float y = ConvertToDouble(splits[1]);
    float radius = ConvertToDouble(splits[2]);
    uint32_t color = ConvertToHexColor(splits[3]);
    ArkUI_NumberValue value[] = {radius,         {.i32 = 0}, x, y, {.i32 = ARKUI_SHADOW_TYPE_COLOR},
                                 {.u32 = color}, {.i32 = 0}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    nodeAPI->setAttribute(node, NODE_CUSTOM_SHADOW, &item);
}

void SetTextShadow(OH_Drawing_TextShadow *shadow, const std::string &css_box_shadow) {
    auto splits = ConvertSplit(css_box_shadow, " ");
    float x = ConvertToDouble(splits[0]);
    float y = ConvertToDouble(splits[1]);
    float radius = ConvertToDouble(splits[2]);
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
    auto boderWidth = ConvertToDouble(splits[0]);
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

    ArkUI_NumberValue value[] = {{.f32 = 0}, {.f32 = 0}, {.f32 = 0}, {.f32 = std::get<1>(hex_color)}, {.f32 = 0},
                                 {.f32 = 0}, {.f32 = 0}, {.f32 = 0}, {.f32 = std::get<2>(hex_color)}, {.f32 = 0},
                                 {.f32 = 0}, {.f32 = 0}, {.f32 = 0}, {.f32 = std::get<3>(hex_color)}, {.f32 = 0},
                                 {.f32 = 0}, {.f32 = 0}, {.f32 = 0}, {.f32 = std::get<0>(hex_color)}, {.f32 = 0}};

    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(handle, NODE_IMAGE_COLOR_FILTER, &item);
}

void ResetArkUIImageTintColor(ArkUI_NodeHandle handle) {
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

void SetArkUIContentOffset(ArkUI_NodeHandle handle, float offset_x, float offset_y, bool animate, int duration) {
    if (!handle) {
        return;
    }

    if (duration < 0) {
        duration = 0;
    }
    int enableDefaultSpringAnimation = animate ? 1 : 0;
    if (duration > 0 && animate) {
        // Default spring animation should be disabled when custom animation duration is specified,
        // otherwise custom animation duration will not take effect.
        enableDefaultSpringAnimation = 0;
    }
    ArkUI_NumberValue value[] = {
        {.f32 = offset_x},
        {.f32 = offset_y},
        {.i32 = duration},
        {.i32 = ARKUI_CURVE_EASE},
        {.i32 = enableDefaultSpringAnimation},  // whether to enable the default spring animation
        {.i32 = 1}                              // whether scrolling can cross the boundary
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

void UpdateInputNodeMaxLength(ArkUI_NodeHandle node, int32_t max_length) {
    ArkUI_NumberValue value[] = {{.i32 = max_length}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(node, NODE_TEXT_INPUT_MAX_LENGTH, &item);
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
    ArkUI_AttributeItem item = {&value, sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(node, NODE_TEXT_INPUT_PLACEHOLDER_COLOR, &item);
}

void UpdateInputNodeCaretrColor(ArkUI_NodeHandle node, uint32_t caret_color) {
    ArkUI_NumberValue value = {.u32 = caret_color};
    ArkUI_AttributeItem item = {&value, sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(node, NODE_TEXT_INPUT_CARET_COLOR, &item);
}

void UpdateInputNodeTextAlign(ArkUI_NodeHandle node, const std::string &text_align) {
    ArkUI_NumberValue value[] = {{.i32 = static_cast<int32_t>(ConvertToArkUITextAlign(text_align))}};
    ArkUI_AttributeItem item = {value, sizeof(value) / sizeof(ArkUI_NumberValue)};
    GetNodeApi()->setAttribute(node, NODE_TEXT_ALIGN, &item);
}

void UpdateInputNodePlaceholderFont(ArkUI_NodeHandle node, uint32_t font_size, ArkUI_FontWeight font_weight) {
    ArkUI_NumberValue fontWeight = {.i32 = font_weight};
    ArkUI_NumberValue tempStyle = {.i32 = ARKUI_FONT_STYLE_NORMAL};
    std::array<ArkUI_NumberValue, 3> value = {{{.f32 = static_cast<float>(font_size)}, tempStyle, fontWeight}};
    ArkUI_AttributeItem item = {value.data(), value.size()};
    GetNodeApi()->setAttribute(node, NODE_TEXT_INPUT_PLACEHOLDER_FONT, &item);
    GetNodeApi()->setAttribute(node, NODE_FONT_SIZE, &item);
    {
        ArkUI_NumberValue valueSize[] = {static_cast<float>(font_size)};
        ArkUI_AttributeItem itemSize = {valueSize, sizeof(valueSize) / sizeof(ArkUI_NumberValue)};
        GetNodeApi()->setAttribute(node, NODE_FONT_SIZE, &itemSize);
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
}  // namespace util
}  // namespace kuikly
