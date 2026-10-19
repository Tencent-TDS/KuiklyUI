/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

package com.tencent.kuikly.core.render.android.css.animation

import android.animation.TimeInterpolator
import android.view.animation.PathInterpolator

internal data class NativeCurve(val kind: String, val values: List<Float>) {
    companion object {
        const val KIND_CUBIC = "cubic"
        const val KIND_SNAP = "snap"

        fun parse(payload: String): NativeCurve? {
            val parts = payload.split(',')
            if (parts.size < 2 || parts[0] != "v2") return null
            val kind = parts[1]
            if (kind != KIND_CUBIC && kind != KIND_SNAP) return null
            val values = parts.drop(2).map { it.toFloatOrNull() ?: return null }
            if (kind == KIND_CUBIC && values.size != 4) return null
            return NativeCurve(kind, values)
        }
    }
}

internal fun parseNativeCurveV2(animationParts: List<String>): NativeCurve? =
    animationParts.firstOrNull { it.startsWith("v2,") }?.let { NativeCurve.parse(it) }

internal fun KRCSSAnimationHandler.applyNativeV2Curve(curve: NativeCurve?) {
    if (this is KRCSSPlainAnimationHandler) nativeCurve = curve
}

internal fun NativeCurve?.createTimeInterpolatorOrNull(): TimeInterpolator? =
    this
        ?.takeIf { it.kind == NativeCurve.KIND_CUBIC }
        ?.values
        ?.let { PathInterpolator(it[0], it[1], it[2], it[3]) }
