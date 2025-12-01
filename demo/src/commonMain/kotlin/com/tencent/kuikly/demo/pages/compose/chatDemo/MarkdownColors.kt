package com.tencent.kuikly.demo.pages.compose.chatDemo

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuiklybase.markdown.model.DefaultMarkdownColors
import com.tencent.kuiklybase.markdown.model.MarkdownColors

@Composable
internal fun markdownColor(
    text: Color = MdTheme.colorScheme.t1,
    codeBackground: Color = MdTheme.colorScheme.bgBlock,
    inlineCodeBackground: Color = codeBackground,
    dividerColor: Color = MdTheme.colorScheme.lineFine,
    tableBackground: Color = MdTheme.colorScheme.bgBlock,
    tableStroke: Color = MdTheme.colorScheme.lineStroke,
    tableHeaderBackground: Color = MdTheme.colorScheme.newBgBlock,
): MarkdownColors = DefaultMarkdownColors(
    text = text,
    codeText = Color.Unspecified,
    inlineCodeText = Color.Unspecified,
    linkText = Color.Unspecified,
    codeBackground = codeBackground,
    inlineCodeBackground = inlineCodeBackground,
    dividerColor = dividerColor,
    tableText = Color.Unspecified,
    tableBackground = tableBackground,
    tableHeaderBackground = tableHeaderBackground,
    tableStroke = tableStroke
)
