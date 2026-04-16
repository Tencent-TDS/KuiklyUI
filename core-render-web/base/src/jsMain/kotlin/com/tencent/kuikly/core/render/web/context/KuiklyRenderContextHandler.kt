package com.tencent.kuikly.core.render.web.context

import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.add
import com.tencent.kuikly.core.render.web.collection.array.fifthArg
import com.tencent.kuikly.core.render.web.collection.array.firstArg
import com.tencent.kuikly.core.render.web.collection.array.fourthArg
import com.tencent.kuikly.core.render.web.collection.array.secondArg
import com.tencent.kuikly.core.render.web.collection.array.sixthArg
import com.tencent.kuikly.core.render.web.collection.array.thirdArg
import com.tencent.kuikly.core.render.web.const.KRExtraConst.WEB_DECREASE_CALLKOTLIN_ID_METHOD
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
        // handle multi-closure registerCallNative before calling it,
        // so that the broadcast interception is set up first
        handleMultiInstanceRegisterCallNative()
        // Register callNative global method for JS environment, for kotlin to call Native
        // Using multi-instance callNative registration method from core, passing instanceId
        // to core for single page multi-instance distinction
        try {
            // Web host registers native communication interface for kuikly to call.
            // After handleMultiInstanceRegisterCallNative(), this call triggers the broadcast
            // function which dispatches to all known closures' registerCallNative, ensuring
            // the correct BridgeManager receives the registration regardless of closure load order.
            kuiklyWindow.asDynamic().com.tencent.kuikly.core.nvi.registerCallNative(pageId, ::callNative)
            // save pageId map
            instanceIdMap[pageId] = instanceId
        } catch (e: dynamic) {
            // Call error
            Log.error("registerCallNative error, reason is: $e")
        }
        // handle multiple instances
        handleMultiInstanceCallKotlinMethod()
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

    /**
     * Handle multi-closure registerCallNative problem.
     *
     * Uses Object.defineProperty to intercept assignments to registerCallNative on the kuikly
     * namespace. Each time a new closure loads and assigns its own registerCallNative, the setter
     * saves it under a unique key. The getter returns a broadcast function that calls ALL saved
     * functions, so the correct closure's BridgeManager always receives the registration.
     *
     * IMPORTANT: Cannot use a global once-flag (like a companion var) because each nativevue
     * bundle load replaces the entire window.com.tencent.kuikly.core.nvi namespace object via
     * its UMD wrapper. When the namespace is replaced, the previous defineProperty is lost.
     * We therefore check via Object.getOwnPropertyDescriptor whether the getter is already
     * installed on the *current* namespace object, and re-install if not.
     */
    fun handleMultiInstanceRegisterCallNative() {
        val win = kuiklyWindow.asDynamic()
        val namespace = win.com.tencent.kuikly.core.nvi

        if (isNullOrUndefined(namespace)) {
            return
        }

        // Check if getter is already installed on the current namespace object.
        // Must NOT use a global flag: the namespace object is replaced on every new nativevue
        // bundle load, so we detect replacement and re-install the descriptor each time.
        if (isDescriptorInstalled(namespace, "registerCallNative")) {
            return
        }

        // Not installed on current namespace: save current value (if any) into next slot,
        // then install the broadcast descriptor.
        // registerCallNativeId starts at -1, so first ++registerCallNativeId yields 0,
        // and subsequent setter calls continue incrementing from there — no slot scanning needed.
        val currentValue = namespace.registerCallNative
        if (!isNullOrUndefined(currentValue)) {
            win[REGISTER_CALL_NATIVE_FN_KEY + "_" + (++registerCallNativeId)] = currentValue
        }

        // broadcast function: iterate all saved fn slots and call each one
        val broadcast: (String, dynamic) -> Unit = { pagerId, callback ->
            var i = 0
            while (true) {
                val fn = win[REGISTER_CALL_NATIVE_FN_KEY + "_" + i]
                if (isNullOrUndefined(fn)) break
                try {
                    fn(pagerId, callback)
                } catch (e: dynamic) {
                    Log.error("registerCallNative broadcast[$i] error: $e")
                }
                i++
            }
        }

        // property descriptor
        val descriptor = js("{}")
        descriptor.get = { broadcast }
        descriptor.set = { newValue: dynamic ->
            // Triggered when a new nativevue closure assigns its registerCallNative.
            // Save it under the next id so the broadcast function can reach it.
            if (jsTypeOf(newValue) == "function") {
                win[REGISTER_CALL_NATIVE_FN_KEY + "_" + (++registerCallNativeId)] = newValue
            }
        }
        descriptor.configurable = true
        descriptor.enumerable = true

        js("Object").defineProperty(namespace, "registerCallNative", descriptor)
    }


    /**
     * handle multi-instance callKotlinMethod
     *
     * use Object.defineProperty to listen callKotlinMethod set,
     * then save new callKotlinMethod to callKotlinMethod_id(auto increment id).
     * then create dispatch method to dispatch callKotlinMethod
     */
    fun handleMultiInstanceCallKotlinMethod() {
        if (isInitMultiInstanceCallKotlinMethod) {
            // execute once, when re init context handle. do not handle multi-instance callKotlinMethod
            return
        }

        val win = kuiklyWindow.asDynamic()
        val methodName = METHOD_NAME_CALL_KOTLIN
        val currentValue = win[methodName]
        // no callKotlinMethod yet: do NOT set the flag so we retry on the next init call
        if (isNullOrUndefined(currentValue)) {
            return
        }

        // Mark as installed only after confirming the value exists, mirroring the
        // registerCallNative approach of not locking out a future retry prematurely.
        isInitMultiInstanceCallKotlinMethod = true

        // save old callKotlinMethod to callKotlinMethod_0
        win[methodName + "_" + instanceId] = currentValue

        // create dispatch method：use second arg（instanceId）to call special callKotlinMethod_id
        val dispatch: (Int, dynamic, dynamic, dynamic, dynamic, dynamic, dynamic) -> dynamic =
            { methodOrdinal, arg0, arg1, arg2, arg3, arg4, arg5 ->
                // second arg for instance id，use current instance id to call special callKotlinMethod_id
                val targetKey = methodName + "_" + instanceIdMap[arg0]
                val targetMethod = win[targetKey]
                val defaultMethod = win[methodName + "_${instanceId}"]
                if (jsTypeOf(targetMethod) == "function") {
                    targetMethod(methodOrdinal, arg0, arg1, arg2, arg3, arg4, arg5)
                } else if (jsTypeOf(defaultMethod) == "function") {
                    // use default callKotlinMethod for single instance or single page multi-instance
                    defaultMethod(methodOrdinal, arg0, arg1, arg2, arg3, arg4, arg5)
                } else {
                    undefined
                }
            }

        // property descriptor
        val descriptor = js("{}")
        descriptor.get = { dispatch }
        descriptor.set = { newValue: dynamic ->
            if (jsTypeOf(newValue) == "function") {
                val id = ++instanceId
                win[methodName + "_" + id] = newValue
            }
        }
        descriptor.configurable = true
        descriptor.enumerable = true

        // Use Object.defineProperty interception the property
        js("Object").defineProperty(win, methodName, descriptor)

        // expose method to decrease instance id
        win[WEB_DECREASE_CALLKOTLIN_ID_METHOD] = {
            instanceId--
        }
    }

    /**
     * Returns true if the given value is null or JS undefined.
     */
    private fun isNullOrUndefined(value: dynamic): Boolean =
        value == null || jsTypeOf(value) == "undefined"

    /**
     * Returns true if a getter-based property descriptor is already installed on [obj]
     * for the given [propName], indicating that defineProperty has been called previously.
     */
    private fun isDescriptorInstalled(obj: dynamic, propName: String): Boolean {
        val desc = js("Object").getOwnPropertyDescriptor(obj, propName)
        return desc != null && jsTypeOf(desc.get) == "function"
    }

    companion object {
        private const val CALL_ARGS_COUNT = 6
        private const val METHOD_NAME_CALL_NATIVE = "callNative"
        private const val METHOD_NAME_CALL_KOTLIN = "callKotlinMethod"
        private const val REGISTER_CALL_NATIVE_FN_KEY = "__kuikly_registerCallNative_fn"
        private var isInitMultiInstanceCallKotlinMethod = false
        // auto increment id for registerCallNative across closures; starts at -1 so first ++id yields slot 0
        private var registerCallNativeId = -1
        // multi instance auto increment id
        private var instanceId = 0
        private var instanceIdMap = mutableMapOf<String, Int>()
    }
}
