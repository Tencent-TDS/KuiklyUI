package com.tencent.kuikly.core.render.web.runtime.miniapp.dom

import com.tencent.kuikly.core.render.web.runtime.miniapp.const.TransformConst

/**
 * Mini program pure text node, eventually rendered as pure text content in mini program
 */
class MiniSpanElement(
    nodeName: String = TransformConst.RAWTEXT,
    nodeType: Int = MiniElementUtil.TEXT_NODE
) : MiniElement(nodeName, nodeType) {
    var textContent: String? = ""

    // provide a method to update ui when use as custom view
    fun updateUiText() {
        parentElement?.updateChildNodes()
    }
}
