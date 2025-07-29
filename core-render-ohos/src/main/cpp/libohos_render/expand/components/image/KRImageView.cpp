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

#include <resourcemanager/ohresmgr.h>
#include <string_view>
#include "libohos_render/expand/components/image/KRImageAdapterManager.h"
#include "libohos_render/expand/modules/cache/KRMemoryCacheModule.h"
#include "libohos_render/manager/KRSnapshotManager.h"
#include "libohos_render/utils/KRURIHelper.h"
#include "libohos_render/utils/KRStringUtil.h"

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
constexpr char kPropNameCapInsets[] = "capInsets";
constexpr char kPropNameDotNineImage[] = "dotNineImage";

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
    return true;
}

void KRImageView::OnDestroy() {
    ResetMaskLinearGradientNode();
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
    }
    return didHanded;
}

bool KRImageView::ResetProp(const std::string &prop_key) {
    auto didHanded = false;
    if (kuikly::util::isEqual(prop_key, kPropNameSrc)) {
        image_src_ = "";
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
    } else if (kuikly::util::isEqual(prop_key, kPropNameCapInsets)) {
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

bool KRImageView::SetImageSrc(const KRAnyValue &value) {
    auto src = value->toString();
    if (image_src_ == src) {
        return true;
    }

    auto imageAdapter = KRImageAdapterManager::GetInstance()->GetAdapter();
    if (imageAdapter) {
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
        kuikly::util::ResetArkUIImageCapInsets(GetNode());
        return true;
    }
    std::vector<std::string> items = kuikly::util::ConvertSplit(valueStr, " ");
    if (items.size() >= 4) {
        double dpi = KRConfig::GetDpi();
        float top = std::stof(items[0]) / dpi;
        float left = std::stof(items[1]) / dpi;
        float bottom = std::stof(items[2]) / dpi;
        float right = std::stof(items[3]) / dpi;
        kuikly::util::SetArkUIImageCapInsets(GetNode(), top, left, bottom, right);
    }
    return true;
}

bool KRImageView::SetDotNineImage(const KRAnyValue &value) {
    this->is_dot_nine_image_ = value->toBool();
    if (this->is_dot_nine_image_) {
        RegisterLoadSuccessCallback(load_success_callback_);
    }
    return true;
}

bool KRImageView::RegisterLoadSuccessCallback(const KRRenderCallback &event_callback) {
    load_success_callback_ = event_callback;
    if (!had_register_on_complete_event_) {
        RegisterEvent(NODE_IMAGE_ON_COMPLETE);
        had_register_on_complete_event_ = true;
    }
    return true;
}

bool KRImageView::RegisterLoadResolutionCallback(const KRRenderCallback &event_callback) {
    load_resolution_callback_ = event_callback;
    if (!had_register_on_complete_event_) {
        RegisterEvent(NODE_IMAGE_ON_COMPLETE);
        had_register_on_complete_event_ = true;
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
    
    
    if (this->is_dot_nine_image_) {
        auto image_size = kuikly::util::GetArkUINodeImagePicSize(event);
        double dpi = KRConfig::GetDpi();
        float top = image_size.height * 0.5 / dpi;
        float left = image_size.width * 0.5 / dpi;
        float bottom = (image_size.height * 0.5 - 1) / dpi;
        float right = (image_size.width * 0.5 - 1) / dpi;
        kuikly::util::SetArkUIImageCapInsets(GetNode(), top, left, bottom, right);
    }

    if (load_success_callback_) {
        KRRenderValueMap map;
        map[kPropNameSrc] = NewKRRenderValue(image_src_);
        load_success_callback_(NewKRRenderValue(map));
    }

    if (load_resolution_callback_) {
        auto image_size = kuikly::util::GetArkUINodeImagePicSize(event);
        KRRenderValueMap map;
        map[kParamKeyImageWidth] = NewKRRenderValue(image_size.width);
        map[kParamKeyImageHeight] = NewKRRenderValue(image_size.height);
        load_resolution_callback_(NewKRRenderValue(map));
    }
}

std::shared_ptr<KRImageLoadOption> KRImageView::ToImageLoadOption(const std::string &src) {
    auto option = std::make_shared<KRImageLoadOption>();
    if (auto root_view = GetRootView().lock()) {
        option->native_resource_manager_ = root_view->GetNativeResourceManager();
    }
    option->src_ = src;
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
