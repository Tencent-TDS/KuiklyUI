package com.tencent.kuikly.demo.pages.compose.chatDemo

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ui.text.TextLinkStyles
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuiklybase.markdown.model.DefaultMarkdownTypography
import com.tencent.kuiklybase.markdown.model.MarkdownTypography

@Composable
internal fun markdownTypography(
    h1: TextStyle = TextStyle(
        color = MdTheme.colorScheme.t1,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 30.sp,
    ),
    h2: TextStyle = TextStyle(
        color = MdTheme.colorScheme.t1,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 28.sp,
    ),
    h3: TextStyle = TextStyle(
        color = MdTheme.colorScheme.t1,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 28.sp,
    ),
    h4: TextStyle = TextStyle(
        color = MdTheme.colorScheme.t1,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 27.sp,
    ),
    h5: TextStyle = h4,
    h6: TextStyle = h4,

    text: TextStyle = TextStyle(
        color = MdTheme.colorScheme.t1,
        fontSize = 16.sp,
        lineHeight = 28.sp
    ),

    code: TextStyle = TextStyle(
        color = MdTheme.colorScheme.t1,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),

    inlineCode: TextStyle = TextStyle(
        color = MdTheme.colorScheme.t1,
        fontSize = 14.sp,
        lineHeight = 28.sp
    ),

    quote: TextStyle = TextStyle(
        color = MdTheme.colorScheme.t3,
        fontSize = 16.sp,
        lineHeight = 28.sp
    ),

    paragraph: TextStyle = TextStyle(
        color = MdTheme.colorScheme.t1,
        fontSize = 16.sp,
        lineHeight = 28.sp
    ),

    ordered: TextStyle = TextStyle(
        color = MdTheme.colorScheme.t1,
        fontSize = 16.sp,
        lineHeight = 28.sp
    ),
    bullet: TextStyle = TextStyle(
        color = MdTheme.colorScheme.t1,
        fontSize = 16.sp,
        lineHeight = 28.sp
    ),
    list: TextStyle = TextStyle(
        color = MdTheme.colorScheme.t1,
        fontSize = 16.sp,
        lineHeight = 28.sp
    ),

    link: TextStyle = TextStyle(
        color = MdTheme.colorScheme.aigcPrompt,
        fontSize = 16.sp,
    ),
    textLink: TextLinkStyles = TextLinkStyles(
        style = link.toSpanStyle()
    ),

    table: TextStyle = text

): MarkdownTypography = DefaultMarkdownTypography(
    h1 = h1,
    h2 = h2,
    h3 = h3,
    h4 = h4,
    h5 = h5,
    h6 = h6,
    text = text,
    quote = quote,
    code = code,
    inlineCode = inlineCode,
    paragraph = paragraph,
    ordered = ordered,
    bullet = bullet,
    list = list,
    link = link,
    textLink = textLink,
    table = table,
)
