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

package com.tencent.kuikly.core.base.attr

/**
 * 4x5 颜色变换矩阵，用于在原生渲染层修改图片颜色。
 *
 * 矩阵以行优先顺序存储为 20 个浮点数：
 * ```
 * [ R' ]   [ a  b  c  d  e ] [ R ]
 * [ G' ] = [ f  g  h  i  j ] [ G ]
 * [ B' ]   [ k  l  m  n  o ] [ B ]
 * [ A' ]   [ p  q  r  s  t ] [ A ]
 * ```
 * 其中第 5 列 (e, j, o, t) 为加性偏移（0–255 范围）。
 *
 * ## 平台支持
 * - Android: 支持
 * - HarmonyOS: 支持
 * - iOS: 暂不支持
 *
 * ## 使用示例
 *
 * Core DSL:
 * ```kotlin
 * Image {
 *     attr {
 *         src("https://example.com/image.png")
 *         // 使用预设灰度矩阵
 *         colorFilter(ColorMatrix.rec709Gray)
 *     }
 * }
 * ```
 *
 * Compose DSL:
 * ```kotlin
 * Image(
 *     painter = rememberAsyncImagePainter("https://example.com/image.png"),
 *     contentDescription = null,
 *     colorFilter = ColorFilter.colorMatrix(ColorMatrix.rec709Gray),
 * )
 * ```
 *
 * 自定义矩阵:
 * ```kotlin
 * val sepia = ColorMatrix(floatArrayOf(
 *     0.393f, 0.769f, 0.189f, 0f, 0f,
 *     0.349f, 0.686f, 0.168f, 0f, 0f,
 *     0.272f, 0.534f, 0.131f, 0f, 0f,
 *     0f,     0f,     0f,     1f, 0f,
 * ))
 * ```
 *
 * @param values 长度为 20 的浮点数组，长度不为 20 时抛出 [IllegalArgumentException]
 * @see ColorMatrixConfig
 * @see IImageAttr.colorFilter
 */
class ColorMatrix(val values: FloatArray) {

    init {
        require(values.size == 20) { "ColorMatrix requires exactly 20 values, got ${values.size}" }
    }

    /**
     * 序列化为原生 `colorFilter` 属性所需的 `|` 分隔字符串格式。
     */
    fun toColorMatrixString(): String = values.joinToString("|")

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ColorMatrix) return false
        return values.contentEquals(other.values)
    }

    override fun hashCode(): Int = values.contentHashCode()

    override fun toString(): String = "ColorMatrix(${toColorMatrixString()})"

    companion object {
        /**
         * 灰度矩阵，使用 ITU-R BT.709 亮度系数。
         *
         * R' = G' = B' = 0.2126·R + 0.7152·G + 0.0722·B
         */
        val rec709Gray: ColorMatrix = ColorMatrix(
            floatArrayOf(
                0.2126f, 0.7152f, 0.0722f, 0f, 0f,
                0.2126f, 0.7152f, 0.0722f, 0f, 0f,
                0.2126f, 0.7152f, 0.0722f, 0f, 0f,
                0f,      0f,      0f,      1f, 0f
            )
        )

        /**
         * 单位矩阵 — 不做任何颜色变换。
         */
        val identity: ColorMatrix = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        /**
         * 从 `|` 分隔的字符串（20 个浮点数）创建 [ColorMatrix]。
         * 格式不正确时返回 `null`。
         */
        fun fromString(matrix: String): ColorMatrix? {
            val parts = matrix.split("|")
            if (parts.size != 20) return null
            val values = FloatArray(20)
            for (i in 0 until 20) {
                values[i] = parts[i].toFloatOrNull() ?: return null
            }
            return ColorMatrix(values)
        }
    }
}

/**
 * 颜色矩阵配置，通过语义化参数（饱和度、亮度、对比度、透明度）生成 [ColorMatrix]。
 *
 * 相比直接构造 [ColorMatrix]，[ColorMatrixConfig] 提供了更直观的参数化方式，
 * 适合不需要精确控制每个矩阵元素的场景。
 *
 * ## 平台支持
 * - Android: 支持
 * - HarmonyOS: 支持
 * - iOS: 暂不支持
 *
 * ## 使用示例
 * ```kotlin
 * // 降低饱和度 + 提高亮度
 * val config = ColorMatrixConfig(saturation = 0.5f, brightness = 30f)
 * Image {
 *     attr {
 *         src("https://example.com/image.png")
 *         colorFilter(config.toColorMatrix())
 *     }
 * }
 * ```
 *
 * @param saturation 饱和度，1.0 为原始值，0.0 为完全灰度，大于 1.0 为过饱和
 * @param brightness 亮度偏移，范围 -255 ~ 255，0 为不变
 * @param contrast 对比度，1.0 为原始值，小于 1.0 降低对比度，大于 1.0 增强对比度
 * @param alpha 透明度，1.0 为完全不透明，0.0 为完全透明
 * @see ColorMatrix
 * @see toColorMatrix
 */
data class ColorMatrixConfig(
    val saturation: Float = 1f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val alpha: Float = 1f
)

/**
 * 将 [ColorMatrixConfig] 转换为 [ColorMatrix]。
 *
 * 内部使用 ITU-R BT.709 亮度系数计算饱和度变换，
 * 以 127.5 为中心计算对比度缩放。
 *
 * @receiver 颜色矩阵配置
 * @return 对应的 4×5 颜色变换矩阵
 */
fun ColorMatrixConfig.toColorMatrix(): ColorMatrix {
    val lumR = 0.2126f
    val lumG = 0.7152f
    val lumB = 0.0722f
    val s = saturation
    val sr = (1f - s) * lumR
    val sg = (1f - s) * lumG
    val sb = (1f - s) * lumB

    val c = contrast
    val contrastOffset = (1f - c) * 127.5f

    val offsetR = contrastOffset + brightness
    val offsetG = contrastOffset + brightness
    val offsetB = contrastOffset + brightness

    return ColorMatrix(
        floatArrayOf(
            (sr + s) * c,  sg * c,        sb * c,        0f,     offsetR,
            sr * c,        (sg + s) * c,  sb * c,        0f,     offsetG,
            sr * c,        sg * c,        (sb + s) * c,  0f,     offsetB,
            0f,            0f,            0f,            alpha,  0f
        )
    )
}
