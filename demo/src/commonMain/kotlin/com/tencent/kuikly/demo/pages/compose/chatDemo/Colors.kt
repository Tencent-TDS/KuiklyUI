package com.tencent.kuikly.demo.pages.compose.chatDemo

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import com.tencent.kuikly.compose.ui.graphics.Color

internal val LocalColorScheme = staticCompositionLocalOf { LightColoScheme }

@Immutable
internal class ColorScheme(

    // 字体颜色
    val t1: Color,
    val t2: Color,
    val t3: Color,
    val aigcPrompt: Color,
    val bgBlock: Color,
    val newBgBlock: Color,
    // 分割线
    val lineFine: Color,
    val lineStroke: Color,
)

internal val LightColoScheme = ColorScheme(
    t1 = Color(0xFF333333),
    t2 = Color(0xFF5C5C5C),
    t3 = Color(0xFF999999),
    aigcPrompt = Color(0xFF505DE5),
    bgBlock = Color(0xFFF5F5F5),
    newBgBlock = Color(0xFFF7F7F7),
    lineFine = Color(0XFFF0F0F0),
    lineStroke = Color(0XFFE6E6E6),
)

internal val DarkColorScheme = ColorScheme(
    t1 = Color(0xFFD9D9D9),
    t2 = Color(0xFFA9A9A9),
    t3 = Color(0xFF696969),
    aigcPrompt = Color(0xFF7780D9),
    bgBlock = Color(0xFF262626),
    newBgBlock = Color(0xFF262626),
    lineFine = Color(0xFF292929),
    lineStroke = Color(0xFF303030),
)
