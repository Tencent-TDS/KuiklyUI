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

package com.tencent.kuikly.core.manager

import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.collection.fastLinkedMapOf
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.nvi.NativeBridge
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.IPager
import com.tencent.kuikly.core.pager.Pager

interface IBridgeCallObserver {
    fun onCallNative(methodId: Int, vararg args: Any?)
    fun onCallKotlin(methodId: Int, vararg args: Any?)
}

internal fun IBridgeCallObserver.safeOnCallNative(methodId: Int, vararg args: Any?) {
    try {
        onCallNative(methodId, *args)
    } catch (_: Throwable) {}
}

internal fun IBridgeCallObserver.safeOnCallKotlin(methodId: Int, vararg args: Any?) {
    try {
        onCallKotlin(methodId, *args)
    } catch (_: Throwable) {}
}

object BridgeManager {

    private const val TAG = "BridgeManager"
    private var didInit = false

    /**
     * currentPageId的值仅在Pager上下文中有效，错误使用可能导致observable不更新、PagerNotFoundException、
     * ReactiveObserverNotFoundException等问题，建议使用Pager上下文的pagerId代替。
     * @see Pager.pagerId
     * @see DeclarativeBaseView.pagerId
     * @see Attr.pagerId
     * @see Event.pagerId
     */
    @Deprecated("使用Pager上下文的pagerId代替")
    var currentPageId : String = ""

    private val nativeBridgeMap = fastLinkedMapOf<String, NativeBridge>()
    private val callObserverMap = fastLinkedMapOf<String, IBridgeCallObserver>()

    fun isDidInit(): Boolean {
        return didInit
    }

    fun init() {
        didInit = true
    }

    fun registerNativeBridge(instanceId: String, nativeBridge: NativeBridge) {
        nativeBridgeMap[instanceId] = nativeBridge
    }

    fun containNativeBridge(instanceId: String): Boolean {
        return nativeBridgeMap.containsKey(instanceId)
    }

    fun isPageExist(pageName: String): Boolean {
        return PagerManager.isPagerCreatorExist(pageName)
    }

    fun registerPageRouter(pageName: String, creator: () -> IPager) {
        PagerManager.registerPageRouter(pageName, creator)
    }

    fun addCallObserver(observer: IBridgeCallObserver) {
        callObserverMap[currentPageId] = observer
    }

    fun removeCallObserver() {
        callObserverMap.remove(currentPageId)
    }

    fun callKotlinMethod(
        methodId: Int,
        arg0: Any? = null,
        arg1: Any? = null,
        arg2: Any? = null,
        arg3: Any? = null,
        arg4: Any? = null,
        arg5: Any? = null
    ) {
        currentPageId = arg0 as String
        callObserverMap[currentPageId]?.safeOnCallKotlin(methodId, arg0, arg1, arg2, arg3, arg4, arg5)
        when (methodId) {
            KotlinMethod.CREATE_INSTANCE -> {
                PagerManager.createPager(
                    arg0 as String,
                    arg1 as String,
                    arg2 as String
                )
            }
            KotlinMethod.UPDATE_INSTANCE -> {
                PagerManager.firePagerEvent(arg0 as String, arg1 as String, arg2 as String)
            }
            KotlinMethod.DESTROY_INSTANCE -> {
                val instanceId = arg0 as String
                PagerManager.destroyPager(instanceId)
                nativeBridgeMap.remove(instanceId)?.destroy()
            }
            KotlinMethod.FIRE_CALLBACK -> {
                PagerManager.fireCallBack(arg0 as String, arg1 as String, arg2)
            }
            KotlinMethod.FIRE_VIEW_EVENT -> {
                PagerManager.fireViewEvent(
                    arg0 as String,
                    arg1 as Int,
                    arg2 as String,
                    arg3 as? String
                )
            }
            KotlinMethod.LAYOUT_VIEW -> {
                PagerManager.fireLayoutView(arg0 as String)
            }
            else -> {
                KLog.e(
                    TAG,
                    "[callKotlinMethod]:call method failed. methodId: $methodId"
                )
            }
        }
    }

    private fun callNativeMethod(
        methodId: Int,
        arg0: Any? = null,
        arg1: Any? = null,
        arg2: Any? = null,
        arg3: Any? = null,
        arg4: Any? = null,
        arg5: Any? = null
    ): Any? {
        callObserverMap[currentPageId]?.safeOnCallNative(methodId, arg0, arg1, arg2, arg3, arg4, arg5)
        return nativeBridgeMap[arg0 as String]?.toNative(
            methodId,
            arg0,
            arg1,
            arg2,
            arg3,
            arg4,
            arg5
        )
    }

    fun createRenderView(instanceId: String, tag: Int, viewName: String) {
        callNativeMethod(NativeMethod.CREATE_RENDER_VIEW, instanceId, tag, viewName)
    }

    fun removeRenderView(instanceId: String, tag: Int) {
        callNativeMethod(NativeMethod.REMOVE_RENDER_VIEW, instanceId, tag)
    }

    fun insertSubRenderView(instanceId: String, parentTag: Int, childTag: Int, index: Int) {
        callNativeMethod(
            NativeMethod.INSERT_SUB_RENDER_VIEW,
            instanceId,
            parentTag,
            childTag,
            index
        )
    }

    fun setViewProp(
        instanceId: String,
        tag: Int,
        propKey: String,
        propValue: Any,
        isEvent: Int,
        sync: Int = 0
    ) {
        callNativeMethod(NativeMethod.SET_VIEW_PROP, instanceId, tag, propKey, propValue, isEvent, sync)
    }

    fun setRenderViewFrame(
        instanceId: String,
        tag: Int,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        callNativeMethod(NativeMethod.SET_RENDER_VIEW_FRAME, instanceId, tag, x, y, width, height)
    }

    fun calculateRenderViewSize(
        instanceId: String,
        tag: Int,
        width: Float,
        height: Float
    ): String {
        val s = callNativeMethod(
            NativeMethod.CALCULATE_RENDER_VIEW_SIZE,
            instanceId,
            tag,
            width,
            height
        )
        return if (s == null) {
            ""
        } else {
            s as String
        }
    }

    fun callViewMethod(
        instanceId: String,
        tag: Int,
        method: String,
        params: String? = null,
        callbackId: String? = null
    ) {
        callNativeMethod(NativeMethod.CALL_VIEW_METHOD, instanceId, tag, method, params, callbackId)
    }

    fun callExceptionMethod(exception: String) {
        callNativeMethod(
            NativeMethod.FIRE_FATAL_EXCEPTION,
            currentPageId,
            exception
        )
    }

    fun callModuleMethod(
        instanceId: String,
        moduleName: String,
        method: String,
        params: Any? = null,
        callbackId: String? = null,
        syncCall: Int = 0
    ) : Any? {
        return callNativeMethod(
            NativeMethod.CALL_MODULE_METHOD,
            instanceId,
            moduleName,
            method,
            params,
            callbackId,
            syncCall
        )
    }

    fun callTDFModuleMethod(
        instanceId: String,
        moduleName: String,
        method: String,
        params: String? = null,
        succCallbackId: String? = null,
        errorCallbackId: String? = null,
        syncCall: Int = 0
    ): Any? {
        val cbsJson = JSONObject()
        succCallbackId?.let {
            cbsJson.put("succ", it)
        }
        errorCallbackId?.let {
            cbsJson.put("error", it)
        }

        return callNativeMethod(
            NativeMethod.CALL_TDF_MODULE_METHOD,
            instanceId,
            moduleName,
            method,
            params,
            cbsJson.toString(),
            syncCall
        )
    }

    fun createShadow(instanceId: String, tag: Int, viewName: String) {
        callNativeMethod(NativeMethod.CREATE_SHADOW, instanceId, tag, viewName)
    }

    fun removeShadow(instanceId: String, tag: Int) {
        callNativeMethod(NativeMethod.REMOVE_SHADOW, instanceId, tag)
    }

    fun setShadowProp(
        instanceId: String,
        tag: Int,
        propKey: String,
        propValue: Any
    ) {
        callNativeMethod(NativeMethod.SET_SHADOW_PROP, instanceId, tag, propKey, propValue)
    }

    fun setShadowForView(
        instanceId: String,
        tag: Int
    ) {
        callNativeMethod(NativeMethod.SET_SHADOW_FOR_VIEW, instanceId, tag)
    }

    fun setTimeout(instanceId: String, delayTimeMs: Float, callbackId: String) {
        callNativeMethod(NativeMethod.SET_TIMEOUT, instanceId, delayTimeMs, callbackId)
    }

    fun callShadowMethod(
        instanceId: String,
        tag: Int,
        method: String,
        params: String? = null
    ) : Any? {
        return callNativeMethod(NativeMethod.CALL_SHADOW_METHOD, instanceId, tag, method, params)
    }

    fun callSyncFlushUIMethod(instanceId: String) {
        callNativeMethod(NativeMethod.SYNC_FLUSH_UI, instanceId)
    }

}

object KotlinMethod {
    const val CREATE_INSTANCE = 1  // "createInstance" 方法
    const val UPDATE_INSTANCE = 2  // "updateInstance" 方法
    const val DESTROY_INSTANCE = 3 // "destroyInstance" 方法
    const val FIRE_CALLBACK = 4    // "fireCallback" 方法
    const val FIRE_VIEW_EVENT = 5  // "fireViewEvent" 方法
    const val LAYOUT_VIEW = 6  // "layoutView" 方法
}

object NativeMethod {
    const val CREATE_RENDER_VIEW = 1  // "createRenderView" 方法
    const val REMOVE_RENDER_VIEW = 2  // "removeRenderView" 方法
    const val INSERT_SUB_RENDER_VIEW = 3  // "insertSubRenderView" 方法
    const val SET_VIEW_PROP = 4  // "insertSubRenderView" 方法
    const val SET_RENDER_VIEW_FRAME = 5  // "setRenderViewFrame" 方法
    const val CALCULATE_RENDER_VIEW_SIZE = 6 // "calculateRenderViewSize" 方法
    const val CALL_VIEW_METHOD = 7 // "callViewMethod" 方法
    const val CALL_MODULE_METHOD = 8 // "callModuleMethod" 方法
    const val CREATE_SHADOW = 9    // "createShadow" 方法
    const val REMOVE_SHADOW = 10  // "removeShadow" 方法
    const val SET_SHADOW_PROP = 11  // "setShadowProp" 方法
    const val SET_SHADOW_FOR_VIEW = 12  // "setShadowForView" 方法
    const val SET_TIMEOUT = 13 // "setTimeout" 方法
    const val CALL_SHADOW_METHOD = 14 // "callShadowModule" 方法
    const val FIRE_FATAL_EXCEPTION = 15 // "fireFatalException" 方法
    const val SYNC_FLUSH_UI = 16 // "syncFlushUI" 方法
    const val CALL_TDF_MODULE_METHOD = 17 // "callTDFModuleMethod" 方法
}
