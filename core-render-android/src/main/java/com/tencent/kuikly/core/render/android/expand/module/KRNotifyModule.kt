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

package com.tencent.kuikly.core.render.android.expand.module

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.tencent.kuikly.core.render.android.css.ktx.isMainThread
import com.tencent.kuikly.core.render.android.expand.module.KRNotifyModule.Companion.BROADCAST_PERMISSION
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import org.json.JSONObject

/**
 * Created by kam on 2023/3/22.
 */
open class KRNotifyModule : KuiklyRenderBaseModule() {

    private val toHRMap: MutableMap<String, MutableList<HRCallbackWrapper>> = mutableMapOf()
    private var notifyBroadcastReceiver: HRNotifyModuleReceiver? = null

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            "addNotify" -> addNotify(params, callback)
            "removeNotify" -> removeNotify(params)
            "postNotify" -> postNotify(params)
            else -> super.call(method, params, callback)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterNotifyModuleReceiver()
    }

    private fun dispatchEvent(eventName: String, data: String) {
        toHRMap[eventName]?.forEach {
            it.callback.invoke(data)
        }
    }

    private fun addNotify(params: String?, callback: KuiklyRenderCallback?) {
        val cb = callback ?: return
        val jsonStr = params ?: return
        val jsonObject = JSONObject(jsonStr)
        val eventName = jsonObject.optString(KEY_EVENT_NAME) ?: return
        val id = jsonObject.optString(KEY_ID) ?: return

        var callbackList: MutableList<HRCallbackWrapper>? = toHRMap[eventName]
        if (callbackList == null) {
            callbackList = mutableListOf()
            toHRMap[eventName] = callbackList
        }
        callbackList.add(HRCallbackWrapper(id, cb))
        registerNotifyModuleReceiver(eventName, jsonObject)
    }

    private fun postNotify(params: String?) {
        val jsonStr = params ?: return
        val jsonObject = JSONObject(jsonStr)
        val eventName = jsonObject.optString(KEY_EVENT_NAME) ?: return
        val data = jsonObject.optJSONObject(KEY_DATA) ?: JSONObject()
        sendKuiklyEvent(eventName, data)
    }

    protected open fun sendKuiklyEvent(eventName: String, data: JSONObject) {
        context?.sendKuiklyEvent(eventName, data)
    }

    private fun removeNotify(params: String?) {
        val jsonStr = params ?: return
        val jsonObject = JSONObject(jsonStr)
        val eventName = jsonObject.optString(KEY_EVENT_NAME) ?: return
        val id = jsonObject.optString(KEY_ID) ?: return
        toHRMap[eventName]?.also {
            val size = it.size
            for (i in 0 until size) {
                val wrapper = it[i]
                if (wrapper.callbackId == id) {
                    it.removeAt(i)
                    break
                }
            }
            if (it.isEmpty()) {
                toHRMap.remove(eventName)
            }
        }
    }

    private fun unregisterNotifyModuleReceiver() {
        if (notifyBroadcastReceiver != null) {
            context?.applicationContext?.unregisterKuiklyBroadcastReceiver(notifyBroadcastReceiver!!)
            notifyBroadcastReceiver = null
        }
    }

    protected open fun registerNotifyModuleReceiver(event: String, params: JSONObject) {
        if (notifyBroadcastReceiver == null) {
            notifyBroadcastReceiver = HRNotifyModuleReceiver {
                val eventName = it.getStringExtra(KEY_EVENT_NAME) ?: ""
                val data = it.getStringExtra(KEY_DATA) ?: "{}"
                dispatchEvent(eventName, data)
            }
            context?.applicationContext?.registerKuiklyBroadcastReceiver(notifyBroadcastReceiver!!)
        }
    }

    companion object {
        const val MODULE_NAME = "KRNotifyModule"
        const val KEY_EVENT_NAME = "eventName"
        const val KEY_DATA = "data"
        private const val KEY_ID = "id"
        const val BROADCAST_RECEIVER_ACTION = "com.tencent.kuikly.broadcast.hr.notify"
        internal val Context.BROADCAST_PERMISSION
            get() = "${applicationContext.packageName}.permission.KUIKLY_NOTIFY"
    }
}

private class HRNotifyModuleReceiver(private val callback: (intent: Intent) -> Unit) :
    BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val it = intent ?: return
        if (isMainThread()) {
            callback.invoke(it)
        } else {
            Handler(Looper.getMainLooper()).post {
                callback.invoke(it)
            }
        }
    }

}

private data class HRCallbackWrapper(
    val callbackId: String,
    val callback: KuiklyRenderCallback
)

fun Context.registerKuiklyBroadcastReceiver(receiver: BroadcastReceiver) {
    val filter = IntentFilter(KRNotifyModule.BROADCAST_RECEIVER_ACTION)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        registerReceiver(receiver, filter, BROADCAST_PERMISSION, null, Context.RECEIVER_NOT_EXPORTED)
    } else {
        registerReceiver(receiver, filter, BROADCAST_PERMISSION, null)
    }
}

fun Context.unregisterKuiklyBroadcastReceiver(receiver: BroadcastReceiver) {
    unregisterReceiver(receiver)
}

fun Context.sendKuiklyEvent(eventName: String, `data`: JSONObject) {
    sendKREvent(eventName, `data`.toString())
}

fun Intent.getKuiklyEventParams(): JSONObject {
    return JSONObject(getStringExtra(KRNotifyModule.KEY_DATA) ?: "{}")
}

fun Intent.getKuiklyEventName(): String = getStringExtra(KRNotifyModule.KEY_EVENT_NAME) ?: ""

private fun Context.sendKREvent(eventName: String, data: String) {
    val intent = Intent(KRNotifyModule.BROADCAST_RECEIVER_ACTION).apply {
        setPackage(applicationContext.packageName)
        putExtra(KRNotifyModule.KEY_EVENT_NAME, eventName)
        putExtra(KRNotifyModule.KEY_DATA, data)
    }
    sendBroadcast(intent, BROADCAST_PERMISSION)
}