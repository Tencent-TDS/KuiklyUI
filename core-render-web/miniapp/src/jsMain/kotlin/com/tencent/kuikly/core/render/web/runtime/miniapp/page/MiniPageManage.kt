package com.tencent.kuikly.core.render.web.runtime.miniapp.page

import com.tencent.kuikly.core.render.web.collection.map.JsMap
import com.tencent.kuikly.core.render.web.collection.map.get
import com.tencent.kuikly.core.render.web.collection.map.set

/**
 * Mini program page management singleton
 */
object MiniPageManage {
    private var incrementPageId: Int = 0

    // Current MiniPage instance, corresponding to the currently displayed mini program page
    var currentPage: MiniPage? = null

    // Page instance Map
    private val pageMap: JsMap<Int, MiniPage> = JsMap()

    /**
     * Allocate a unique page id for each Page instance
     */
    fun generatePageUniqueId(): Int {
        incrementPageId += 1
        return incrementPageId
    }

    /**
     * Add MiniPage instance
     */
    fun addMiniPage(inst: MiniPage, id: Int) {
        pageMap[id] = inst
    }

    /**
     * Get MiniPage instance by id
     */
    fun getMiniPageByPageId(pageId: Int): MiniPage? = pageMap[pageId]

    /**
     * Delete MiniPage instance by id
     */
    fun removeMiniPageByPageId(pageId: Int) {
        pageMap.delete(pageId)
    }
}
