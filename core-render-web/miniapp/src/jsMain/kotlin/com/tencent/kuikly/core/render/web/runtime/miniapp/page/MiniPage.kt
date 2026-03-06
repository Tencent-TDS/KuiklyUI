package com.tencent.kuikly.core.render.web.runtime.miniapp.page

import com.tencent.kuikly.core.render.web.KuiklyRenderView
import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.add
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.MpInstance
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.Page.ON_HIDE
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.Page.ON_LOAD
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.Page.ON_READY
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.Page.ON_SHOW
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.Page.ON_UNLOAD
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.PageLifeCycleEvent
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniListElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.event.EventHook
import com.tencent.kuikly.core.render.web.utils.Log
import kotlin.js.Promise

/**
 * Mini program page class, mainly associates page-level events
 */
class MiniPage {
    // Current renderView instance corresponding to this Page instance
    var renderView: KuiklyRenderView? = null

    // Page's unique ID
    val pageId = MiniPageManage.generatePageUniqueId()

    // Initialize page event instance
    val lifeCycle = PageLifeCycle(this)

    // Variable for scroll state detection, consider removing
    var isScrolling: Boolean = false

    // Variable for paging state detection, consider removing
    var isPaging: Boolean = false

    // onload completion callback hasLoaded
    private var loadResolver: (Any) -> Unit = {}

    // Promise for checking page onload
    private val hasLoaded = Promise { resolve, _ ->
        loadResolver = resolve
    }

    // Mini program instance corresponding to current page
    var mpInstance: MpInstance? = null

    // All moveable lists in the project
    private val movableList: JsArray<MiniListElement> = JsArray()

    /**
     * Handle mini program's own Page constructor onLoad callback
     */
    fun emitLoad(instance: MpInstance, args: Any) {
        Log.log("page onLoad: ", args)
        // Set current mini program Page instance
        mpInstance = instance
        // Set current page and current route
        MiniPageManage.currentPage = this
        // Inject current page into global cache instance
        MiniPageManage.addMiniPage(this, pageId)
        // Mark load as completed
        loadResolver(true)
        // Trigger OnLoad hook. Initialize based on PageName obtained here
        EventHook.callWithPageId(ON_LOAD, pageId, args)
    }

    /**
     * Trigger unload event, need to delete self from MiniPageManage
     */
    fun emitUnLoad() {
        renderView?.destroy()
        EventHook.callWithPageId(ON_UNLOAD, pageId)
        MiniPageManage.removeMiniPageByPageId(pageId)
    }

    /**
     * Trigger ready event, needs to be triggered after onLoad
     */
    fun emitReady() {
        hasLoaded.then {
            EventHook.callWithPageId(ON_READY, pageId)
        }
    }

    /**
     * Trigger onShow event, must be triggered after onLoad, needs to change MiniPageManage.currentPage
     */
    fun emitShow() {
        hasLoaded.then {
            MiniPageManage.currentPage = this
            renderView?.resume()
            EventHook.callWithPageId(ON_SHOW, pageId)
        }
    }

    /**
     * Trigger onHide event, needs to change MiniPageManage.currentPage
     */
    fun emitHide() {
        if (MiniPageManage.currentPage == this) {
            renderView?.pause()
            MiniPageManage.currentPage = null
        }
        EventHook.callWithPageId(ON_HIDE, pageId)
    }

    /**
     * Trigger other page events
     */
    fun emitPageLifeCycleEvent(event: PageLifeCycleEvent, vararg args: Any) {
        hasLoaded.then {
            EventHook.callWithPageId(event.toString(), pageId, args)
        }
    }

    /**
     * Trigger onShareAppMessage event and return result
     */
    fun emitShareAppMessage(vararg args: Any): Any? {
        return EventHook.callWithPageIdAndReturn(PageLifeCycleEvent.ON_SHARE_APP_MESSAGE.toString(), pageId, args)
    }

    /**
     * Add movable element to the list
     */
    fun addMovableViewToList(listElement: MiniListElement) {
        movableList.add(listElement)
    }

    /**
     * Disable all moveable-views in the project
     */
    fun disableMovableView() {
        movableList.forEach { listItem ->
            // Disable movable view for all listElements with movable-view, must be scrollable
            if (listItem.isMovableArea && !listItem.isDisableMovableView && listItem.scrollEnabled) {
                // First set the list element as moveable disabled
                listItem.isDisableMovableView = true
                // Then set the mini program real element as disabled
                listItem.firstElementChild?.setAttribute("disabled", true)
            }
        }
    }

    /**
     * Enable all moveable-views in the project
     */
    fun enableMovableView() {
        movableList.forEach { listItem ->
            // Restore disabled movable view for all listElements with movable-view, note that if listItem itself
            // is not scrollable, don't restore
            if (listItem.isMovableArea && listItem.isDisableMovableView && listItem.scrollEnabled) {
                // First restore the list element's disabled property
                listItem.isDisableMovableView = false
                // Then restore the mini program real element's moveable
                listItem.firstElementChild?.setAttribute("disabled", false)
            }
        }
    }
}
