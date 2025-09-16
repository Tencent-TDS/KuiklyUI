package com.tencent.kuikly.core.render.web.expand.components.list

import com.tencent.kuikly.core.render.web.ktx.KRCssConst

/**
 * listView contentInset parameter formatting
 */
class KRListViewContentInset(contentInset: String = KRCssConst.EMPTY_STRING) {
    var top = 0f
    var left = 0f
    var bottom = 0f
    var right = 0f
    var animate: Boolean = false

    init {
        if (contentInset.isNotEmpty()) {
            val spilt = contentInset.split(KRCssConst.BLANK_SEPARATOR)
            top = spilt[0].toFloat()
            left = spilt[1].toFloat()
            bottom = spilt[2].toFloat()
            right = spilt[3].toFloat()
            if (spilt.size > 4) {
                animate = spilt[4].toInt() == 1
            }
        }
    }
}
