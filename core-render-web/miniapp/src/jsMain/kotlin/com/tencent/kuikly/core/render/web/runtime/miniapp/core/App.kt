package com.tencent.kuikly.core.render.web.runtime.miniapp.core

import com.tencent.kuikly.core.render.web.runtime.miniapp.event.EventHook
import kotlin.js.json

typealias AppLifeCallback = (params: Any?) -> Unit

/**
 * Provides methods and hooks for initializing mini program App
 */
object App {
    private const val ON_PAGE_NOT_FOUND = "onPageNotFound"
    private const val ON_UNHANDLED_REJECTION = "onUnhandledRejection"
    private const val ON_ERROR = "onError"
    private const val ON_LAUNCH = "onLaunch"
    private const val ON_SHOW = "onShow"
    private const val ON_HIDE = "onHide"


    /**
     * Initialize mini program App
     */
    fun initApp(options: dynamic) {
        App(
            json(
                ON_LAUNCH to { params: dynamic ->
                    EventHook.call(ON_LAUNCH)
                    if (jsTypeOf(options[ON_LAUNCH]) == "function") {
                        options[ON_LAUNCH](params)
                    }
                },
                ON_SHOW to { params: dynamic ->
                    EventHook.call(ON_SHOW)
                    if (jsTypeOf(options[ON_SHOW]) == "function") {
                        options[ON_SHOW](params)
                    }
                },
                ON_HIDE to { params: dynamic ->
                    EventHook.call(ON_HIDE)
                    if (jsTypeOf(options[ON_HIDE]) == "function") {
                        options[ON_HIDE](params)
                    }
                },
                ON_ERROR to { params: dynamic ->
                    EventHook.call(ON_ERROR)
                    if (jsTypeOf(options[ON_ERROR]) == "function") {
                        options[ON_ERROR](params)
                    }
                },
                ON_PAGE_NOT_FOUND to { params: dynamic ->
                    EventHook.call(ON_PAGE_NOT_FOUND)
                    if (jsTypeOf(options[ON_PAGE_NOT_FOUND]) == "function") {
                        options[ON_PAGE_NOT_FOUND](params)
                    }
                },
                ON_UNHANDLED_REJECTION to { params: dynamic ->
                    EventHook.call(ON_UNHANDLED_REJECTION)
                    if (jsTypeOf(options[ON_UNHANDLED_REJECTION]) == "function") {
                        options[ON_UNHANDLED_REJECTION](params)
                    }
                },
            )
        )
    }

    fun onLaunch(callback: AppLifeCallback) {
        EventHook.tap(ON_LAUNCH, callback)
    }

    fun onUnhandledRejection(callback: AppLifeCallback) {
        EventHook.tap(ON_UNHANDLED_REJECTION, callback)
    }

    fun onError(callback: AppLifeCallback) {
        EventHook.tap(ON_ERROR, callback)
    }

    fun onShow(callback: AppLifeCallback) {
        EventHook.tap(ON_SHOW, callback)
    }

    fun onHide(callback: AppLifeCallback) {
        EventHook.tap(ON_HIDE, callback)
    }
}
