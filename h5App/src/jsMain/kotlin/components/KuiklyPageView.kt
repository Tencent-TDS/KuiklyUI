package com.tencent.kuikly.h5app.components

import com.tencent.kuikly.core.render.web.expand.KuiklyRenderViewDelegatorDelegate
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.SizeI
import com.tencent.kuikly.core.render.web.ktx.toJSONObjectSafely
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement

/**
 * Kuikly page view example
 */
class KuiklyPageView : IKuiklyRenderViewExport,
    KuiklyRenderViewDelegatorDelegate {
    private var kuiklyRenderView: KuiklyRenderView? = null
    private var pageName = ""
    private var pageData = "{}"
    private var loadSuccessCallback: KuiklyRenderCallback? = null
    private var loadFailureCallback: KuiklyRenderCallback? = null
    private val lazyEvents by lazy(LazyThreadSafetyMode.NONE) { mutableListOf<() -> Unit>() }
    private val divElement = document.createElement(ElementType.DIV)


    override val ele: HTMLDivElement
        get() = divElement.unsafeCast<HTMLDivElement>()

    private fun loadSuccessCallback(propValue: Any): Boolean {
        loadSuccessCallback = propValue as KuiklyRenderCallback
        return true
    }

    private fun loadFailure(propValue: Any): Boolean {
        loadFailureCallback = propValue as KuiklyRenderCallback
        return true
    }

    private fun performTaskWhenKuiklyViewDidLoad(callback: () -> Unit) {
        if (kuiklyRenderView != null) {
            callback()
        } else {
            lazyEvents.add(callback)
        }
    }

    private fun sendEventWithParams(params: String?) {
        val json = params.toJSONObjectSafely()
        val event = json.optString("event")
        val data = json.optJSONObject("data") ?: JSONObject()
        performTaskWhenKuiklyViewDidLoad {
            kuiklyRenderView?.sendEvent(event, data.toMap())
        }
    }

    private fun getHostPageData(): Map<String, Any> = mapOf()

    private fun performAllLazyTasks() {
        lazyEvents.forEach {
            it()
        }
        lazyEvents.clear()
    }

    private fun initQQKuiklyViewIfNeed() {
        if (kuiklyRenderView != null) {
            return
        }

        // Container size
        val containerWidth = window.innerWidth
        val containerHeight = window.innerHeight
        if (pageName.isNotEmpty()) {
            val hostPageData = mutableMapOf<String, Any>().apply {
                putAll(getHostPageData())
            }
            hostPageData.putAll(pageData.toJSONObjectSafely().toMap())
            kuiklyRenderView = KuiklyRenderView(this).apply {
                // After view initialization is complete, create container
                onAttach(ele, pageName, mapOf(), SizeI(containerWidth, containerHeight))
                // Execute delayed tasks
                performAllLazyTasks()
            }
        }
    }

    override fun onAddToParent(parent: Element) {
        super.onAddToParent(parent)

        // Page load complete, start loading View
        initQQKuiklyViewIfNeed()
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            "loadSuccess" -> loadSuccessCallback(propValue)
            "loadFailure" -> loadFailure(propValue)
            "pageName" -> {
                pageName = propValue as String
                true
            }

            "pageData" -> {
                pageData = propValue as String
                true
            }

            else -> super.setProp(propKey, propValue)
        }
    }

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            "sendEvent" -> sendEventWithParams(params)
            else -> super.call(method, params, callback)
        }
    }

    companion object {
        const val VIEW_NAME = "KuiklyPageView"
    }
}
