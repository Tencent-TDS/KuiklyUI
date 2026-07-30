package com.tencent.kuikly.demo.pages.app.lang

import com.tencent.kuikly.core.module.CallbackRef
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.module.SharedPreferencesModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.demo.pages.base.BasePager

internal abstract class MultiLingualPager : BasePager() {
    var resStrings by observable(LangManager.getCurrentResStrings())
    private lateinit var langEventCallbackRef: CallbackRef

    override fun created() {
        super.created()

        // 获取当前语言
        val lang = acquireModule<SharedPreferencesModule>(SharedPreferencesModule.MODULE_NAME)
            .getString(LangManager.KEY_PREF_LANGUAGE)
            .takeUnless { it.isEmpty() } ?: pagerData.params.optString(LangManager.KEY_PARAM_SYSTEM_LANGUAGE)

        // 切换全局语言并更新当切页面的字符串资源
        LangManager.changeLanguage(lang)
        resStrings = LangManager.getCurrentResStrings()

        // 注册语言切换事件监听
        langEventCallbackRef = acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .addNotify(LangManager.LANG_CHANGED_EVENT) { _ ->
                langEventCallbackFn()
            }
    }

    open fun langEventCallbackFn() {
        resStrings = LangManager.getCurrentResStrings()
    }

    override fun pageWillDestroy() {
        super.pageWillDestroy()
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .removeNotify(LangManager.LANG_CHANGED_EVENT, langEventCallbackRef)
    }
}