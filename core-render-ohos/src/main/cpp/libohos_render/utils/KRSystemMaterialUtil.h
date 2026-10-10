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

#ifndef CORE_RENDER_OHOS_KRSYSTEMMATERIALUTIL_H
#define CORE_RENDER_OHOS_KRSYSTEMMATERIALUTIL_H

#include <arkui/native_dialog.h>
#include <arkui/native_node.h>
#include <arkui/native_type.h>
#include <cstdint>
#include <string>

// 编译期门控：<arkui/native_material.h> 是 API 26 才提供的头。SDK header < 26 时
// 本文件主体被整体裁剪，libkuikly.so 仍可在老 SDK 上编译通过。
#ifndef KUIKLY_OHOS_SYSTEM_MATERIAL_AVAILABLE
#if defined(__has_include)
#if __has_include(<arkui/native_material.h>)
#define KUIKLY_OHOS_SYSTEM_MATERIAL_AVAILABLE 1
#else
#define KUIKLY_OHOS_SYSTEM_MATERIAL_AVAILABLE 0
#endif
#else
#define KUIKLY_OHOS_SYSTEM_MATERIAL_AVAILABLE 0
#endif
#endif

#if KUIKLY_OHOS_SYSTEM_MATERIAL_AVAILABLE
#include <arkui/native_material.h>

// ============================================================================
// 弱符号重声明块（与 KRTextEditorCommon.h 同一策略）
//
// SDK header 里这些 API 26 入口以强符号声明。.so 链接后符号进入 .dynsym，运行在
// API < 26 的系统上时 dlopen 重定位会因符号缺失整体失败。此处重声明同名同签名
// 版本并附加 __attribute__((weak))，未解析时取 nullptr，由 IsSystemMaterialAvailable()
// 判空后走降级路径。
//
// 随 SDK 升级需与 native_material.h / native_dialog.h 复核签名一致性。
// ============================================================================
extern "C" {
bool OH_ArkUI_NativeModule_GetSystemMaterialSupported() __attribute__((weak));
ArkUI_MaterialLevel OH_ArkUI_NativeModule_GetGlobalMaterialLevel() __attribute__((weak));
ArkUI_ImmersiveMaterialHandle OH_ArkUI_NativeModule_ImmersiveMaterial_Create(ArkUI_ImmersiveStyle style)
    __attribute__((weak));
void OH_ArkUI_NativeModule_ImmersiveMaterial_Destroy(ArkUI_ImmersiveMaterialHandle material) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_NativeModule_ImmersiveMaterial_SetStyle(ArkUI_ImmersiveMaterialHandle material,
                                                                ArkUI_ImmersiveStyle style) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_NativeModule_ImmersiveMaterial_SetMaterialColor(ArkUI_ImmersiveMaterialHandle material,
                                                                        uint32_t color) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_NativeModule_ImmersiveMaterial_SetApplyShadow(ArkUI_ImmersiveMaterialHandle material,
                                                                      bool applyShadow) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_NativeModule_ImmersiveMaterial_SetInteractive(ArkUI_ImmersiveMaterialHandle material,
                                                                      bool interactive) __attribute__((weak));
ArkUI_LightEffectOptionsHandle OH_ArkUI_NativeModule_LightEffectOptions_Create() __attribute__((weak));
void OH_ArkUI_NativeModule_LightEffectOptions_Destroy(ArkUI_LightEffectOptionsHandle options) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_NativeModule_LightEffectOptions_SetColor(ArkUI_LightEffectOptionsHandle options,
                                                                 uint32_t color) __attribute__((weak));
ArkUI_ErrorCode OH_ArkUI_NativeModule_ImmersiveMaterial_SetLightEffect(
    ArkUI_ImmersiveMaterialHandle material, const ArkUI_LightEffectOptionsHandle options) __attribute__((weak));
int32_t OH_ArkUI_NativeModule_CustomDialog_SetSystemMaterial(ArkUI_NativeDialogHandle handle,
                                                            ArkUI_ImmersiveMaterialHandle material)
    __attribute__((weak));
}
#endif  // KUIKLY_OHOS_SYSTEM_MATERIAL_AVAILABLE

namespace kuikly {
namespace util {

// Kotlin 侧下发的属性键，与 Attr.StyleConst.SYSTEM_MATERIAL_OHOS 保持一致。
constexpr char kPropNameSystemMaterialOhos[] = "system-material-ohos";
// Modal 是否启用 customStyle（默认 true）。官方明确 CustomDialog 背板自定义
// （customStyle=true）时不支持沉浸材质，此键用于 A/B 验证该约束是否同样作用于
// NativeDialog。与 Attr.StyleConst.MODAL_CUSTOM_STYLE_OHOS 保持一致。
constexpr char kPropNameModalCustomStyleOhos[] = "modal-custom-style-ohos";
// Modal 内容是否相对屏幕居中（默认 false，保持 Kuikly 原有的 TOP_START 对齐）。
// 打开后 dialog 会随内容尺寸居中，便于观察沉浸材质在彩色背景上的表现。
constexpr char kPropNameModalAlignCenterOhos[] = "modal-align-center-ohos";

/**
 * 沉浸式系统材质参数。未显式下发的字段保持 hasXxx=false，不调用对应 setter，
 * 让系统/组件用自己的默认值。
 */
struct SystemMaterialOptions {
    int style = 2;  // ArkUI_ImmersiveStyle：0=ULTRA_THIN ... 4=ULTRA_THICK
    bool has_interactive = false;
    bool interactive = false;
    bool has_apply_shadow = false;
    bool apply_shadow = true;
    bool has_light_color = false;
    uint32_t light_color = 0xFFFFFFFF;
    bool has_material_color = false;
    uint32_t material_color = 0;
};

/**
 * 解析 Kotlin 侧下发的材质描述串。
 * 格式："style=2,interactive=1,applyShadow=1,lightColor=4294967295,materialColor=0"
 * 特殊值："off" 表示不设置材质。
 * @return true 解析成功且需要设置材质；false 表示无需设置（off 或格式非法）。
 */
bool ParseSystemMaterialOptions(const std::string &value, SystemMaterialOptions &options);

/**
 * 设备是否支持系统材质（API 26 弱符号已解析 + OH_ArkUI_NativeModule_GetSystemMaterialSupported 为真）。
 */
bool IsSystemMaterialAvailable();

/**
 * 设备算力档位：0=EXQUISITE 1=GENTLE 2=SMOOTH，不可用返回 -1。
 */
int GetSystemMaterialLevel();

/**
 * 创建沉浸式材质对象，返回 ArkUI_ImmersiveMaterialHandle（void*），失败返回 nullptr。
 * 调用方负责在节点/弹窗销毁前调用 DestroySystemMaterial。
 */
void *CreateSystemMaterial(const SystemMaterialOptions &options);

void DestroySystemMaterial(void *material);

/**
 * 将材质应用到普通节点：setAttribute(NODE_SYSTEM_MATERIAL)。
 * @return ArkUI 错误码；材质不可用时返回 -1。
 */
int32_t ApplySystemMaterialToNode(ArkUI_NodeHandle node, void *material);

/**
 * 清除节点上的材质：resetAttribute(NODE_SYSTEM_MATERIAL)。
 */
int32_t ResetSystemMaterialOfNode(ArkUI_NodeHandle node);

/**
 * 将材质应用到 NativeDialog（弹窗类接口，官方白名单内可全页面生效）。
 * material 的生命周期由调用方持有，弹窗销毁后再 DestroySystemMaterial。
 * 各种失败原因都会打日志，便于定位是"符号未解析"还是"调用被拒"。
 */
int32_t ApplySystemMaterialToDialog(ArkUI_NativeDialogHandle dialog, void *material);

/**
 * CustomDialog 材质专用接口在当前进程是否可用（弱符号是否已解析）。
 */
bool IsDialogMaterialApiAvailable();

}  // namespace util
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_KRSYSTEMMATERIALUTIL_H
