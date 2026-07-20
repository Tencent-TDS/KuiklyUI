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

#include "libohos_render/expand/components/image/KRImageView.h"

#include <deviceinfo.h>
#include <resourcemanager/ohresmgr.h>
#include <sstream>
#include <string_view>
#include "libohos_render/api/src/KRAnyDataInternal.h"
#include "libohos_render/expand/components/image/KRImageAdapterManager.h"
#include "libohos_render/expand/modules/cache/KRMemoryCacheModule.h"
#include "libohos_render/foundation/KRConfig.h"
#include "libohos_render/manager/KRRenderManager.h"
#include "libohos_render/manager/KRSnapshotManager.h"
#include "libohos_render/utils/KRThreadChecker.h"
#include "libohos_render/utils/KRURIHelper.h"
#include "libohos_render/utils/KRStringUtil.h"
#include "libohos_render/utils/KRViewContext.h"

// OH_Drawing_LatticeDestroy 弱符号声明：与 KRViewUtil.cpp 里的声明保持一致，
// 用于 OnDestroy 时释放 lattice_pool_ 里累计的图形对象。弱符号保证旧系统
// （API < 24）链接时不失败；运行时官方依赖未提供时 &OH_Drawing_LatticeDestroy
// 会为 nullptr，可提前拦截避免崩溃。同一 TU 内重复声明弱符号目前只能以全局
// 符号形式存在（native drawing 头不包 KRImageView.cpp），因此直接声明而不 include
// <native_drawing/drawing_lattice.h> 避免引入不必要的头依赖。
extern "C" int32_t OH_Drawing_LatticeDestroy(struct OH_Drawing_Lattice *lattice) __attribute__((weak));

constexpr char kPropNameSrc[] = "src";
constexpr char kBase64Prefix[] = "data:image";
constexpr char kHttpPrefix[] = "http:";
constexpr char kHttpsPrefix[] = "https:";
constexpr char kFilePrefix[] = "file:";
constexpr char kAssetsPrefix[] = "assets:";

constexpr char kPropNameResize[] = "resize";
constexpr char kResizeModeCover[] = "cover";
constexpr char kResizeModeContain[] = "contain";
constexpr char kResizeModeStretch[] = "stretch";

constexpr char kPropNameDragEnable[] = "dragEnable";

constexpr char kPropNameBlurRadius[] = "blurRadius";
constexpr char kPropNameTintColor[] = "tintColor";
constexpr char kPropNameColorFilter[] = "colorFilter";
constexpr char kPropNameCapInsets[] = "capInsets";
constexpr char kPropNameDotNineImage[] = "dotNineImage";
constexpr char kPropNameImageParams[] = "imageParams";
// imageParams map 中的 "__scale__" 键：上层指定的图片自身像素密度倍率
//（如 @2x = 2），供 capInsets + lattice 路径下的 NODE_IMAGE_SOURCE_SIZE 使用。
constexpr char kImageParamKeyScale[] = "__scale__";
// capinset_image_scale_ 默认值：1.0 表示"1 图片像素 = 1 vp"，即未显式指定
// __scale__ 时的默认语义。非正值/缺失时也 fallback 到该默认。
constexpr float DEFAULT_CAPINST_IMAGE_SCALE = 1.0f;

constexpr char kEventNameLoadSuccess[] = "loadSuccess";
constexpr char kEventNameLoadResolution[] = "loadResolution";
constexpr char kEventNameLoadFailure[] = "loadFailure";
constexpr char kEventNameLoadErrorCode[] = "errorCode";
constexpr char kParamKeyImageWidth[] = "imageWidth";
constexpr char kParamKeyImageHeight[] = "imageHeight";
constexpr char kPropNameMaskLinearGradient[] = "maskLinearGradient";

bool isBase64(const std::string &src) {
    return src.find(kBase64Prefix) == 0;
}
bool isNetwork(const std::string &src) {
    return src.find(kHttpPrefix) == 0 || src.find(kHttpsPrefix) == 0;
}
bool isFile(const std::string &src) {
    return src.find(kFilePrefix) == 0;
}
bool isAssets(const std::string &src) {
    return src.find(kAssetsPrefix) == 0;
}

ArkUI_NodeHandle KRImageView::CreateNode() {
    return kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_IMAGE);
}

void KRImageView::DidInit() {
    auto resize_mode = NewKRRenderValue(kResizeModeCover);
    SetResizeMode(resize_mode);
    kuikly::util::SetArkUIImageDraggable(GetNode(), false);
    kuikly::util::UpdateNodeFrame(GetNode(), KRRect());
}

bool KRImageView::ReuseEnable() {
    static bool reuse_enable = true;
    static std::once_flag flag;
    std::call_once(flag, [this]() {
        if (OH_GetSdkApiVersion() < 20) {
            auto rootView = GetRootView().lock();
            if (rootView) {
                reuse_enable = !rootView->GetContext()->Config()->ImeMode();
            }
        }
    });
    return reuse_enable;
}

void KRImageView::OnDestroy() {
    ResetMaskLinearGradientNode();
    // 释放本实例生命周期内累计创建的所有 OH_Drawing_Lattice 对象：
    // ArkUI setAttribute 阶段不能立即销毁（会崩），因此既不在下发位置销毁、
    // 也不在处理 cap insets 变更时销毁旧 lattice，而是累到 lattice_pool_ 里，到
    // 本方法统一释放——此时 ArkUI 已经不可能再引用它们。弱符号先检查，
    // 避免在低版本系统（未提供 OH_Drawing_LatticeDestroy）上调空指针崩溃。
    if (&OH_Drawing_LatticeDestroy != nullptr) {
        for (auto *lattice : lattice_pool_) {
            if (lattice) {
                OH_Drawing_LatticeDestroy(lattice);
            }
        }
    }
    lattice_pool_.clear();
}

bool KRImageView::SetProp(const std::string &prop_key, const KRAnyValue &prop_value,
                          const KRRenderCallback event_call_back) {
    auto didHanded = false;
    if (kuikly::util::isEqual(prop_key, kPropNameSrc)) {
        didHanded = SetImageSrc(prop_value);
    } else if (kuikly::util::isEqual(prop_key, kPropNameResize)) {
        didHanded = SetResizeMode(prop_value);
    } else if (kuikly::util::isEqual(prop_key, kPropNameBlurRadius)) {
        didHanded = SetBlurRadius(prop_value);
    } else if (kuikly::util::isEqual(prop_key, kPropNameTintColor)) {
        didHanded = SetTintColor(prop_value);
    } else if (kuikly::util::isEqual(prop_key, kPropNameColorFilter)) {
        didHanded = SetColorFilter(prop_value);
    } else if (kuikly::util::isEqual(prop_key, kPropNameCapInsets)) {
        didHanded = SetCapInsets(prop_value);
    } else if (kuikly::util::isEqual(prop_key, kPropNameDotNineImage)) {
        didHanded = SetDotNineImage(prop_value);
    } else if (kuikly::util::isEqual(prop_key, kPropNameMaskLinearGradient)) {
        didHanded = SetMaskLinearGradient(prop_value);        
    } else if (kuikly::util::isEqual(prop_key, kEventNameLoadSuccess)) {
        didHanded = RegisterLoadSuccessCallback(event_call_back);
    } else if (kuikly::util::isEqual(prop_key, kEventNameLoadResolution)) {
        didHanded = RegisterLoadResolutionCallback(event_call_back);
    } else if (kuikly::util::isEqual(prop_key, kEventNameLoadFailure)) {
        didHanded = RegisterLoadFailureCallback(event_call_back);
    } else if (kuikly::util::isEqual(prop_key, kPropNameDragEnable)) {
        didHanded = SetDragEnable(prop_value);
    } else if (kuikly::util::isEqual(prop_key, kPropNameImageParams)) {
        didHanded = SetImageParams(prop_value);
    }
    return didHanded;
}

bool KRImageView::ResetProp(const std::string &prop_key) {
    auto didHanded = false;
    if (kuikly::util::isEqual(prop_key, kPropNameSrc)) {
        image_src_ = "";
        has_loaded_image_ = false;
        loaded_image_size_ = {};
        // src 复位：清空原图尺寸缓存与 source size 幂等门闸，让下一次 src
        // 到达时能重新捕获新原图尺寸并允许一次 SetArkUIImageSourceSize 下发。
        has_original_image_size_ = false;
        original_image_size_ = {};
        source_size_applied_ = false;
        kuikly::util::ResetArkUIImageSrc(GetNode());
        didHanded = true;
    } else if (kuikly::util::isEqual(prop_key, kPropNameResize)) {
        SetResizeMode(NewKRRenderValue(kResizeModeCover));
        didHanded = true;
    } else if (kuikly::util::isEqual(prop_key, kPropNameBlurRadius)) {
        kuikly::util::ResetArkUIImageBlurRadius(GetNode());
        didHanded = true;
    } else if (kuikly::util::isEqual(prop_key, kPropNameTintColor)) {
        kuikly::util::ResetArkUIImageTintColor(GetNode());
        didHanded = true;
    } else if (kuikly::util::isEqual(prop_key, kPropNameColorFilter)) {
        didHanded = ResetColorFilter();
    } else if (kuikly::util::isEqual(prop_key, kPropNameCapInsets)) {
        has_cap_insets_ = false;
        cap_insets_top_ = 0.f;
        cap_insets_left_ = 0.f;
        cap_insets_bottom_ = 0.f;
        cap_insets_right_ = 0.f;
        kuikly::util::ResetArkUIImageCapInsets(GetNode());
        didHanded = true;
    } else if (kuikly::util::isEqual(prop_key, kPropNameDotNineImage)) {
        this->is_dot_nine_image_ = false;
        didHanded = true;
    } else if (kuikly::util::isEqual(prop_key, kPropNameMaskLinearGradient)) {
        ResetMaskLinearGradientNode();
        kuikly::util::ResetArkUIImageBlendMode(GetNode());
        didHanded = true;
    } else if (kuikly::util::isEqual(prop_key, kEventNameLoadSuccess)) {
        didHanded = true;
        had_register_on_complete_event_ = false;
        load_success_callback_ = nullptr;
    } else if (kuikly::util::isEqual(prop_key, kEventNameLoadResolution)) {
        didHanded = true;
        had_register_on_complete_event_ = false;
        load_resolution_callback_ = nullptr;
    } else if (kuikly::util::isEqual(prop_key, kEventNameLoadFailure)) {
        didHanded = true;
        had_register_on_error_event_ = false;
        load_failure_callback_ = nullptr;
    } else if (kuikly::util::isEqual(prop_key, kPropNameImageParams)) {
        image_params_ = nullptr;
        capinset_image_scale_ = DEFAULT_CAPINST_IMAGE_SCALE;
        // __scale__ 变化 → source size 需要按新倍率重新下发，放开幂等门闸。
        source_size_applied_ = false;
        didHanded = true;
    } else {
        didHanded = IKRRenderViewExport::ResetProp(prop_key);
    }
    return didHanded;
}

void KRImageView::OnEvent(ArkUI_NodeEvent *event, const ArkUI_NodeEventType &event_type) {
    if (event_type == NODE_IMAGE_ON_COMPLETE) {
        FireOnImageCompleteEvent(event);
    } else if (event_type == NODE_IMAGE_ON_ERROR) {
        FireOnImageErrorEvent(event);
    }
}

void KRImageView::AdapterSetImageCallback(const void* context,
                                   const char *src,
                                   ArkUI_DrawableDescriptor *imageDescriptor,
                                   const char *new_src){
    KREnsureMainThread();
    KRViewContext ctx(context);
    if(auto render_view = KRRenderManager::GetInstance().GetRenderView(ctx.InstanceString())){
        if (std::shared_ptr<IKRRenderViewExport> view = render_view->GetView(ctx.Tag());
        auto image_view = std::dynamic_pointer_cast<KRImageView> (view)) {
            if(src != image_view->image_src_){
                KR_LOG_ERROR << "Image src mismatch, src in callback:" << src <<", current src of image view:"<<image_view->image_src_;
                return;
            }

            if (imageDescriptor) {
                kuikly::util::SetArkUIImageSrc(image_view->GetNode(), imageDescriptor);
            } else if (new_src) {
                image_view->LoadFromSrc(std::string(new_src));
            } else {
                KR_LOG_INFO << "Neither image descriptor nor new_src is returned";
            }
        }
    }
}

bool KRImageView::SetImageSrc(const KRAnyValue &value) {
    auto src = value->toString();
    if (image_src_ == src) {
        return true;
    }

    kuikly::util::ResetArkUIImageSrc(GetNode());
    image_src_ = src;
    has_loaded_image_ = false;
    loaded_image_size_ = {};
    // src 换了：原图快照失效，source size 幂等门闸放开，让新 src 加载完成
    // 后允许再下发一次 NODE_IMAGE_SOURCE_SIZE。
    has_original_image_size_ = false;
    original_image_size_ = {};
    source_size_applied_ = false;
    
    // 优先使用 V3 adapter（支持 imageParams）
    if (auto imageAdapterV3 = KRImageAdapterManager::GetInstance()->GetAdapterV3()) {
        KRViewContext ctx(GetInstanceId(), GetViewTag());
        
        // 将KRAnyValue 转换为KRAnyData
        KRAnyData imageParamsData = nullptr;
        KRAnyDataInternal imageParamsInternal;
        if (image_params_) {
            imageParamsInternal.anyValue = image_params_;
            imageParamsData = &imageParamsInternal;
        }
        if (imageAdapterV3((const void*)ctx.Context(), src.c_str(), imageParamsData, &KRImageView::AdapterSetImageCallback)) {
            return true;
        }
    }
    // 兼容 V2 adapter
    else if (auto imageAdapterV2 = KRImageAdapterManager::GetInstance()->GetAdapterV2()) {
        KRViewContext ctx(GetInstanceId(), GetViewTag());
        if (imageAdapterV2((const void*)ctx.Context(), src.c_str(), &KRImageView::AdapterSetImageCallback)) {
            return true;
        }
    }
    // 兼容 V1 adapter
    else if (auto imageAdapter = KRImageAdapterManager::GetInstance()->GetAdapter()) {
        ArkUI_DrawableDescriptor *imageDescriptor = nullptr;
        KRImageDataDeallocator deallocator = nullptr;
        char *imageSrc = imageAdapter(src.c_str(), &imageDescriptor, &deallocator);
        if (imageDescriptor) {
            kuikly::util::SetArkUIImageSrc(GetNode(), imageDescriptor);
            if (deallocator) {
                deallocator(imageDescriptor);
            }
        } else if (imageSrc) {
            LoadFromSrc(std::string(imageSrc));
            if (deallocator) {
                deallocator(imageSrc);
            }
        }
        return true;
    }

    LoadFromSrc(src);
    return true;
}

bool KRImageView::SetImageParams(const KRAnyValue &value) {
    const float prev_image_scale = capinset_image_scale_;
    if (!value) {
        image_params_ = nullptr;
        capinset_image_scale_ = DEFAULT_CAPINST_IMAGE_SCALE;
    } else {
        // 直接使用 toMap() 解析 JSON 字符串为 Map，并存储
        auto map = value->toMap();
        if (map.empty()) {
            image_params_ = nullptr;
            capinset_image_scale_ = DEFAULT_CAPINST_IMAGE_SCALE;
        } else {
            image_params_ = NewKRRenderValue(map);
            // 从 imageParams 中提取 __scale__（图片自身像素密度倍率，例如 @2x = 2）。
            // 存下来供 ApplyCapInsetsWithLattice 计算 NODE_IMAGE_SOURCE_SIZE 使用。
            // 缺失、非数值或 <= 0 时 fallback 到默认值（1.0，即 1 像素 = 1 vp）。
            auto it = map.find(kImageParamKeyScale);
            if (it != map.end() && it->second) {
                float s = it->second->toFloat();
                capinset_image_scale_ = s > 0.f ? s : DEFAULT_CAPINST_IMAGE_SCALE;
            } else {
                capinset_image_scale_ = DEFAULT_CAPINST_IMAGE_SCALE;
            }
        }
    }
    // __scale__ 变化会改变 source size 目标值，需要放开 source_size_applied_
    // 幂等门闸，让下一次 ApplyCapInsetsWithLattice 能按新倍率重新下发。
    // 不变则保持原状——避免误触发多余的重解码。
    if (capinset_image_scale_ != prev_image_scale) {
        source_size_applied_ = false;
    }
    return true;
}

bool KRImageView::SetResizeMode(const KRAnyValue &value) {
    auto resize = ARKUI_OBJECT_FIT_COVER;
    auto resize_mode = value->toString();
    if (kuikly::util::isEqual(resize_mode, kResizeModeCover)) {
        resize = ARKUI_OBJECT_FIT_COVER;
    } else if (kuikly::util::isEqual(resize_mode, kResizeModeContain)) {
        resize = ARKUI_OBJECT_FIT_CONTAIN;
    } else if (kuikly::util::isEqual(resize_mode, kResizeModeStretch)) {
        resize = ARKUI_OBJECT_FIT_FILL;
    }
    kuikly::util::SetArkUIIMageResizeMode(GetNode(), resize);
    return true;
}

bool KRImageView::SetDragEnable(const KRAnyValue &value) {
    kuikly::util::SetArkUIImageDraggable(GetNode(), value->toBool());
    return true;
}

bool KRImageView::SetBlurRadius(const KRAnyValue &value) {
    auto radius = value->toFloat();
    // 注： 该调参经验证是对齐安卓和iOS 高斯模糊效果
    kuikly::util::SetArkUIImageBlurRadius(GetNode(), 150.0 * (radius / 10.0));
    return true;
}

bool KRImageView::SetTintColor(const KRAnyValue &value) {
    auto valueStr = value->toString();
    if (valueStr.empty()) {
        kuikly::util::ResetArkUIImageTintColor(GetNode());
    } else {
        auto argb = kuikly::util::ToArgb(valueStr);
        kuikly::util::SetArkUIImageTintColor(GetNode(), argb);
    }
    return true;
}

bool KRImageView::SetColorFilter(const KRAnyValue &value) {
    std::string matrix_str = value->toString();
    if (matrix_str.empty()) {
        kuikly::util::ResetArkUIImageColorFilter(GetNode());
        return true;
    }
    std::vector<float> matrix;
    matrix.reserve(20);
    std::istringstream ss(matrix_str);
    std::string token;
    while (std::getline(ss, token, '|')) {
        try {
            matrix.push_back(std::stof(token));
        } catch (...) {
            matrix.push_back(0.f);
        }
    }
    kuikly::util::SetArkUIImageColorFilter(GetNode(), matrix);
    return true;
}

bool KRImageView::ResetColorFilter() {
    kuikly::util::ResetArkUIImageColorFilter(GetNode());
    return true;
}

static ArkUI_NodeHandle CreateNodeBackgroundImage(const std::string &cssBackgroundImage) {
    ArkUI_NodeHandle node = kuikly::util::GetNodeApi()->createNode(ARKUI_NODE_STACK);
    auto nodeAPI = kuikly::util::GetNodeApi();
    ArkUI_NumberValue sizeValue[] = {{.f32 = 1}};
    ArkUI_AttributeItem sizeItem = {sizeValue, sizeof(sizeValue) / sizeof(ArkUI_NumberValue)};
    nodeAPI->setAttribute(node, NODE_WIDTH_PERCENT, &sizeItem);
    nodeAPI->setAttribute(node, NODE_HEIGHT_PERCENT, &sizeItem);

    ArkUI_NumberValue blendValue[] = {{.i32 = ARKUI_BLEND_MODE_DST_IN}, {.i32 = BLEND_APPLY_TYPE_OFFSCREEN}};
    ArkUI_AttributeItem blendItem = {blendValue, sizeof(blendValue) / sizeof(ArkUI_NumberValue)};
    nodeAPI->setAttribute(node, NODE_BLEND_MODE, &blendItem);

    kuikly::util::UpdateNodeBackgroundImage(node, cssBackgroundImage);
    return node;
}

void KRImageView::ResetMaskLinearGradientNode() {
    if (mask_linear_gradient_node_) {
        auto nodeAPI = kuikly::util::GetNodeApi();
        nodeAPI->removeChild(GetNode(), mask_linear_gradient_node_);
        nodeAPI->disposeNode(mask_linear_gradient_node_);
        mask_linear_gradient_node_ = nullptr;
    }
}

bool KRImageView::SetMaskLinearGradient(const KRAnyValue &value) {
    auto valueStr = value->toString();
    if (valueStr.empty()) {
        return true;
    }
    auto nodeAPI = kuikly::util::GetNodeApi();
    ResetMaskLinearGradientNode();
    mask_linear_gradient_node_ =  CreateNodeBackgroundImage(valueStr);
    nodeAPI->addChild(GetNode(), mask_linear_gradient_node_);

    // 此处设置为了透出底部内容，否则透明处为黑色
    ArkUI_NumberValue numberValue[] = {{.i32 = ARKUI_BLEND_MODE_SRC_OVER}, {.i32 = BLEND_APPLY_TYPE_OFFSCREEN}};
    ArkUI_AttributeItem item = {numberValue, sizeof(numberValue) / sizeof(ArkUI_NumberValue)};
    nodeAPI->setAttribute(GetNode(), NODE_BLEND_MODE, &item);

    return true;
}

bool KRImageView::SetCapInsets(const KRAnyValue &value) {
    auto valueStr = value->toString();
    if (valueStr.empty()) {
        has_cap_insets_ = false;
        cap_insets_top_ = 0.f;
        cap_insets_left_ = 0.f;
        cap_insets_bottom_ = 0.f;
        cap_insets_right_ = 0.f;
        //kuikly::util::ResetArkUIImageCapInsets(GetNode());
        return true;
    }
    std::vector<std::string> items = kuikly::util::ConvertSplit(valueStr, " ");
    if (items.size() >= 4) {
        // 单位约定：SetProp 阶段拿到的 top/left/bottom/right 单位就是**图片 vp**
        //（与其他布局字段保持一致，属于"上层协议单位"）。这里**只做透传**，
        //  不在此处乘 dpi、也不乘任何魔法系数——历史实现里 top 乘 dpi、其余三边
        //  硬编码 * 3.25，除了让四边不一致导致视觉错位以外没有任何好处，因此本
        //  次一并去掉；把"vp -> 图片像素"的换算集中到 ApplyCapInsetsWithLattice
        //  里（下发 lattice 前统一做）。
        //
        // 只记录 capInsets 数据，不在此处直接下发到 ArkUI：
        //   * lattice 精确九宫格（API 24+）依赖图片的真实像素分辨率来把 vp 边距
        //     换算成整数分割线，SetProp 阶段图片一般还没加载完成，拿不到分辨率；
        //   * 老四值路径本身正是本次修复的痛点（rezisable 效果差），不再作为兜底
        //     使用——宁可短暂显示无 capInsets 的原图，也不要让用户先看到错误效果。
        //
        // 真正下发在 FireOnImageCompleteEvent 里拿到 loaded_image_size_ 之后执行；
        // 因此这里必须确保 NODE_IMAGE_ON_COMPLETE 事件已注册，否则永远拿不到分辨率。
        has_cap_insets_ = true;
        cap_insets_top_ = std::stof(items[0]);
        cap_insets_left_ = std::stof(items[1]);
        cap_insets_bottom_ = std::stof(items[2]);
        cap_insets_right_ = std::stof(items[3]);
        EnsureLoadCompleteEventRegistered();
        // 兜底：capInsets 在 src 之后到达、且 src 已经加载完成的场景（例如 base64
        // 命中缓存的同步路径），此时 has_loaded_image_ 已是 true，可以立即下发。
        if (has_loaded_image_) {
            ApplyCapInsetsWithLattice();
        }
    }
    return true;
}

bool KRImageView::SetDotNineImage(const KRAnyValue &value) {
    this->is_dot_nine_image_ = value->toBool();
    if (this->is_dot_nine_image_) {
        EnsureLoadCompleteEventRegistered();
    }
    return true;
}

void KRImageView::EnsureLoadCompleteEventRegistered() {
    if (!had_register_on_complete_event_) {
        RegisterEvent(NODE_IMAGE_ON_COMPLETE);
        had_register_on_complete_event_ = true;
    }
}

void KRImageView::ApplyCapInsetsWithLattice() {
    // 单位约定（三种坐标系，务必分清）：
    //   * GetFrame() 返回的 width/height 单位是 **vp**（屏幕坐标）；
    //   * original_image_size_ 单位是 **px**（图片自身原始像素，首帧捕获后不再变）；
    //   * cap_insets_* 单位是 **图片自身独立坐标**（上层协议数值，与屏幕 dpi 无关，
    //     和 iOS UIImage capInsets / Android .9png 语义一致）；
    //   * NODE_IMAGE_SOURCE_SIZE 官方文档虽写 vp，但 attribute 结构体实际按 .i32
    //     (整数 px) 读取（见 SetArkUIImageSourceSize 实现），因此这里直接传"重采样
    //     目标像素"即可。
    //
    // 循环防护（关键！）：SetArkUIImageSourceSize 会触发 ArkUI 重解码，重解码完成
    // 后又会回调 NODE_IMAGE_ON_COMPLETE 再次进到这里；若不加防护，且 img_px 计算
    // 时使用被覆盖后的 loaded_image_size_，就会形成尺寸雪崩式无限递归。两把锁：
    //   1) img_px 参数**始终**用 original_image_size_（首帧捕获，之后固定不变）
    //      → 每一轮算出的 source size 是常量，天然幂等。
    //   2) source_size_applied_ 幂等门闸 → 当前 src+__scale__ 生命周期内只允许
    //      下发一次 NODE_IMAGE_SOURCE_SIZE；后续再进来只重发 lattice 分割线，
    //      不再触发重解码，彻底断开回环。src 变、__scale__ 变时该门闸会被放开。
    //
    // 两条分支：
    //   (A) capinset_image_scale_ > 0（imageParams 中显式指定了 __scale__，例如 @2x = 2；
    //       默认值 DEFAULT_CAPINST_IMAGE_SCALE = 1 也满足该条件）：
    //       首次进入时下发 NODE_IMAGE_SOURCE_SIZE 让 ArkUI 重解码 pixmap，随后
    //       lattice 分割线建立在**重采样后 pixmap** 的坐标系里。source scale 直接
    //       由 __scale__ 决定，完全不依赖 view frame：
    //           source_scale = (1 / __scale__) * dpi = dpi / __scale__
    //           source_px    = original_img_px * source_scale
    //                        = (original_img_px / __scale__) * dpi
    //                        = 图片对应的 vp 尺寸 * dpi
    //       lattice 的 xDivs/yDivs 用同一个 source_scale 把 cap_insets_* 换算成
    //       "重采样 pixmap 像素"，与 image_width/height 同坐标系。
    //       第二次及以后进入（例如 setCapInsets 变化）只重发 lattice，跳过
    //       SetArkUIImageSourceSize，避免重解码回环。
    //   (B) 未指定 __scale__ 或必要尺寸缺失：**不下发 NODE_IMAGE_SOURCE_SIZE**，
    //       直接按原图像素坐标下发 lattice——一图片单位=一像素是默认解读，
    //       cap_insets_* 与 image_width/height 都用原图像素同坐标系。若原图尺寸
    //       也取不到，helper 内部会自动回退到老四值 RESIZABLE 路径。
    const auto &view_frame = GetFrame();
    const float view_w_vp = view_frame.width;
    const float view_h_vp = view_frame.height;
    // 原图尺寸优先取首帧快照 original_image_size_；快照未就位（罕见的兜底路径）
    // 才退回 loaded_image_size_——此时 source_size_applied_ 也一定还是 false，
    // 一次下发之后快照会在下一次 ON_COMPLETE 里被落下来。
    const KRSize &img_size = has_original_image_size_ ? original_image_size_ : loaded_image_size_;
    if (capinset_image_scale_ > 0.f && view_w_vp > 0.f && view_h_vp > 0.f &&
        img_size.width > 0.f && img_size.height > 0.f) {
        const double dpi = KRConfig::GetDpi();
        // NODE_IMAGE_SOURCE_SIZE 使用的 scale：
        //   imageParams 中显式指定了 __scale__（图片自身像素密度倍率，例如 @2x = 2）
        //   时，source size 不跟随 view frame 走 contain 缩放，而是直接按
        //   (1 / __scale__) * dpi 把"图片自身像素"折算成"屏幕重采样像素"：
        //       source_px = original_img_px * (1 / __scale__) * dpi
        //                 = (original_img_px / __scale__) * dpi
        //                 = 图片对应的 vp 尺寸 * dpi
        //   这样素材倍率与屏幕 dpi 严格对齐，不会被 view 尺寸拉伸/压缩解码。
        const float source_scale = static_cast<float>(dpi / capinset_image_scale_);
        const float source_w_px = img_size.width * source_scale;
        const float source_h_px = img_size.height * source_scale;
        // source size 只允许下发一次——见函数头部注释里的"循环防护"说明。
        // 首次下发后置位 source_size_applied_，之后的 ApplyCapInsetsWithLattice
        // 调用只走下面的 SetArkUIImageCapInsetsWithLattice 重发 lattice。
        if (!source_size_applied_) {
            kuikly::util::SetArkUIImageSourceSize(GetNode(), source_w_px, source_h_px);
            source_size_applied_ = true;
        }

        // lattice 分割线：cap_insets_*（图片自身坐标）* source_scale = 重采样
        // pixmap 像素。image_width/height 也必须是**重采样后**的像素尺寸，与
        // xDivs/yDivs 同坐标系；因此这里必须使用与 source size 完全一致的
        // source_scale。
        SetArkUIImageCapInsetsWithLattice(cap_insets_top_ * source_scale,
                                          cap_insets_left_ * source_scale,
                                          cap_insets_bottom_ * source_scale,
                                          cap_insets_right_ * source_scale,
                                          source_w_px,
                                          source_h_px);
        return;
    }
    // 未指定 __scale__ 或缺失必要尺寸：不下发 NODE_IMAGE_SOURCE_SIZE，直接按
    // 原图像素坐标走 lattice——cap_insets_* 数值即视为图片自身坐标
    //（一图片单位=一像素），image_width/height 用原图像素。
    // 若原图尺寸也取不到，helper 内部会自动回退到老四值 RESIZABLE 路径。
    SetArkUIImageCapInsetsWithLattice(cap_insets_top_,
                                      cap_insets_left_,
                                      cap_insets_bottom_,
                                      cap_insets_right_,
                                      img_size.width,
                                      img_size.height);
}

void KRImageView::SetArkUIImageCapInsetsWithLattice(float top, float left, float bottom, float right,
                                                    float image_width_px, float image_height_px) {
    // 委托 util 底层实现完成实际下发，并通过 out_lattice 接住创建出的
    // OH_Drawing_Lattice 指针；util 内部不再销毁。创建成功才 push 进池，
    // fallback 到老四值 RESIZABLE 路径时 out_lattice 会被 util 置为 nullptr。
    struct OH_Drawing_Lattice *lattice = nullptr;
    kuikly::util::SetArkUIImageCapInsetsWithLattice(GetNode(), top, left, bottom, right,
                                                   image_width_px, image_height_px, &lattice);
    if (lattice) {
        lattice_pool_.push_back(lattice);
    }
}

bool KRImageView::RegisterLoadSuccessCallback(const KRRenderCallback &event_callback) {
    load_success_callback_ = event_callback;
    EnsureLoadCompleteEventRegistered();
    if (load_success_callback_ && has_loaded_image_) {
        KRRenderValueMap map;
        map[kPropNameSrc] = NewKRRenderValue(image_src_);
        load_success_callback_(NewKRRenderValue(map));
    }
    return true;
}

bool KRImageView::RegisterLoadResolutionCallback(const KRRenderCallback &event_callback) {
    load_resolution_callback_ = event_callback;
    EnsureLoadCompleteEventRegistered();
    if (load_resolution_callback_ && has_loaded_image_) {
        KRRenderValueMap map;
        map[kParamKeyImageWidth] = NewKRRenderValue(loaded_image_size_.width);
        map[kParamKeyImageHeight] = NewKRRenderValue(loaded_image_size_.height);
        load_resolution_callback_(NewKRRenderValue(map));
    }
    return true;
}

bool KRImageView::RegisterLoadFailureCallback(const KRRenderCallback &event_callback) {
    load_failure_callback_ = event_callback;
    if (!had_register_on_error_event_) {
        RegisterEvent(NODE_IMAGE_ON_ERROR);
        had_register_on_error_event_ = true;
    }
    return true;
}

void KRImageView::FireOnImageErrorEvent(ArkUI_NodeEvent *event) {
    if (load_failure_callback_) {
        int32_t code = kuikly::util::GetImageLoadSuccessStatusCode(event);
        KRRenderValueMap map;
        map[kPropNameSrc] = NewKRRenderValue(image_src_);
        map[kEventNameLoadErrorCode] = NewKRRenderValue(code);
        load_failure_callback_(NewKRRenderValue(map));
    }
}

void KRImageView::FireOnImageCompleteEvent(ArkUI_NodeEvent *event) {
    if (!kuikly::util::IsImageLoadSuccessStatus(event)) {
        return;
    }

    loaded_image_size_ = kuikly::util::GetArkUINodeImagePicSize(event);
    has_loaded_image_ = true;
    // 首帧捕获**原图**像素尺寸快照：SetArkUIImageSourceSize 触发的重采样会让
    // 后续 ON_COMPLETE 里的 width/height 变成重采样后 pixmap 的尺寸，直接拿来
    // 参与 img_px * source_scale 会尺寸雪崩。所以只在第一次记录，之后无论
    // ArkUI 又回调多少次都不覆盖，直到 src 换了才清空。
    // loaded_image_size_ 依然每次都刷——它对外承担 load_resolution 语义，
    // 需要反映当前 pixmap 的真实尺寸。
    if (!has_original_image_size_) {
        original_image_size_ = loaded_image_size_;
        has_original_image_size_ = true;
    }

    if (this->is_dot_nine_image_) {
        // dotNine 场景保持原始老逻辑不动：图片中间 1 像素为拉伸区，把图片像素
        // /dpi 换算成 vp 后直接下发到 NODE_IMAGE_RESIZABLE（老四值路径）。这条路径
        // 长期以来工作稳定，本次 lattice 改造只针对普通 capInsets prop 场景，dotNine
        // 不参与——避免影响已经工作正常的代码。
        double dpi = KRConfig::GetDpi();
        float top = loaded_image_size_.height * 0.5 / dpi;
        float left = loaded_image_size_.width * 0.5 / dpi;
        float bottom = (loaded_image_size_.height * 0.5 - 1) / dpi;
        float right = (loaded_image_size_.width * 0.5 - 1) / dpi;
        kuikly::util::SetArkUIImageCapInsets(GetNode(), top, left, bottom, right);
    } else if (has_cap_insets_) {
        // 图片加载完成，首次拿到真实像素尺寸：先按 view frame 等比缩放下发
        // NODE_IMAGE_SOURCE_SIZE，再升级为 lattice 精确九宫格（API 24+）。低版本 /
        // 缺尺寸时 helper 内部自动回退老四值路径，视觉与之前一致。
        ApplyCapInsetsWithLattice();
    }

    if (load_success_callback_) {
        KRRenderValueMap map;
        map[kPropNameSrc] = NewKRRenderValue(image_src_);
        load_success_callback_(NewKRRenderValue(map));
    }

    if (load_resolution_callback_) {
        KRRenderValueMap map;
        map[kParamKeyImageWidth] = NewKRRenderValue(loaded_image_size_.width);
        map[kParamKeyImageHeight] = NewKRRenderValue(loaded_image_size_.height);
        load_resolution_callback_(NewKRRenderValue(map));
    }
}

std::shared_ptr<KRImageLoadOption> KRImageView::ToImageLoadOption(const std::string &src) {
    auto option = std::make_shared<KRImageLoadOption>();
    if (auto root_view = GetRootView().lock()) {
        option->native_resource_manager_ = root_view->GetNativeResourceManager();
    }
    option->src_ = src;
    option->image_params_ = image_params_;  // 设置图片加载参数（Map 类型）
    if (isBase64(src)) {
        option->src_type_ = KRImageSrcType::kImageSrcTypeBase64;
    } else if (isNetwork(src)) {
        option->src_type_ = KRImageSrcType::kImageSrcTypeNetwork;
    } else if (isFile(src)) {
        option->src_type_ = KRImageSrcType::kImageSrcTypeFile;
    } else if (isAssets(src)) {
        option->src_type_ = KRImageSrcType::kImageSrcTypeAssets;
    }

    auto image_adapter = KRRenderAdapterManager::GetInstance().GetImageAdapter();
    if (image_adapter != nullptr) {
        image_adapter->ConvertImageLoadOption(option);
    }
    return option;
}

void KRImageView::LoadFromSrc(const std::string image_src) {
    image_option_ = ToImageLoadOption(image_src);
    image_src_ = image_option_->src_;

    if (image_option_->src_type_ == KRImageSrcType::kImageSrcTypeBase64) {
        LoadFromBase64(image_option_);
    } else if (image_option_->src_type_ == KRImageSrcType::kImageSrcTypeFile) {
        LoadFromFile(image_option_);
    } else if (image_option_->src_type_ == KRImageSrcType::kImageSrcTypeNetwork) {
        LoadFromNetwork(image_option_);
    } else if (image_option_->src_type_ == KRImageSrcType::kImageSrcTypeResourceMedia) {
        LoadFromResourceMedia(image_option_);
    } else if (image_option_->src_type_ == KRImageSrcType::kImageSrcTypeAssets) {
        LoadFromAssets(image_option_);
    }

    if (!image_option_->tint_color_.empty()) {
        SetTintColor(NewKRRenderValue(image_option_->tint_color_));
    }
}

void KRImageView::LoadFromBase64(const std::shared_ptr<KRImageLoadOption> image_option) {
    if (image_src_.rfind("data:image_Md5_Pixelmap", 0) == 0) {
        if (auto root = GetRootView().lock()) {
            root->GetSnapshotManager()->SetCachedSnapshotToNode(GetNode(), image_src_);
        }
    } else {
        auto module_name = std::string(kMemoryCacheModuleName);
        auto memory_cache_module = std::dynamic_pointer_cast<KRMemoryCacheModule>(GetModule(module_name));
        if (memory_cache_module) {
            auto base64Str = memory_cache_module->Get(image_option->src_)->toString();
            if (!base64Str.empty()) {
                kuikly::util::SetArkUIImageSrc(GetNode(), base64Str);
            }
        }
    }
}

void KRImageView::LoadFromFile(const std::shared_ptr<KRImageLoadOption> image_option) {
    kuikly::util::SetArkUIImageSrc(GetNode(), image_option->src_);
}

void KRImageView::LoadFromNetwork(const std::shared_ptr<KRImageLoadOption> image_option) {
    kuikly::util::SetArkUIImageSrc(GetNode(), image_option->src_);
}

void KRImageView::LoadFromResourceMedia(const std::shared_ptr<KRImageLoadOption> image_option) {
    auto root_view = GetRootView().lock();
    if (!root_view) {
        return;
    }
    if (image_option->resource_drawable_) {
        kuikly::util::SetArkUIImageSrc(GetNode(), image_option->resource_drawable_);
        return;
    }

    auto resource_manager = root_view->GetNativeResourceManager();
    if (!resource_manager) {
        return;
    }

    ArkUI_DrawableDescriptor *drawable = nullptr;
    OH_ResourceManager_GetDrawableDescriptorByName(resource_manager, image_option->src_.c_str(), &drawable, 0, 0);
    if (!drawable) {
        return;
    }

    kuikly::util::SetArkUIImageSrc(GetNode(), drawable);
}

void KRImageView::LoadFromAssets(const std::shared_ptr<KRImageLoadOption> image_option) {
    const auto &rootView = GetRootView().lock();
    if (rootView) {
        const std::string &assetsDir = rootView->GetContext()->Config()->GetAssetsDir();
        if (!assetsDir.empty()) {
            std::string uri =
                KRURIHelper::GetInstance()->URIForResFile(image_src_.substr(KR_ASSET_PREFIX.size()), assetsDir);
            kuikly::util::SetArkUIImageSrc(GetNode(), uri);
            return;
        }
    }
}
