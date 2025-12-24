package com.example.kuiklyworkwithminiapp.adapter

import android.content.Context
import android.graphics.Color
import com.tencent.kuikly.core.render.android.adapter.IKRColorParserAdapter
import com.example.kuiklyworkwithminiapp.SkinIniFile

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