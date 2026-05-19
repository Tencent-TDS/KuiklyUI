package com.tencent.kuikly.core.render.web.runtime.miniapp.event

import com.tencent.kuikly.core.render.web.runtime.miniapp.page.MiniPageManage

/**
 * Overall mini program event handling, divided into page-level and non-page-level
 */
@JsExport
@OptIn(ExperimentalJsExport::class)
object EventHook {
    // 改为 internal 而不是 private，确保实例方法可访问
    internal val runTimeHooks: EventEmitter = EventEmitter()

    /**
     * Listen for non-page-level events
     */
    fun tap(hookName: String, callBack: CallBack = {}) {
        runTimeHooks.on(hookName, callBack)
    }

    /**
     * Trigger non-page-level events
     */
    fun call(hookName: String, vararg args: Any) {
        runTimeHooks.trigger(hookName, *args)
    }

    /**
     * Trigger page-level events
     */
    fun callWithPageId(hookName: String, pageId: Int, vararg args: Any) {
        val pageInstance = MiniPageManage.getMiniPageByPageId(pageId)
        pageInstance?.let {
            if (MiniPageManage.currentPage == it) {
                val usedHookName = (pageId.asDynamic() + hookName).unsafeCast<String>()
                runTimeHooks.trigger(usedHookName, *args)
            }
        }
    }

    /**
     * Listen for page-level events
     */
    fun tapWithPageId(hookName: String, pageId: Int, callBack: CallBack) {
        val usedHookName = (pageId.asDynamic() + hookName).unsafeCast<String>()
        runTimeHooks.off(usedHookName)
        runTimeHooks.on(usedHookName, callBack)
    }
}
