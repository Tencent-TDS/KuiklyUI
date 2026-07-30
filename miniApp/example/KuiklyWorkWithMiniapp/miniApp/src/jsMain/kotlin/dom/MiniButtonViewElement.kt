package dom

import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniElementUtil


class MiniButtonViewElement(
    nodeName: String = NODE_NAME,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType) {
    var openType: String = ""
        set(value) {
            setAttribute("openType", value)
        }

    companion object {
        const val NODE_NAME = "button"
        val componentsAlias = js("""
            {
                _num: '76', 
                class: 'cl',
                animation: 'p0',
                openType: 'openType',
                formType: 'formType',
                type: 'type',
                size: 'size',
                type: 'type',
                name: 'name',
                lang: 'lang',
                sessionFrom: 'sessionFrom',
          }
        """)
    }
}