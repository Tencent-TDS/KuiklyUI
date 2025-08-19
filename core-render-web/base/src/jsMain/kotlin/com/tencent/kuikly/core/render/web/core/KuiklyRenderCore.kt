package com.tencent.kuikly.core.render.web.core

import com.tencent.kuikly.core.render.web.IKuiklyRenderView
import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.fifthArg
import com.tencent.kuikly.core.render.web.collection.array.fourthArg
import com.tencent.kuikly.core.render.web.collection.array.get
import com.tencent.kuikly.core.render.web.collection.array.secondArg
import com.tencent.kuikly.core.render.web.collection.array.sixthArg
import com.tencent.kuikly.core.render.web.collection.array.thirdArg
import com.tencent.kuikly.core.render.web.collection.map.JsMap
import com.tencent.kuikly.core.render.web.collection.map.get
import com.tencent.kuikly.core.render.web.collection.map.set
import com.tencent.kuikly.core.render.web.context.KuiklyRenderContextHandler
import com.tencent.kuikly.core.render.web.context.KuiklyRenderContextMethod
import com.tencent.kuikly.core.render.web.context.KuiklyRenderNativeMethod
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderModuleExport
import com.tencent.kuikly.core.render.web.ktx.Frame
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderNativeMethodCallback
import com.tencent.kuikly.core.render.web.ktx.SizeF
import com.tencent.kuikly.core.render.web.ktx.height
import com.tencent.kuikly.core.render.web.ktx.width
import com.tencent.kuikly.core.render.web.layer.IKuiklyRenderLayerHandler
import com.tencent.kuikly.core.render.web.layer.KuiklyRenderLayerHandler
import com.tencent.kuikly.core.render.web.processor.KuiklyProcessor
import com.tencent.kuikly.core.render.web.scheduler.IKuiklyRenderCoreScheduler
import com.tencent.kuikly.core.render.web.scheduler.KuiklyRenderCoreContextScheduler
import com.tencent.kuikly.core.render.web.scheduler.KuiklyRenderCoreUIScheduler
import com.tencent.kuikly.core.render.web.utils.Log
import org.w3c.dom.Element

/**
 * Kuikly rendering core
 */
class KuiklyRenderCore : IKuiklyRenderCore {
    // Unique identifier id for kuikly page
    private val instanceId: String = instanceIdProducer++.toString()

    // Kuikly page rendering layer protocol handler
    private var renderLayerHandler: IKuiklyRenderLayerHandler? = null

    // UI task scheduler
    private var uiScheduler: IKuiklyRenderCoreScheduler? = null

    // Native method callback cache map, supports dynamic method calls based on variable names
    private val nativeMethodRegistry = JsMap<Int, KuiklyRenderNativeMethodCallback>()

    // Context execution environment instance
    private val contextHandler = KuiklyRenderContextHandler()

    /**
     * Initialize rendering core
     *
     * @param renderView Page root View
     * @param url Page url
     * @param params Page parameters
     * @param contextInitCallback Callback for rendering environment initialization process
     */
    override fun init(
        renderView: IKuiklyRenderView,
        url: String,
        params: Map<String, Any>,
        contextInitCallback: IKuiklyRenderContextInitCallback
    ) {
        // UI thread scheduler
        uiScheduler = KuiklyRenderCoreUIScheduler {
            // Before synchronizing main thread tasks, need to tell kotlin to layoutIfNeed
            // to avoid timing mismatch between viewFrame setting and view creation
            contextHandler.call(
                KuiklyRenderContextMethod.KuiklyRenderContextMethodLayoutView,
                JsArray(instanceId)
            )
        }
        // Initialize rendering execution environment
        renderLayerHandler = KuiklyRenderLayerHandler().apply {
            init(renderView)
        }
        // Initialize Native event registry
        initNativeMethodRegisters()
        // Put initialization context into execution queue
        performOnContextQueue {
            // Initialize context
            initContextHandler(url, params, contextInitCallback)
        }
    }

    /**
     * Call kuikly side methods from native side to notify instance updates,
     * send event names, parameters, etc.
     */
    override fun sendEvent(event: String, data: Map<String, Any>) {
        performOnContextQueue {
            contextHandler.call(
                KuiklyRenderContextMethod.KuiklyRenderContextMethodUpdateInstance,
                JsArray(
                    instanceId,
                    event,
                    data
                ),
            )
        }
    }

    /**
     * Get [IKuiklyRenderModuleExport] by name, currently not implemented
     */
    override fun <T : IKuiklyRenderModuleExport> module(name: String): T? =
        renderLayerHandler?.module(name)

    /**
     * Destroy rendering core
     */
    override fun destroy() {
        // Destroy render execution environment, mainly destroy view, shadow, module cache maps
        renderLayerHandler?.onDestroy()
        // Destroy business environment
        performOnContextQueue {
            // Notify kotlin to destroy instance
            contextHandler.call(
                KuiklyRenderContextMethod.KuiklyRenderContextMethodDestroyInstance,
                JsArray(
                    instanceId,
                )
            )
            // Destroy context, i.e. remove methods registered in globalThis for native and kuikly to call each other
            contextHandler.destroy()
        }
    }

    /**
     * Return actual DOM element corresponding to tag
     */
    override fun getView(tag: Int): Element? = renderLayerHandler?.getView(tag)

    /**
     * Put event into context queue for execution
     */
    private fun performOnContextQueue(delayMs: Float = 0f, task: () -> Unit) {
        KuiklyRenderCoreContextScheduler.scheduleTask(delayMs.toInt(), task)
    }

    /**
     * Return whether current method call is synchronous
     */
    private fun isSyncMethodCall(method: KuiklyRenderNativeMethod, args: JsArray<Any?>): Boolean {
        if (method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCallModuleMethod) {
            // If calling module method and 5th parameter is 1, indicates synchronous method
            val fifthArg = if (args.length >= 6) {
                args[5].unsafeCast<Int?>() ?: 0
            } else {
                0
            }

            return fifthArg == 1
        }

        // Other methods are also specified as synchronous calls
        return method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCalculateRenderViewSize ||
                method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCreateShadow ||
                method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodRemoveShadow ||
                method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodSetShadowForView ||
                method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodSetShadowProp ||
                method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodSetTimeout ||
                method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCallShadowMethod
    }

    private fun performNativeMethodWithMethod(
        method: KuiklyRenderNativeMethod,
        args: JsArray<Any?>,
    ): Any? {
        if (KuiklyProcessor.isDev) {
            Log.trace("callNative: $method, ${JSON.stringify(args)}")
        }
        // Native method callback
        val cb = nativeMethodRegistry[method.value]
        cb?.also {
            if (isSyncMethodCall(method, args)) {
                // Synchronous method, need to return call result
                return it(method, args)
            } else {
                // Asynchronous method, just put into queue for execution
                uiScheduler?.scheduleTask {
                    it(method, args)
                }
            }
        }
        // Asynchronous method returns null
        return null
    }

    /**
     * Initialize execution environment
     */
    private fun initContextHandler(
        url: String,
        params: Map<String, Any>,
        contextInitCallback: IKuiklyRenderContextInitCallback
    ) {
        contextHandler.apply {
            // Register native methods for kotlin to call, inject into global object
            registerCallNative { method, args ->
                // Execute native method
                performNativeMethodWithMethod(method, args)
            }
            // Record context start
            contextInitCallback.onStart()
            // Initialize execution environment, inject Native methods for kotlin to call
            init(url, instanceId)
            // Record context completion
            contextInitCallback.onFinish()
            // Record start creating page instance
            contextInitCallback.onCreateInstanceStart()
            // Call kotlin method to create instance
            call(
                KuiklyRenderContextMethod.KuiklyRenderContextMethodCreateInstance,
                JsArray(
                    // Instance id
                    instanceId,
                    // Page url
                    url,
                    // Page parameters
                    params
                )
            )
            // Record creating page instance completion
            contextInitCallback.onCreateInstanceFinish()
        }
    }

    /**
     * Create render view
     */
    private fun createRenderView(method: KuiklyRenderNativeMethod, args: JsArray<Any?>): Any? {
        return renderLayerHandler?.createRenderView(
            args.secondArg().unsafeCast<Int>(),
            args.thirdArg().unsafeCast<String>()
        )
    }

    /**
     * Remove specified render view
     */
    private fun removeRenderView(method: KuiklyRenderNativeMethod, args: JsArray<Any?>): Any? =
        renderLayerHandler?.removeRenderView(args.secondArg().unsafeCast<Int>())

    /**
     * Insert sub node into specified parent node
     */
    private fun insertSubRenderView(method: KuiklyRenderNativeMethod, args: JsArray<Any?>): Any? {
        return renderLayerHandler?.insertSubRenderView(
            // Parent node id
            args.secondArg().unsafeCast<Int>(),
            // Sub node id
            args.thirdArg().unsafeCast<Int>(),
            // Insert position
            args.fourthArg().unsafeCast<Int>()
        )
    }

    /**
     * Request parameter whether is event
     */
    private fun isEvent(args: JsArray<Any?>): Boolean = args.fifthArg() == 1

    /**
     * Set property value for view
     */
    private fun setViewProp(method: KuiklyRenderNativeMethod, args: JsArray<Any?>): Any? {
        // First get property value
        var propValue = args.fourthArg().unsafeCast<Any>()
        // If the property to be set is event registration, propValue needs to be encapsulated as callback form
        if (isEvent(args)) {
            // View id
            val tag = args.secondArg()
            // If it's event, register event callback method, callback method execution time calls kuikly side method,
            // method type is fireViewEvent, parameter type and value also passed in
            propValue = KuiklyRenderCallback { result ->
                performOnContextQueue {
                    contextHandler.call(
                        KuiklyRenderContextMethod.KuiklyRenderContextMethodFireViewEvent,
                        JsArray(
                            instanceId,
                            tag,
                            args.thirdArg(),
                            result
                        )
                    )
                }
            }
        }
        return renderLayerHandler?.setProp(
            args.secondArg().unsafeCast<Int>(),
            args.thirdArg().unsafeCast<String>(),
            propValue
        )
    }

    /**
     * Set render view's size position data
     */
    private fun setRenderViewFrame(method: KuiklyRenderNativeMethod, args: JsArray<Any?>): Any? {
        val frame = Frame(
            args.thirdArg().unsafeCast<Double>(),
            args.fourthArg().unsafeCast<Double>(),
            args.fifthArg().unsafeCast<Double>(),
            args.sixthArg().unsafeCast<Double>()
        )
        return renderLayerHandler?.setRenderViewFrame(args.secondArg().unsafeCast<Int>(), frame)
    }

    /**
     * Calculate size information of given text content occupying view
     */
    private fun calculateRenderViewSize(
        method: KuiklyRenderNativeMethod,
        args: JsArray<Any?>
    ): Any {
        // Get calculated view size data
        val size = renderLayerHandler?.calculateRenderViewSize(
            args.secondArg().unsafeCast<Int>(),
            SizeF(args.thirdArg().unsafeCast<Float>(), args.fourthArg().unsafeCast<Float>()),
        )
        // Data compatibility processing, exception takes 0
        val width = size?.width?.asDynamic().toFixed(2) ?: "0.00"
        val height = size?.height?.asDynamic().toFixed(2) ?: "0.00"
        // Return size information
        return "$width|$height"
    }

    /**
     * Call view provided method
     */
    private fun callViewMethod(method: KuiklyRenderNativeMethod, args: JsArray<Any?>): Any? {
        // First get callback method id
        val callbackId = args.fifthArg().unsafeCast<String?>()
        // If callback method id is passed in, use event callback to wrap
        val callback = if (callbackId?.isNotEmpty() == true) {
            KuiklyRenderCallback { result ->
                performOnContextQueue {
                    // Callback kotlin side
                    contextHandler.call(
                        KuiklyRenderContextMethod.KuiklyRenderContextMethodFireCallback,
                        JsArray(
                            instanceId,
                            callbackId,
                            result
                        )
                    )
                }
            }
        } else {
            null
        }
        return renderLayerHandler?.callViewMethod(
            args.secondArg().unsafeCast<Int>(),
            args.thirdArg().unsafeCast<String>(),
            args.fourthArg().unsafeCast<String?>(),
            callback
        )
    }

    /**
     * Call shadow view method
     */
    private fun callShadowMethod(method: KuiklyRenderNativeMethod, args: JsArray<Any?>): Any? {
        return renderLayerHandler?.callShadowMethod(
            args.secondArg().unsafeCast<Int>(),
            args.thirdArg().unsafeCast<String>(),
            args.fourthArg().unsafeCast<String>(),
        )
    }

    /**
     * Call module method
     */
    private fun callModuleMethod(method: KuiklyRenderNativeMethod, args: JsArray<Any?>): Any? {
        // First get callback id
        val callbackId = args.fifthArg().unsafeCast<String?>()
        // If callback method id is passed in, use event callback to wrap
        val callback = if (callbackId?.isNotEmpty() == true) {
            KuiklyRenderCallback { result ->
                performOnContextQueue {
                    // Callback kotlin side
                    contextHandler.call(
                        KuiklyRenderContextMethod.KuiklyRenderContextMethodFireCallback,
                        JsArray(
                            instanceId,
                            callbackId,
                            result
                        )
                    )
                }
            }
        } else {
            null
        }
        return renderLayerHandler?.callModuleMethod(
            args.secondArg().unsafeCast<String>(),
            args.thirdArg().unsafeCast<String>(),
            args.fourthArg(),
            callback
        )
    }

    /**
     * Create shadow view
     */
    private fun createShadow(method: KuiklyRenderNativeMethod, args: JsArray<Any?>): Any? {
        return renderLayerHandler?.createShadow(
            args.secondArg().unsafeCast<Int>(),
            args.thirdArg().unsafeCast<String>()
        )
    }

    /**
     * Remove shadow view
     */
    private fun removeShadow(method: KuiklyRenderNativeMethod, args: JsArray<Any?>): Any? =
        renderLayerHandler?.removeShadow(args.secondArg().unsafeCast<Int>())

    /**
     * Set shadow property
     */
    private fun setShadowProp(method: KuiklyRenderNativeMethod, args: JsArray<Any?>): Any? {
        return renderLayerHandler?.setShadowProp(
            args.secondArg().unsafeCast<Int>(),
            args.thirdArg().unsafeCast<String>(),
            args.fourthArg().unsafeCast<Any>()
        )
    }

    /**
     * Set shadow view
     */
    private fun setShadow(method: KuiklyRenderNativeMethod, args: JsArray<Any?>): Any? {
        // First get cached shadow, if not get directly return
        val shadow = renderLayerHandler?.shadow(args.secondArg().unsafeCast<Int>()) ?: return null
        uiScheduler?.scheduleTask {
            // ui asynchronous execution
            renderLayerHandler?.setShadow(args.secondArg().unsafeCast<Int>(), shadow)
        }

        return null
    }

    /**
     * Set delayed execution method
     */
    private fun setTimeout(method: KuiklyRenderNativeMethod, args: JsArray<Any?>): Any? {
        // Pass in delay time and insert delay execution queue
        performOnContextQueue(args.secondArg().unsafeCast<Float>()) {
            contextHandler.call(
                KuiklyRenderContextMethod.KuiklyRenderContextMethodFireCallback,
                JsArray(
                    instanceId,
                    args.thirdArg()
                )
            )
        }

        return null
    }

    /**
     * Initialize native event registry
     */
    private fun initNativeMethodRegisters() {
        // Register render method reference to native method cache map for subsequent dynamic calls
        // kotlin side called parameters, first parameter is instanceId, subsequent are specific parameters
        nativeMethodRegistry[KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCreateRenderView.value] =
            ::createRenderView
        nativeMethodRegistry[KuiklyRenderNativeMethod.KuiklyRenderNativeMethodRemoveRenderView.value] =
            ::removeRenderView
        nativeMethodRegistry[KuiklyRenderNativeMethod.KuiklyRenderNativeMethodInsertSubRenderView.value] =
            ::insertSubRenderView
        nativeMethodRegistry[KuiklyRenderNativeMethod.KuiklyRenderNativeMethodSetViewProp.value] =
            ::setViewProp
        nativeMethodRegistry[KuiklyRenderNativeMethod.KuiklyRenderNativeMethodSetRenderViewFrame.value] =
            ::setRenderViewFrame
        nativeMethodRegistry[KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCalculateRenderViewSize.value] =
            ::calculateRenderViewSize
        nativeMethodRegistry[KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCallViewMethod.value] =
            ::callViewMethod
        nativeMethodRegistry[KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCallModuleMethod.value] =
            ::callModuleMethod
        nativeMethodRegistry[KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCreateShadow.value] =
            ::createShadow
        nativeMethodRegistry[KuiklyRenderNativeMethod.KuiklyRenderNativeMethodRemoveShadow.value] =
            ::removeShadow
        nativeMethodRegistry[KuiklyRenderNativeMethod.KuiklyRenderNativeMethodSetShadowProp.value] =
            ::setShadowProp
        nativeMethodRegistry[KuiklyRenderNativeMethod.KuiklyRenderNativeMethodSetShadowForView.value] =
            ::setShadow
        nativeMethodRegistry[KuiklyRenderNativeMethod.KuiklyRenderNativeMethodSetTimeout.value] =
            ::setTimeout
        nativeMethodRegistry[KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCallShadowMethod.value] =
            ::callShadowMethod
    }

    companion object {
        // Global page unique identifier id
        private var instanceIdProducer = 0L
    }
}
