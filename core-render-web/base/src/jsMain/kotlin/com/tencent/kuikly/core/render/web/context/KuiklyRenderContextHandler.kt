package com.tencent.kuikly.core.render.web.context

import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.add
import com.tencent.kuikly.core.render.web.collection.array.fifthArg
import com.tencent.kuikly.core.render.web.collection.array.firstArg
import com.tencent.kuikly.core.render.web.collection.array.fourthArg
import com.tencent.kuikly.core.render.web.collection.array.secondArg
import com.tencent.kuikly.core.render.web.collection.array.sixthArg
import com.tencent.kuikly.core.render.web.collection.array.thirdArg
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderNativeMethodCallback
import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import com.tencent.kuikly.core.render.web.ktx.toJSONArray
import com.tencent.kuikly.core.render.web.ktx.toJSONObject
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.render.web.utils.Log

/**
 * Handler for rendering process execution in web native environment, used to register methods
 * for kuikly side and native side to call each other
 */
class KuiklyRenderContextHandler : IKuiklyRenderContextHandler {
    // Callback method for native calls
    private var callNativeCallback: KuiklyRenderNativeMethodCallback? = null

    /**
     * Initialize context, register methods for kotlin side to call native side
     */
    override fun init(url: String?, pageId: String) {
        // Register callNative global method for JS environment, for kotlin to call Native
        // Using multi-instance callNative registration method from core, passing instanceId
        // to core for single page multi-instance distinction
        try {
            // Web host registers native communication interface for kuikly to call
            kuiklyWindow.asDynamic().com.tencent.kuikly.core.nvi.registerCallNative(pageId, ::callNative)
        } catch (e: dynamic) {
            // Call error
            Log.error("registerCallNative error, reason is: $e")
        }
    }

    /**
     * Destroy context handler
     */
    override fun destroy() {
        // Remove registered global native call method
        // globalThis[METHOD_NAME_CALL_NATIVE] = null
        // No need to handle in web environment, TODO for mini program
    }

    /**
     * Call methods registered in kuikly core from native side
     */
    override fun call(method: KuiklyRenderContextMethod, args: JsArray<Any?>) {
        // 1. Convert objects or arrays in list to json string, e.g. {"a":1}, ["1"]
        // Also convert if input is json object or jsonArray
        var argsList: JsArray<Any?> = JsArray()
        args.forEach { arg ->
            when (arg) {
                is Map<*, *> -> {
                    argsList.add(arg.unsafeCast<Map<String, Any>>().toJSONObject().toString())
                }

                is List<*> -> {
                    argsList.add(arg.unsafeCast<List<Any>>().toJSONArray().toString())
                }

                is JSONObject -> {
                    argsList.add(arg.toString())
                }

                is JSONArray -> {
                    argsList.add(arg.toString())
                }

                else -> {
                    argsList.add(arg)
                }
            }
        }

        // 2. Ensure parameter list length is 6
        argsList = if (argsList.length >= CALL_ARGS_COUNT) {
            // Truncate if exceeds
            argsList.slice(0, CALL_ARGS_COUNT)
        } else {
            // Pad with null if insufficient
            val appendArgCount = CALL_ARGS_COUNT - argsList.length
            for (i in 0 until appendArgCount) {
                argsList.add(null)
            }
            argsList
        }

        // 3. Call method registered globally for native to call kuikly side
        kuiklyWindow.asDynamic()[METHOD_NAME_CALL_KOTLIN](
            // Use enum ordinal value directly
            method.ordinal,
            // Parameter values
            argsList.firstArg(),
            argsList.secondArg(),
            argsList.thirdArg(),
            argsList.fourthArg(),
            argsList.fifthArg(),
            argsList.sixthArg()
        )
    }

    /**
     * Register actual callback method for kotlin to call native
     */
    override fun registerCallNative(callback: KuiklyRenderNativeMethodCallback) {
        callNativeCallback = callback
    }

    /**
     * After compilation to JS code, maintain callNative name for assignment to global Global object,
     * to call web native capabilities
     */
    @JsName("callNative")
    fun callNative(
        methodId: Int,
        arg0: Any?,
        arg1: Any?,
        arg2: Any?,
        arg3: Any?,
        arg4: Any?,
        arg5: Any?,
    ): Any? {
        return callNativeCallback?.invoke(
            KuiklyRenderNativeMethod.fromInt(methodId), JsArray(
                arg0,
                arg1,
                arg2,
                arg3,
                arg4,
                arg5,
            )
        )
    }

    companion object {
        private const val CALL_ARGS_COUNT = 6
        private const val METHOD_NAME_CALL_NATIVE = "callNative"
        private const val METHOD_NAME_CALL_KOTLIN = "callKotlinMethod"
    }
}
