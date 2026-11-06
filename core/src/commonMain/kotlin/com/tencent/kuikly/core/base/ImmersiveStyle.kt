/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of the KuiklyUI license;
 * you may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.core.base

/**
 * 鸿蒙沉浸光感材质厚薄档位，取值与官方 `uiMaterial.ImmersiveStyle` 一一对应。
 *
 * 供 [Attr.immersiveMaterial] 的 `style` 参数使用（跨端侧不直接依赖 ohos 类型，
 * 以常量对齐官方枚举命名）：
 *
 * ```
 * immersiveMaterial(style = ImmersiveStyle.REGULAR)
 * ```
 */
object ImmersiveStyle {
    /** 极薄：极强透明感 */
    const val ULTRA_THIN = 0

    /** 薄：强透明感 */
    const val THIN = 1

    /** 标准：常规厚度 */
    const val REGULAR = 2

    /** 厚 */
    const val THICK = 3

    /** 极厚 */
    const val ULTRA_THICK = 4
}
