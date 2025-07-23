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

package com.tencent.kuikly.android.demo.adapter

import android.content.Context
import android.graphics.Color
import com.tencent.kuikly.core.render.android.adapter.IKRColorParserAdapter
import com.tencent.kuikly.android.demo.SkinIniFile

class KRColorParserAdapter(context: Context) : IKRColorParserAdapter {

    companion object {
        private const val COLOR_UNIQUE_ID = "_color_unique_id_"
        private const val COLOR_KUIKLY_TOKEN_PREFIX = "kuikly"
        private const val COLOR_SECTION = "Color"
    }

    private val colorIniFile = SkinIniFile(context).apply {
        load("configColor.ini")
    }

    override fun toColor(colorStr: String): Int? {
        if (!colorStr.contains(COLOR_UNIQUE_ID) && !colorStr.contains(COLOR_KUIKLY_TOKEN_PREFIX)) {
            return colorStr.toLongOrNull()?.toInt()
        }

        var token = colorStr
        if (colorStr.contains(COLOR_UNIQUE_ID)) {
            val index = colorStr.indexOf(COLOR_UNIQUE_ID)
            token = colorStr.substring(0, index)
        }

        val colorHex = colorIniFile.get(
            sectionName = COLOR_SECTION,
            sectionKey = token,
            defaultValue = null
        )
            ?: throw IllegalArgumentException("找不到对应的颜色 token=$token，请检查 demo 中 configColor.ini 文件中是否存在改颜色，若不存在，手动更新一下")
        return Color.parseColor(colorHex)
    }
}
