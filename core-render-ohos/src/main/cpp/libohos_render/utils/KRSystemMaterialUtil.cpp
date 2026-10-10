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

#include "libohos_render/utils/KRSystemMaterialUtil.h"

#include <cstdlib>
#include <cstring>
#include <string>

#include "libohos_render/utils/KRRenderLoger.h"
#include "libohos_render/utils/KRViewUtil.h"

namespace kuikly {
namespace util {

namespace {

// 从 "k=v,k=v" 形式中取值；未找到返回 false。
bool TakeValue(const std::string &text, const std::string &key, std::string &out_value) {
    size_t begin = 0;
    while (begin <= text.size()) {
        size_t comma = text.find(',', begin);
        if (comma == std::string::npos) {
            comma = text.size();
        }
        std::string pair = text.substr(begin, comma - begin);
        size_t eq = pair.find('=');
        if (eq != std::string::npos && pair.substr(0, eq) == key) {
            out_value = pair.substr(eq + 1);
            return true;
        }
        if (comma == text.size()) {
            break;
        }
        begin = comma + 1;
    }
    return false;
}

uint32_t ParseColor(const std::string &text) {
    // Kotlin 侧 Color.toString() 输出无符号十进制（如 4294967295）。
    return static_cast<uint32_t>(strtoull(text.c_str(), nullptr, 10));
}

}  // namespace

bool ParseSystemMaterialOptions(const std::string &value, SystemMaterialOptions &options) {
    if (value == "off" || value.empty()) {
        return false;
    }
    std::string raw;
    if (TakeValue(value, "style", raw)) {
        options.style = atoi(raw.c_str());
    }
    if (TakeValue(value, "interactive", raw)) {
        options.has_interactive = true;
        options.interactive = (raw == "1");
    }
    if (TakeValue(value, "applyShadow", raw)) {
        options.has_apply_shadow = true;
        options.apply_shadow = (raw == "1");
    }
    if (TakeValue(value, "lightColor", raw)) {
        options.has_light_color = true;
        options.light_color = ParseColor(raw);
    }
    if (TakeValue(value, "materialColor", raw)) {
        options.has_material_color = true;
        options.material_color = ParseColor(raw);
    }
    return true;
}

bool IsSystemMaterialAvailable() {
#if KUIKLY_OHOS_SYSTEM_MATERIAL_AVAILABLE
    if (OH_ArkUI_NativeModule_GetSystemMaterialSupported == nullptr) {
        return false;
    }
    return OH_ArkUI_NativeModule_GetSystemMaterialSupported();
#else
    return false;
#endif
}

int GetSystemMaterialLevel() {
#if KUIKLY_OHOS_SYSTEM_MATERIAL_AVAILABLE
    if (OH_ArkUI_NativeModule_GetGlobalMaterialLevel == nullptr) {
        return -1;
    }
    return static_cast<int>(OH_ArkUI_NativeModule_GetGlobalMaterialLevel());
#else
    return -1;
#endif
}

void *CreateSystemMaterial(const SystemMaterialOptions &options) {
#if KUIKLY_OHOS_SYSTEM_MATERIAL_AVAILABLE
    if (!IsSystemMaterialAvailable()) {
        KR_LOG_INFO << "ImmLightTest: device not support system material, level=" << GetSystemMaterialLevel();
        return nullptr;
    }
    if (OH_ArkUI_NativeModule_ImmersiveMaterial_Create == nullptr) {
        return nullptr;
    }
    ArkUI_ImmersiveStyle style = ARKUI_IMMERSIVE_STYLE_REGULAR;
    switch (options.style) {
        case 0:
            style = ARKUI_IMMERSIVE_STYLE_ULTRA_THIN;
            break;
        case 1:
            style = ARKUI_IMMERSIVE_STYLE_THIN;
            break;
        case 3:
            style = ARKUI_IMMERSIVE_STYLE_THICK;
            break;
        case 4:
            style = ARKUI_IMMERSIVE_STYLE_ULTRA_THICK;
            break;
        default:
            style = ARKUI_IMMERSIVE_STYLE_REGULAR;
            break;
    }
    ArkUI_ImmersiveMaterialHandle material = OH_ArkUI_NativeModule_ImmersiveMaterial_Create(style);
    if (material == nullptr) {
        KR_LOG_ERROR << "ImmLightTest: ImmersiveMaterial_Create failed";
        return nullptr;
    }
    if (options.has_interactive && OH_ArkUI_NativeModule_ImmersiveMaterial_SetInteractive != nullptr) {
        OH_ArkUI_NativeModule_ImmersiveMaterial_SetInteractive(material, options.interactive);
    }
    if (options.has_apply_shadow && OH_ArkUI_NativeModule_ImmersiveMaterial_SetApplyShadow != nullptr) {
        OH_ArkUI_NativeModule_ImmersiveMaterial_SetApplyShadow(material, options.apply_shadow);
    }
    if (options.has_material_color && OH_ArkUI_NativeModule_ImmersiveMaterial_SetMaterialColor != nullptr) {
        OH_ArkUI_NativeModule_ImmersiveMaterial_SetMaterialColor(material, options.material_color);
    }
    if (options.has_light_color && OH_ArkUI_NativeModule_LightEffectOptions_Create != nullptr &&
        OH_ArkUI_NativeModule_ImmersiveMaterial_SetLightEffect != nullptr) {
        ArkUI_LightEffectOptionsHandle light_options = OH_ArkUI_NativeModule_LightEffectOptions_Create();
        if (light_options != nullptr) {
            if (OH_ArkUI_NativeModule_LightEffectOptions_SetColor != nullptr) {
                OH_ArkUI_NativeModule_LightEffectOptions_SetColor(light_options, options.light_color);
            }
            OH_ArkUI_NativeModule_ImmersiveMaterial_SetLightEffect(material, light_options);
            if (OH_ArkUI_NativeModule_LightEffectOptions_Destroy != nullptr) {
                OH_ArkUI_NativeModule_LightEffectOptions_Destroy(light_options);
            }
        }
    }
    KR_LOG_INFO << "ImmLightTest: create ok, style=" << options.style
                << ", level=" << GetSystemMaterialLevel() << ", v2";
    return material;
#else
    return nullptr;
#endif
}

void DestroySystemMaterial(void *material) {
#if KUIKLY_OHOS_SYSTEM_MATERIAL_AVAILABLE
    if (material == nullptr || OH_ArkUI_NativeModule_ImmersiveMaterial_Destroy == nullptr) {
        return;
    }
    OH_ArkUI_NativeModule_ImmersiveMaterial_Destroy(
        reinterpret_cast<ArkUI_ImmersiveMaterialHandle>(material));
#else
    (void)material;
#endif
}

int32_t ApplySystemMaterialToNode(ArkUI_NodeHandle node, void *material) {
#if KUIKLY_OHOS_SYSTEM_MATERIAL_AVAILABLE
    if (node == nullptr || material == nullptr) {
        return -1;
    }
    ArkUI_AttributeItem item = {.object = material};
    int32_t ret = GetNodeApi()->setAttribute(node, NODE_SYSTEM_MATERIAL, &item);
    KR_LOG_INFO << "ImmLightTest: setAttribute NODE_SYSTEM_MATERIAL ret=" << ret;
    return ret;
#else
    (void)node;
    (void)material;
    return -1;
#endif
}

int32_t ResetSystemMaterialOfNode(ArkUI_NodeHandle node) {
#if KUIKLY_OHOS_SYSTEM_MATERIAL_AVAILABLE
    if (node == nullptr) {
        return -1;
    }
    return GetNodeApi()->resetAttribute(node, NODE_SYSTEM_MATERIAL);
#else
    (void)node;
    return -1;
#endif
}

bool IsDialogMaterialApiAvailable() {
#if KUIKLY_OHOS_SYSTEM_MATERIAL_AVAILABLE
    return OH_ArkUI_NativeModule_CustomDialog_SetSystemMaterial != nullptr;
#else
    return false;
#endif
}

int32_t ApplySystemMaterialToDialog(ArkUI_NativeDialogHandle dialog, void *material) {
#if KUIKLY_OHOS_SYSTEM_MATERIAL_AVAILABLE
    if (OH_ArkUI_NativeModule_CustomDialog_SetSystemMaterial == nullptr) {
        KR_LOG_ERROR << "ImmLightTest: dialog api symbol unresolved (CustomDialog_SetSystemMaterial)";
        return -1;
    }
    if (dialog == nullptr) {
        KR_LOG_ERROR << "ImmLightTest: dialog handle is null";
        return -1;
    }
    if (material == nullptr) {
        KR_LOG_ERROR << "ImmLightTest: dialog material is null";
        return -1;
    }
    int32_t ret = OH_ArkUI_NativeModule_CustomDialog_SetSystemMaterial(
        dialog, reinterpret_cast<ArkUI_ImmersiveMaterialHandle>(material));
    KR_LOG_INFO << "ImmLightTest: CustomDialog_SetSystemMaterial ret=" << ret;
    return ret;
#else
    KR_LOG_ERROR << "ImmLightTest: built without native_material.h (SDK < 26)";
    (void)dialog;
    (void)material;
    return -1;
#endif
}

}  // namespace util
}  // namespace kuikly
