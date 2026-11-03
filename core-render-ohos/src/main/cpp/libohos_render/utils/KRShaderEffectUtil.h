/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef CORE_RENDER_OHOS_KRSHADEREFFECTUTIL_H
#define CORE_RENDER_OHOS_KRSHADEREFFECTUTIL_H

#include <deviceinfo.h>
#include <cstddef>
#include <native_drawing/drawing_shader_effect.h>
#include <native_drawing/drawing_types.h>

namespace kuikly {
namespace util {

/**
 * OH_Drawing_ShaderEffectDestroy 的可用门槛。
 * 低于此版本的系统上调用会在绘制阶段崩溃，见下方 KRSafeDestroyShaderEffect 的说明。
 */
constexpr int SHADER_EFFECT_DESTROY_API_LEVEL = 19;

/**
 * 安全释放 OH_Drawing_ShaderEffect。
 *
 * OH_Drawing_ShaderEffectDestroy 有已知问题：
 * 1. 在 api 13、17 的设备上会在绘制阶段崩溃（
 *    /system/lib64/lib2d_graphics.z.so
 *    OHOS::Rosen::Drawing::ColorFilter::Serialize() const+8）
 * 2. api 19 起正常
 * 因此在低版本上保持 noop，代价是这部分 shader effect 会被泄漏，
 * 但比崩溃可接受。
 *
 * 注意：调用方应保证 shaderEffect 已被 brush/pen 通过
 * OH_Drawing_BrushSetShaderEffect / OH_Drawing_PenSetShaderEffect 接管
 * （drawing 内部会持有其引用），之后再调用本函数交出所有权。
 */
inline void KRSafeDestroyShaderEffect(OH_Drawing_ShaderEffect *shaderEffect) {
    if (shaderEffect == nullptr) {
        return;
    }
    if (OH_GetSdkApiVersion() >= SHADER_EFFECT_DESTROY_API_LEVEL) {
        OH_Drawing_ShaderEffectDestroy(shaderEffect);
    }
}

}  // namespace util
}  // namespace kuikly

#endif  // CORE_RENDER_OHOS_KRSHADEREFFECTUTIL_H
