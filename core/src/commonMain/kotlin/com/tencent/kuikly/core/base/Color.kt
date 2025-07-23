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

package com.tencent.kuikly.core.base

import com.tencent.kuikly.core.utils.ConvertUtil

class Color {

    private var colorString: String = ""
    var hexColor: Long = 0
        private set

    constructor()

    /**
     * 十六进制颜色值构造方法，如 0xFFFFFFFFL。
     * 注: 最左边 2 位为 alpha，剩余六位为 rgb。
     */
    constructor(hexColor: Long) {
        this.hexColor = hexColor
    }

    /**
     * 十六进制颜色值及透明度构造方法。
     * @param hexColor 十六进制颜色值，如 0xFFFFFF。
     * @param alpha01 透明度值，范围 0.0 到 1.0。
     */
    constructor(hexColor: Long, alpha01: Float) {
        val red255: Long = hexColor and 0xFF0000 shr 16
        val green255: Long = hexColor and 0xFF00 shr 8
        val blue255: Long = hexColor and 0xFF
        val hexString = (alpha01 * 255).toInt().toColorHexString() + red255.toColorHexString() + green255.toColorHexString() + blue255.toColorHexString()
        this.hexColor = hexString.toLong(16)
    }

    /**
     * 宿主扩展字符串构造方法。
     * 该方法用于宿主扩展颜色能力使用，如token等，若有十六进制字符串需求，可通过parseString16ToLong生成Long然后Color(hexColor:Long)构造
     * @param colorString 透传字符串
     */
    constructor(colorString: String) {
        this.colorString = colorString
    }

    /**
     * 基于红绿蓝及透明度的构造方法。
     * @param red255 红色值，范围 0 到 255。
     * @param green255 绿色值，范围 0 到 255。
     * @param blue255 蓝色值，范围 0 到 255。
     * @param alpha01 透明度值，范围 0.0 到 1.0。
     */
    constructor(red255: Int, green255: Int, blue255: Int, alpha01: Float) {
        val hexString = (alpha01 * 255).toInt().toColorHexString() + red255.toColorHexString() + green255.toColorHexString() + blue255.toColorHexString()
        this.hexColor = hexString.toLong(16)
    }

    override fun toString(): String {
        return colorString.ifEmpty {
            hexColor.toString()
        }
    }

    /**
     * 与 SwiftUI 方法类似：https://developer.apple.com/documentation/swiftui/color/opacity(_:)
     * 通过 Color 对象生成指定 alpha 通道的颜
     */
    fun opacity(opacity: Float): Color {
        return Color(hexColor and 0x00FFFFFF, opacity)
    }

    companion object {
        val BLACK = Color(0xff000000L)
        val BLUE = Color(0xff0000FFL)
        val RED = Color(0xffFF0000L)
        val GREEN = Color(0xff00FF00L)
        val WHITE = Color(0xffFFFFFFL)
        val YELLOW = Color(0xffFFFF00L)
        val TRANSPARENT = Color(0x00000000L)
        val TRANSPARENT_WHITE = Color(255, 255, 255, 0f)
        val GRAY = Color(0xff999999L)

        /**
         * 将 16 进制字符串转换为 long 值。
         * @param colorString 16 进制字符串，如 "0xff00FF00"。
         * @return long 值，如 0xff00FF00。
         */
        fun parseString16ToLong(colorString: String): Long {
            // 默认为蓝色
            var colorLong = 0xff0000FFL
            try {
                colorLong = ConvertUtil.parseString16ToLong(colorString)
            } catch (e: Exception) {
            }
            return colorLong
        }
    }
}

fun Int.toColorHexString(): String {
    val hexStr = toString(16)
    if (hexStr.length == 1) {
        return "0$hexStr"
    }
    return hexStr
}

fun Long.toColorHexString(): String {
    val hexStr = toString(16)
    if (hexStr.length == 1) {
        return "0$hexStr"
    }
    return hexStr
}