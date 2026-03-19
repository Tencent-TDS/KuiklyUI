package com.tencent.kuikly.core.render.web.runtime.miniapp.core

import com.tencent.kuikly.core.render.web.collection.array.emptyJsArrayOf
import com.tencent.kuikly.core.render.web.runtime.miniapp.const.ShortCutsConst
import com.tencent.kuikly.core.render.web.runtime.miniapp.event.EventManage
import com.tencent.kuikly.core.render.web.runtime.miniapp.page.MiniPage
import kotlin.js.Json
import kotlin.js.json

/**
 * Official supported major mini program page-level events
 */
enum class PageLifeCycleEvent(private val eventName: String) {
    ON_PULL_DOWN_REFRESH("onPullDownRefresh"),
    ON_REACH_BOTTOM("onReachBottom"),
    ON_PAGE_SCROLL("onPageScroll"),
    ON_TITLE_CLICK("onTitleClick"),
    ON_OPTION_MENU_CLICK("onOptionMenuClick"),
    ON_POP_MENU_CLICK("onPopMenuClick"),
    ON_PULL_INTERCEPT("onPullIntercept"),
    ON_ADD_TO_FAVORITES("onAddToFavorites"),
    ON_SHARE_APP_MESSAGE("onShareAppMessage");

    fun getValue(): String = eventName
}

/**
 * Provides methods and hooks for initializing mini program Page
 */
object Page {
    // Callback method name used in the template
    private const val GLOBAL_EVENT_HANDLER = "eh"
    private const val DATA = "data"

    // Main event callbacks for Page
    const val ON_LOAD = "onLoad"
    const val ON_UNLOAD = "onUnload"
    const val ON_READY = "onReady"
    const val ON_SHOW = "onShow"
    const val ON_HIDE = "onHide"

    private fun createPageConfig(pageInstance: MiniPage, initData: Json? = null): Json {
        val config = json(
            // Basic Page events
            ON_LOAD to { options: Any ->
                val mpInstance = js("this").unsafeCast<MpInstance>()
                pageInstance.emitLoad(mpInstance, options)
            },
            ON_UNLOAD to { pageInstance.emitUnLoad() },
            ON_READY to { pageInstance.emitReady() },
            ON_SHOW to { pageInstance.emitShow() },
            ON_HIDE to { pageInstance.emitHide() },
            // Additional Page events
            PageLifeCycleEvent.ON_PAGE_SCROLL.toString() to { args: Any ->
                pageInstance.emitPageLifeCycleEvent(
                    PageLifeCycleEvent.ON_PAGE_SCROLL,
                    args
                )
            },
            PageLifeCycleEvent.ON_PULL_DOWN_REFRESH.toString() to { args: Any ->
                pageInstance.emitPageLifeCycleEvent(
                    PageLifeCycleEvent.ON_PULL_DOWN_REFRESH,
                    args
                )
            },
            PageLifeCycleEvent.ON_REACH_BOTTOM.toString() to { args: Any ->
                pageInstance.emitPageLifeCycleEvent(
                    PageLifeCycleEvent.ON_REACH_BOTTOM,
                    args
                )
            },
            PageLifeCycleEvent.ON_TITLE_CLICK.toString() to { args: Any ->
                pageInstance.emitPageLifeCycleEvent(
                    PageLifeCycleEvent.ON_TITLE_CLICK,
                    args
                )
            },
            PageLifeCycleEvent.ON_OPTION_MENU_CLICK.toString() to { args: Any ->
                pageInstance.emitPageLifeCycleEvent(
                    PageLifeCycleEvent.ON_OPTION_MENU_CLICK,
                    args
                )
            },
            PageLifeCycleEvent.ON_POP_MENU_CLICK.toString() to { args: Any ->
                pageInstance.emitPageLifeCycleEvent(
                    PageLifeCycleEvent.ON_POP_MENU_CLICK,
                    args
                )
            },
            PageLifeCycleEvent.ON_PULL_INTERCEPT.toString() to { args: Any ->
                pageInstance.emitPageLifeCycleEvent(
                    PageLifeCycleEvent.ON_PULL_INTERCEPT,
                    args
                )
            },
            PageLifeCycleEvent.ON_ADD_TO_FAVORITES.toString() to { args: Any ->
                pageInstance.emitPageLifeCycleEvent(
                    PageLifeCycleEvent.ON_ADD_TO_FAVORITES,
                    args
                )
            },
            PageLifeCycleEvent.ON_SHARE_APP_MESSAGE.toString() to { args: Any ->
                pageInstance.emitShareAppMessage(args)
            },
            GLOBAL_EVENT_HANDLER to EventManage.eventHandler
        )

        if (initData != null) {
            config[DATA] = initData
        }

        return config
    }

    /**
     * Initialize mini program Page
     */
    fun initMiniPage(): MiniPage {
        // Current page runtime instance, handles mini program lifecycle hooks
        val pageInstance = MiniPage()
        // Parameters passed to the mini program page constructor, associated with the current Page instance
        val pageConfig = createPageConfig(
            pageInstance,
            json(ShortCutsConst.ROOT_NAME to json(ShortCutsConst.CHILD_NODE to emptyJsArrayOf<Json>()))
        )
        // Pass parameters to the mini program Page constructor
        Page(pageConfig)
        return pageInstance
    }
}
