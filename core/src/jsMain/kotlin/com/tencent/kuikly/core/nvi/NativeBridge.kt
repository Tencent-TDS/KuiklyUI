package com.tencent.kuikly.core.nvi

import com.tencent.kuikly.core.callNative
import com.tencent.kuikly.core.manager.BridgeManager

/**
 * Created by kamlin on 2022/4/11.
 */
actual open class NativeBridge actual constructor() {

    private var pagerId: String = ""
    private var callNativeCallback: CallNativeCallback? = null

    actual fun toNative(
        methodId: Int,
        arg0: Any?,
        arg1: Any?,
        arg2: Any?,
        arg3: Any?,
        arg4: Any?,
        arg5: Any?
    ) : Any? {
        if (callNativeCallbackMap != null) { // 一旦callNativeCallbackMap不为空，则认为全部走新注册Native回调接口模式，原本旧CallNative接口则不需要再使用
            if (pagerId.isEmpty()) {
                pagerId = arg0 as String
            }
            if (callNativeCallback == null) {
                callNativeCallback = callNativeCallbackMap?.get(pagerId)
            }
            return callNativeCallback?.invoke(methodId, arg0, arg1, arg2, arg3, arg4, arg5)
        }
        return callNative(methodId, arg0, arg1, arg2, arg3, arg4, arg5)
    }

    actual fun destroy() {
        unRegisterCallNativeCallback(pagerId)
        callNativeCallback = null
    }

    companion object {
        private var callNativeCallbackMap: HashMap<String, CallNativeCallback>? = null
        fun registerCallNativeCallback(pagerId: String, callback: CallNativeCallback) {
            if (callNativeCallbackMap == null) {
                callNativeCallbackMap = hashMapOf()
            }
            callNativeCallbackMap?.put(pagerId, callback)
        }
        fun unRegisterCallNativeCallback(pagerId: String) {
            callNativeCallbackMap?.remove(pagerId)
        }
    }
}

typealias CallNativeCallback = (
    methodId: Int,
    arg0: Any?,
    arg1: Any?,
    arg2: Any?,
    arg3: Any?,
    arg4: Any?,
    arg5: Any?,
) -> Any?

@JsName(name = "registerCallNative")
@JsExport
@ExperimentalJsExport
/**
 * 新的注册回调Native接口
 * 注：该注册方式和旧全局js注入CallNative方式互斥，一旦使用该新接口注册，旧接口则不生效
 */
fun registerCallNative(pagerId: String, callback: CallNativeCallback) {
    NativeBridge.registerCallNativeCallback(pagerId, callback)
    if (!BridgeManager.containNativeBridge(pagerId)) {
        BridgeManager.registerNativeBridge(pagerId, NativeBridge())
    }
}