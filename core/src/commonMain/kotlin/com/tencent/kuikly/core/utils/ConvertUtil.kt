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

package com.tencent.kuikly.core.utils

import com.tencent.kuikly.core.manager.PagerManager
import kotlin.math.roundToInt

/**
 * 提供数据转换工具方法
 */

object ConvertUtil {

    /**
     * value数据转换为Boolean
     */
    fun toBoolean(value: Any?): Boolean {
        if (value is String) {
           return value.length > 0
        } else if (value is Number) {
            return value.toFloat() != 0.0f
        } else if (value is Boolean) {
            return value
        }
        return value != null
    }

    /**
     * 将一个dp 浮点型数值转成可不带小数点的px数值的dp值
     */
    fun toIntegerPxOfDpValue(value: Float): Float {
        if (value.isNaN()) {
            return 0f
        }
        if (PagerManager.getCurrentPager().pageData.isIOS) { // iOS 不存在精度转换问题
            return value
        }
        val density = PagerManager.getCurrentPager().pagerDensity()
        if (density != 0f) {
            val px = (value * density).roundToInt().toFloat()
            return px / density
        }
        return value
    }

    /**
     * @param str16
     * @return
     * @Description: 将16进制的字符串转化为long值
     */
    fun parseString16ToLong(str16: String?): Long {
        var str16 = str16 ?: throw NumberFormatException("null")
        //先转化为小写
        str16 = str16.lowercase()
        //如果字符串以0x开头，去掉0x
        str16 = if (str16.startsWith("0x")) str16.substring(2) else str16
        if (str16.length > 16) {
            throw RuntimeException("For input string '$str16' is to long")
        }
        return parseMd5L16ToLong(str16)
    }

    /**
     * @param md5L16
     * @return
     * @Description: 将16位的md5转化为long值
     */
    private fun parseMd5L16ToLong(md5L16: String?): Long {
        var md5L16 = md5L16 ?: throw RuntimeException("null")
        md5L16 = md5L16.lowercase()
        val bA = md5L16.encodeToByteArray()
        var re = 0L
        for (i in bA.indices) {
            //加下一位的字符时，先将前面字符计算的结果左移4位
            re = re shl 4
            //0-9数组
            var b = (bA[i] - 48).toByte()
            //A-F字母
            if (b > 9) {
                b = (b - 39).toByte()
            }
            //非16进制的字符
            if (b > 15 || b < 0) {
                throw RuntimeException("For input string '$md5L16")
            }
            re += b.toLong()
        }
        return re
    }
}