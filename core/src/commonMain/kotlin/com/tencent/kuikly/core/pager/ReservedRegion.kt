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

package com.tencent.kuikly.core.pager

import com.tencent.kuikly.core.nvi.serialization.json.JSONArray

/**
 * 避让区域类型，[rawValue] 对齐 iOS `UIViewReservedRegionKind`。
 *
 * iOS 的 kind 是 `UIKIT_FINAL` 的 NSObject 类而非枚举，理论上可扩展；
 * 但「被遮挡」与「需分隔」是概念上封闭的两种语义，故这里用枚举并对未识别值回退 [UNKNOWN]。
 */
enum class ReservedRegionKind(val rawValue: String) {
    UNKNOWN(""),

    /** 被系统元素遮挡的区域，如摄像头、侧边状态栏。 */
    OCCLUSION("occlusion"),

    /** 折缝区域，内容应分居两侧。 */
    DIVISION("division");

    companion object {
        fun fromRaw(rawValue: String): ReservedRegionKind =
            values().firstOrNull { it.rawValue == rawValue } ?: UNKNOWN
    }
}

/**
 * Reserved Region（系统避让区域）。坐标相对 Kuikly 根视图，单位是点。
 *
 * 需要 **KuiklyUI 2.29.0** 及以上版本，且仅 iOS 27.1+ 的折叠设备（如 iPhone Duo）会返回数据；
 * 其他平台 / 设备上 [PagerData.reservedRegions] 为空列表。
 *
 * @property kind 区域类型，见 [ReservedRegionKind]
 * @property active 当前是否生效（如设备平铺时折缝为未生效）
 */
class ReservedRegion(
    val kind: ReservedRegionKind,
    val active: Boolean,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    /** frame 中为交互内容预留的边距，[width] / [height] 已包含它。 */
    val marginTop: Float,
    val marginLeft: Float,
    val marginBottom: Float,
    val marginRight: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReservedRegion) return false
        return kind == other.kind &&
            active == other.active &&
            x == other.x &&
            y == other.y &&
            width == other.width &&
            height == other.height &&
            marginTop == other.marginTop &&
            marginLeft == other.marginLeft &&
            marginBottom == other.marginBottom &&
            marginRight == other.marginRight
    }

    override fun hashCode(): Int {
        var result = kind.hashCode()
        result = 31 * result + active.hashCode()
        result = 31 * result + x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + width.hashCode()
        result = 31 * result + height.hashCode()
        result = 31 * result + marginTop.hashCode()
        result = 31 * result + marginLeft.hashCode()
        result = 31 * result + marginBottom.hashCode()
        result = 31 * result + marginRight.hashCode()
        return result
    }

    override fun toString(): String =
        "ReservedRegion(kind=$kind, active=$active, x=$x, y=$y, width=$width, height=$height, " +
            "marginTop=$marginTop, marginLeft=$marginLeft, marginBottom=$marginBottom, " +
            "marginRight=$marginRight)"

    companion object {

        /** 解析 iOS 上报的 reservedRegions JSON，格式异常时回退空列表。 */
        internal fun decodeReservedRegions(raw: String): List<ReservedRegion> {
            if (raw.isEmpty() || raw == "[]") {
                return emptyList()
            }
            return try {
                val array = JSONArray(raw)
                val regions = ArrayList<ReservedRegion>(array.length())
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    regions.add(
                        ReservedRegion(
                            kind = ReservedRegionKind.fromRaw(item.optString("kind", "")),
                            active = item.optInt("active", 0) == 1,
                            x = item.optDouble("x", 0.0).toFloat(),
                            y = item.optDouble("y", 0.0).toFloat(),
                            width = item.optDouble("width", 0.0).toFloat(),
                            height = item.optDouble("height", 0.0).toFloat(),
                            marginTop = item.optDouble("marginTop", 0.0).toFloat(),
                            marginLeft = item.optDouble("marginLeft", 0.0).toFloat(),
                            marginBottom = item.optDouble("marginBottom", 0.0).toFloat(),
                            marginRight = item.optDouble("marginRight", 0.0).toFloat()
                        )
                    )
                }
                regions
            } catch (_: Throwable) {
                emptyList()
            }
        }

    }
}
