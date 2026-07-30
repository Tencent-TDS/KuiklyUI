package com.kuikly.table.model

import com.tencent.kuikly.core.layout.FlexAlign

/**
 * 文本对齐方式
 */
enum class TextAlign(val value: String) {
    LEFT("left"),
    CENTER("center"),
    RIGHT("right")
}

/**
 * 映射到 Kuikly 2.15 的 FlexAlign 枚举（旧版用字符串）
 */
fun TextAlign.toFlexAlign(): FlexAlign = when (this) {
    TextAlign.LEFT -> FlexAlign.FLEX_START
    TextAlign.CENTER -> FlexAlign.CENTER
    TextAlign.RIGHT -> FlexAlign.FLEX_END
}
