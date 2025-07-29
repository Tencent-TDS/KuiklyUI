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

#ifndef CORE_RENDER_OHOS_KRIMAGELOADOPTION_H
#define CORE_RENDER_OHOS_KRIMAGELOADOPTION_H

#include <arkui/drawable_descriptor.h>
#include <rawfile/raw_file_manager.h>
#include <resourcemanager/ohresmgr.h>
#include <string>

enum class KRImageSrcType {
    kImageSrcTypeUnknown = 0,
    kImageSrcTypeNetwork = 1,
    kImageSrcTypeFile = 2,
    kImageSrcTypeBase64 = 3,
    kImageSrcTypeResourceMedia = 4,
    kImageSrcTypeAssets = 5,
};

class KRImageLoadOption {
 public:
    KRImageLoadOption()
        : src_type_(KRImageSrcType::kImageSrcTypeUnknown), src_(""), tint_color_(""), resource_drawable_(nullptr),
          native_resource_manager_(nullptr) {}

    ArkUI_DrawableDescriptor *GetNativeMediaResourceDrawable(const std::string &media_resource_name) {
        if (!native_resource_manager_) {
            return nullptr;
        }

        ArkUI_DrawableDescriptor *drawable = nullptr;
        OH_ResourceManager_GetDrawableDescriptorByName(native_resource_manager_, media_resource_name.c_str(), &drawable,
                                                       0, 0);
        if (!drawable) {
            return nullptr;
        } else {
            return drawable;
        }
    }

 public:
    KRImageSrcType src_type_;
    std::string src_;
    std::string tint_color_;
    ArkUI_DrawableDescriptor *resource_drawable_;
    NativeResourceManager *native_resource_manager_;
};

#endif  // CORE_RENDER_OHOS_KRIMAGELOADOPTION_H
