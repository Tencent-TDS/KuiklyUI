package com.tencent.kuikly.core.render.web.runtime.miniapp.dom

import com.tencent.kuikly.core.render.web.collection.map.JsMap
import com.tencent.kuikly.core.render.web.collection.map.get
import com.tencent.kuikly.core.render.web.collection.map.set

/**
 * Node management, maintains a node map, supporting node queries
 */
object MiniElementManage {
    private val elementMap: JsMap<String, MiniElement> = JsMap()

    /**
     * Delete node cache
     */
    fun deleteElement(sid: String) {
        elementMap.delete(sid)
    }


    /**
     * Set node cache
     */
    fun setElement(sid: String, child: MiniElement) {
        elementMap[sid] = child
    }

    /**
     * Get node by sid
     */
    fun getElement(sid: String): MiniElement? = elementMap[sid]

    /**
     * Delete node and child nodes cache
     */
    fun removeNodeTree(child: MiniElement) {
        deleteElement(child.unsafeCast<MiniElement>().innerId)
        child.childNodes.forEach { element ->
            removeNodeTree(element)
        }
    }
}
