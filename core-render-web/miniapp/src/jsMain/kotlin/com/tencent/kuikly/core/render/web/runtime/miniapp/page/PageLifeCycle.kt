package com.tencent.kuikly.core.render.web.runtime.miniapp.page

import com.tencent.kuikly.core.render.web.runtime.miniapp.core.Page
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.PageLifeCycleEvent
import com.tencent.kuikly.core.render.web.runtime.miniapp.event.EventHook

typealias OnLoadCallback = (pageName: String, params: dynamic) -> Unit

typealias PageLifeCallback = (params: Any?) -> Unit

/**
 * Mini program lifecycle callbacks exposed for external use
 */
class PageLifeCycle(private val pageInstance: MiniPage) {
    /**
     * Expose onLoad event, the callback needs to handle the open parameters,
     * get page_name and passed business parameters
     */
    fun onLoad(callback: OnLoadCallback) {
        EventHook.tapWithPageId(Page.ON_LOAD, pageInstance.pageId) {
            var pageName = ""
            val params = it[0].unsafeCast<Any?>()
            if (params != null) {
                pageName = params.asDynamic()?.page_name.unsafeCast<String?>() ?: ""
            }
            callback(pageName, params)
        }
    }

    /**
     * Page onReady callback
     */
    fun onReady(callback: PageLifeCallback) {
        EventHook.tapWithPageId(Page.ON_READY, pageInstance.pageId) {
            callback(it)
        }
    }

    /**
     * Page onUnload callback
     */
    fun onUnload(callback: PageLifeCallback) {
        EventHook.tapWithPageId(Page.ON_UNLOAD, pageInstance.pageId) {
            callback(it)
        }
    }

    /**
     * Page onShow callback
     */
    fun onShow(callback: PageLifeCallback) {
        EventHook.tapWithPageId(Page.ON_SHOW, pageInstance.pageId) {
            callback(it)
        }
    }


    /**
     * Page onHide callback
     */
    fun onHide(callback: PageLifeCallback) {
        EventHook.tapWithPageId(Page.ON_HIDE, pageInstance.pageId) {
            callback(it)
        }
    }

    /**
     * Page onShareAppMessage callback
     */
    fun onShareAppMessage(callback: (Any?) -> Any?) {
        EventHook.tapWithPageId(PageLifeCycleEvent.ON_SHARE_APP_MESSAGE.toString(), pageInstance.pageId) {
            callback(it[0])
        }
    }

    /**
     * Other mini program page event callbacks, requires passing in the event enum PageLifeCycleEvent
     */
    fun onPageLifeEvent(event: PageLifeCycleEvent, callback: PageLifeCallback) {
        EventHook.tapWithPageId(event.toString(), pageInstance.pageId) {
            callback(it)
        }
    }
}
