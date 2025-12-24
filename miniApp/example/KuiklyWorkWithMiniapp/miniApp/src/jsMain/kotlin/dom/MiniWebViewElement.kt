package dom

import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniElementUtil

/**
 * Inherit from MiniElement class, add own implementation, nodeName adds
 * \real mini program tags, for example here using web-view
 */
class MiniWebViewElement(
    nodeName: String = NODE_NAME,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType) {
    var src: String = ""
        set(value) {
            setAttribute("src", value)
        }

    companion object {
        const val NODE_NAME = "web-view"
        val componentsAlias = js("{_num: '74', src: 'p0'}")
    }
}