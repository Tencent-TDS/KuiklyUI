package com.tencent.kuikly.core.render.web.runtime.miniapp.dom

import com.tencent.kuikly.core.render.web.runtime.miniapp.const.TransformConst

/**
 * First node corresponding to kuikly's KRListView, serves as scroll area
 * List's mini program implementation could be scroll-view or movable-area, need to handle differently
 */
class MiniScrollContentElement(
    nodeName: String = TransformConst.VIEW,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType) {
    init {
        // Set movable-view required content first, if not movable-view in the end, these settings won't take effect
        // If delayed, will cause multiple setData calls
        setAttribute("direction", "horizontal")
        setAttribute("animation", true)
        setAttribute("inertia", false)
        setAttribute("damping", 20)
    }

    override fun onTransformData(): String {
        val isMovableArea = parentElement?.getAttribute("isMovableArea") == true
        val isScrollDisable = parentElement?.getAttribute("isScrollDisable") == true
        // When parent node is movable-area, scrollContent must use movable-view
        if (isMovableArea) {
            setAttribute("disabled", isScrollDisable)
            return TransformConst.SCROLL_CONTENT
        }
        return super.onTransformData()
    }
}
