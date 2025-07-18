package com.tencent.kuikly.demo.pages.app.theme

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

enum class FontWeight {
    NORMAL, MEDIUM, SEMISOLID, BOLD
}

data class TextStyle(
    val typeface: String?,
    val weight: FontWeight,
    val size: Float,
    val letterSpacing: Float
)

data class ThemeTypography(
    val h1: TextStyle,
    val h2: TextStyle,
    val h3: TextStyle,
    val h4: TextStyle,
    val h5: TextStyle,
    val h6: TextStyle,
    val subtitle1: TextStyle,
    val subtitle2: TextStyle,
    val body1: TextStyle,
    val body2: TextStyle,
    val button: TextStyle,
    val caption: TextStyle,
    val overline: TextStyle
) {
    companion object {
        // 定义所有键名常量
        private const val KEY_H1 = "h1"
        private const val KEY_H2 = "h2"
        private const val KEY_H3 = "h3"
        private const val KEY_H4 = "h4"
        private const val KEY_H5 = "h5"
        private const val KEY_H6 = "h6"
        private const val KEY_SUBTITLE1 = "subtitle1"
        private const val KEY_SUBTITLE2 = "subtitle2"
        private const val KEY_BODY1 = "body1"
        private const val KEY_BODY2 = "body2"
        private const val KEY_BUTTON = "button"
        private const val KEY_CAPTION = "caption"
        private const val KEY_OVERLINE = "overline"

        fun fromJson(json: JSONObject): ThemeTypography {
            return ThemeTypography(
                h1 = parseTextStyle(json, KEY_H1),
                h2 = parseTextStyle(json, KEY_H2),
                h3 = parseTextStyle(json, KEY_H3),
                h4 = parseTextStyle(json, KEY_H4),
                h5 = parseTextStyle(json, KEY_H5),
                h6 = parseTextStyle(json, KEY_H6),
                subtitle1 = parseTextStyle(json, KEY_SUBTITLE1),
                subtitle2 = parseTextStyle(json, KEY_SUBTITLE2),
                body1 = parseTextStyle(json, KEY_BODY1),
                body2 = parseTextStyle(json, KEY_BODY2),
                button = parseTextStyle(json, KEY_BUTTON),
                caption = parseTextStyle(json, KEY_CAPTION),
                overline = parseTextStyle(json, KEY_OVERLINE)
            )
        }

        private fun parseTextStyle(json: JSONObject, key: String): TextStyle {
            val textStyleJson = json.optJSONObject(key) ?: JSONObject()
            return try {
                TextStyle(
                    typeface = textStyleJson.optString("typeface", "").takeIf { it.isNotEmpty() },
                    weight = FontWeight.valueOf(textStyleJson.optString("weight", "NORMAL")),
                    size = textStyleJson.optDouble("size", 16.0).toFloat(),
                    letterSpacing = textStyleJson.optDouble("letterSpacing", 0.0).toFloat()
                )
            } catch (e: IllegalArgumentException) {
                TextStyle(
                    typeface = null,
                    weight = FontWeight.NORMAL,
                    size = 16.0f,
                    letterSpacing = 0.0f
                )
            }
        }
    }
}

// 预定义的默认排版方案
val defaultTypography = ThemeTypography(
    h1 = TextStyle(null, FontWeight.BOLD, 24f, 0.0f),
    h2 = TextStyle(null, FontWeight.BOLD, 20f, 0.0f),
    h3 = TextStyle(null, FontWeight.BOLD, 18f, 0.0f),
    h4 = TextStyle(null, FontWeight.BOLD, 16f, 0.0f),
    h5 = TextStyle(null, FontWeight.BOLD, 14f, 0.0f),
    h6 = TextStyle(null, FontWeight.BOLD, 12f, 0.0f),
    subtitle1 = TextStyle(null, FontWeight.MEDIUM, 16f, 0.0f),
    subtitle2 = TextStyle(null, FontWeight.MEDIUM, 14f, 0.0f),
    body1 = TextStyle(null, FontWeight.NORMAL, 16f, 0.0f),
    body2 = TextStyle(null, FontWeight.NORMAL, 14f, 0.0f),
    button = TextStyle(null, FontWeight.BOLD, 14f, 0.0f),
    caption = TextStyle(null, FontWeight.NORMAL, 12f, 0.0f),
    overline = TextStyle(null, FontWeight.NORMAL, 10f, 0.0f)
)
