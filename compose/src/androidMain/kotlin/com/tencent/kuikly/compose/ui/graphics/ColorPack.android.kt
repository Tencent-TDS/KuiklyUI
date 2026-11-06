package com.tencent.kuikly.compose.ui.graphics

internal actual fun packSrgbColor(argb: Int): ULong =
    (argb.toULong() and 0xffffffffUL) shl 32
