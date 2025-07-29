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

package com.tencent.kuikly.core.render.android.context

import com.tencent.kuikly.core.render.android.exception.IKuiklyRenderExceptionListener

/**
 * 渲染流程执行环境
 */
interface IKuiklyRenderContextHandler {

    /**
     * 初始化渲染执行环境
     * @param contextCode 执行上下文code
     */
    fun init(contextCode: String)

    /**
     * 销毁渲染执行环境
     */
    fun destroy()

    /**
     * 调用KTV KuiklyRenderContextMethod的方法
     * @param method KuiklyRenderContextMethod定义的方法
     * @param args 参数列表
     */
    fun call(method: KuiklyRenderContextMethod, args: List<Any?>)

    /**
     * 注册KTV侧调用Native的回调
     * @param callback KTV -> Native 回调
     */
    fun registerCallNative(callback: KuiklyRenderNativeMethodCallback)

    /**
     * 设置 Kotlin Bridge 状态监听
     */
    fun setBridgeStatusListener(listener: IKotlinBridgeStatusListener)

    /**
     * 设置异常监听
     */
    fun setRenderExceptionListener(listener: IKuiklyRenderExceptionListener?)

    companion object {
        const val CALL_ARGS_COUNT = 6
    }

}

/**
 * KTV执行环境暴露给Native调用的方法列表
 */
enum class KuiklyRenderContextMethod(value: Int) {
    KuiklyRenderContextMethodUnknown(0),
    KuiklyRenderContextMethodCreateInstance(1), // "createInstance" 方法
    KuiklyRenderContextMethodUpdateInstance(2), // "updateInstance" 方法
    KuiklyRenderContextMethodDestroyInstance(3), // "destroyInstance" 方法
    KuiklyRenderContextMethodFireCallback(4), // "fireCallback" 方法
    KuiklyRenderContextMethodFireViewEvent(5), // "fireViewEvent" 方法
    KuiklyRenderContextMethodLayoutView(6); // "layoutView" 方法
}

/**
 * Native暴露给KTV执行环境调用的方法列表
 */
enum class KuiklyRenderNativeMethod(val value: Int) {
    KuiklyRenderNativeMethodUnknown(0),
    KuiklyRenderNativeMethodCreateRenderView(1), // "createRenderView" 方法
    KuiklyRenderNativeMethodRemoveRenderView(2), // "removeRenderView" 方法
    KuiklyRenderNativeMethodInsertSubRenderView(3), // "insertSubRenderView" 方法
    KuiklyRenderNativeMethodSetViewProp(4), // "setViewProp" 方法
    KuiklyRenderNativeMethodSetRenderViewFrame(5), // "setRenderViewFrame" 方法
    KuiklyRenderNativeMethodCalculateRenderViewSize(6), // "calculateRenderViewSize" 方法
    KuiklyRenderNativeMethodCallViewMethod(7), // "callViewMethod" 方法
    KuiklyRenderNativeMethodCallModuleMethod(8), // "callModuleMethod" 方法
    KuiklyRenderNativeMethodCreateShadow(9), // "createShadow" 方法
    KuiklyRenderNativeMethodRemoveShadow(10), // "removeShadow" 方法
    KuiklyRenderNativeMethodSetShadowProp(11), // "setShadowProp" 方法
    KuiklyRenderNativeMethodSetShadowForView(12), // "setShadowForView" 方法
    KuiklyRenderNativeMethodSetTimeout(13), // "setTimeout方法"
    KuiklyRenderNativeMethodCallShadowMethod(14), // "callShadowModule方法"
    KuiklyRenderNativeMethodFireFatalException(15), // "fireFatalException方法"
    KuiklyRenderNativeMethodSyncFlushUI(16), // "syncFlushUI方法"
    KuiklyRenderNativeMethodCallTDFNativeMethod(17); // "callTDFModuleMethod"

    companion object {
        fun fromInt(value: Int): KuiklyRenderNativeMethod =
            values().firstOrNull { it.value == value } ?: KuiklyRenderNativeMethodUnknown
    }
}

typealias KuiklyRenderNativeMethodCallback = (methodId: KuiklyRenderNativeMethod, args: List<Any?>) -> Any?
