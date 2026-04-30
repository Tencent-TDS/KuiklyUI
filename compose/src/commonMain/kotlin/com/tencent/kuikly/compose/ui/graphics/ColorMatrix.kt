/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.ui.graphics

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * A 4×5 color transformation matrix used to modify image colors at the native rendering layer.
 *
 * The matrix is stored in row-major order as 20 floats:
 * ```
 * [ R' ]   [ a  b  c  d  e ] [ R ]
 * [ G' ] = [ f  g  h  i  j ] [ G ]
 * [ B' ]   [ k  l  m  n  o ] [ B ]
 * [ A' ]   [ p  q  r  s  t ] [ A ]
 * ```
 * where the 5th column (e, j, o, t) is an additive offset (0–255 range).
 *
 * Use [ColorMatrix.grayscale] or [ColorMatrix.identity] for common presets,
 * or construct with a custom 20-element [FloatArray].
 *
 * Serialize to the native prop string with [toColorMatrixString].
 */
@Immutable
class ColorMatrix(val values: FloatArray) {

    init {
        require(values.size == 20) { "ColorMatrix requires exactly 20 values, got ${values.size}" }
    }

    /**
     * Serializes this matrix to the `|`-separated string format expected by the
     * native `colorFilter` prop (Android, iOS, HarmonyOS).
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
         * Grayscale matrix using ITU-R BT.709 luminosity coefficients.
         *
         * R' = G' = B' = 0.2126·R + 0.7152·G + 0.0722·B
         */
        @Stable
        val rec709Gray: ColorMatrix = ColorMatrix(
            floatArrayOf(
                0.2126f, 0.7152f, 0.0722f, 0f, 0f,
                0.2126f, 0.7152f, 0.0722f, 0f, 0f,
                0.2126f, 0.7152f, 0.0722f, 0f, 0f,
                0f,      0f,      0f,      1f, 0f
            )
        )

        /**
         * Identity matrix — no color transformation applied.
         */
        @Stable
        val identity: ColorMatrix = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        )

        /**
         * Creates a [ColorMatrix] from a raw `|`-separated string (20 floats).
         * Returns `null` if the string is malformed.
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

data class ColorMatrixConfig(
    val saturation: Float = 1f,   // 饱和度
    val brightness: Float = 0f,   // 亮度偏移（-255 ~ 255）
    val contrast: Float = 1f,     // 对比度
    val alpha: Float = 1f         // 透明度
)

fun ColorMatrixConfig.toColorMatrix(): ColorMatrix {
    // 饱和度：在灰度基础上插值到原色
    // 灰度权重 (ITU-R BT.709)
    val lumR = 0.2126f
    val lumG = 0.7152f
    val lumB = 0.0722f
    val s = saturation
    val sr = (1f - s) * lumR
    val sg = (1f - s) * lumG
    val sb = (1f - s) * lumB

    // 对比度：以 127.5 为中心缩放，偏移量 = (1 - contrast) * 127.5
    val c = contrast
    val contrastOffset = (1f - c) * 127.5f

    // 亮度偏移叠加到对比度偏移上
    val offsetR = contrastOffset + brightness
    val offsetG = contrastOffset + brightness
    val offsetB = contrastOffset + brightness

    return ColorMatrix(
        floatArrayOf(
            (sr + s) * c,  sg * c,        sb * c,        0f,     offsetR,
            sr * c,        (sg + s) * c,  sb * c,        0f,     offsetG,
            sr * c,        sg * c,        (sb + s) * c,  0f,     offsetB,
            0f,            0f,            0f,            alpha,  0f,
        )
    )
}