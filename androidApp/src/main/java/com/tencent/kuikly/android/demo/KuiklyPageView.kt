/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.android.demo

import android.content.Context
import android.util.Size
import android.view.ViewGroup
import android.widget.FrameLayout
import com.tencent.kuikly.core.render.android.context.KuiklyRenderCoreExecuteModeBase
import com.tencent.kuikly.core.render.android.css.ktx.frame
import com.tencent.kuikly.core.render.android.css.ktx.toJSONObjectSafely
import com.tencent.kuikly.core.render.android.css.ktx.toMap
import com.tencent.kuikly.core.render.android.exception.ErrorReason
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegatorDelegate
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import org.json.JSONObject

/**
 * Created by kam on 2023/9/22.
 */
class KuiklyPageView(context: Context) : FrameLayout(context), IKuiklyRenderViewExport,
    KuiklyRenderViewBaseDelegatorDelegate {

    private var kuiklyRenderView: KuiklyRenderView? = null
    private var pageName = ""
    private var pageData = "{}"
    private var loadSuccessCallback: KuiklyRenderCallback? = null
    private var loadFailureCallback: KuiklyRenderCallback? = null

    private val lazyEvents by lazy(LazyThreadSafetyMode.NONE) { mutableListOf<() -> Unit>() }

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

    override fun onPageLoadComplete(
        isSucceed: Boolean,
        errorReason: ErrorReason?,
        executeMode: KuiklyRenderCoreExecuteModeBase,
    ) {
        if (isSucceed && loadSuccessCallback != null) {
            loadSuccessCallback?.invoke(mapOf<String, Any>())
        }
        if (!isSucceed && loadFailureCallback != null) {
            loadSuccessCallback?.invoke(mapOf<String, Any>())
        }
    }

    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        super.setLayoutParams(params)
        params?.also {
            kuiklyRenderView?.layoutParams = LayoutParams(it.width, it.height)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        initKuiklyViewIfNeed()
    }

    override fun onDestroy() {
        super.onDestroy()
        kuiklyRenderView?.onDetach()
    }

    private fun initKuiklyViewIfNeed() {
        if (kuiklyRenderView != null) {
            return
        }

        val currentFrame = frame
        if (currentFrame.right > 0 && currentFrame.bottom > 0 && pageName.isNotEmpty()) {
            val hostPageData = mutableMapOf<String, Any>().apply {
                putAll(getHostPageData())
            }
            hostPageData.putAll(pageData.toJSONObjectSafely().toMap())
            kuiklyRenderView = KuiklyRenderView(context, this).apply {
                this@KuiklyPageView.addView(this)
                onAttach("", pageName, mapOf(), Size(currentFrame.right, currentFrame.bottom))
                performAllLazyTasks()
            }
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

    private fun performAllLazyTasks() {
        lazyEvents.forEach {
            it()
        }
        lazyEvents.clear()
    }

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

    private fun getHostPageData(): Map<String, Any> {
        val act = activity
//        if (act is PublicFragmentActivity) {
//            return (act.fragment as? KuiklyFragment)?.pageData ?: mapOf()
//        }
        return mapOf()
    }

    companion object {
        const val VIEW_NAME = "KuiklyPageView"
    }

}