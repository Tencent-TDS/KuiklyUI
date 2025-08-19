package com.tencent.kuikly.core.render.web.adapter

/**
 * Color conversion adapter
 */
interface IKRColorParserAdapter {

    /**
     * Convert color of type [String] to hexadecimal color [Int]
     * @param colorStr String color
     * @return Hexadecimal color [Int]
     */
    fun toColor(colorStr: String): Long?
}
