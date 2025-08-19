package com.tencent.kuikly.core.render.web.expand.components

import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import org.khronos.webgl.ArrayBuffer
import org.w3c.dom.HTMLCanvasElement

/**
 * Pag animation view
 */
class KRPagView : IKuiklyRenderViewExport {
    // Pag instance for playback
    private val pag = kuiklyDocument.createElement(ElementType.CANVAS)
    // Pag animation instance
    private var pagView: dynamic = null
    // Animation source file
    private var src = ""

    // Whether to auto-play
    private var autoPlay = true
    // Whether the animation has stopped
    private var hadStop = false
    // animation repeat count
    private var repeatCount = 0
    // callback functions
    private var loadFailureCallback: KuiklyRenderCallback? = null
    private var animationStartCallback: KuiklyRenderCallback? = null
    private var animationEndCallback: KuiklyRenderCallback? = null
    private var animationCancelCallback: KuiklyRenderCallback? = null
    private var animationRepeatCallback: KuiklyRenderCallback? = null
    // Web-side actual callback methods
    private var pagAnimationStartCallback: (() -> Unit)? = null
    private var pagAnimationEndCallback: (() -> Unit)? = null
    private var pagAnimationCancelCallback: (() -> Unit)? = null
    private var pagAnimationRepeatCallback: (() -> Unit)? = null

    override val ele: HTMLCanvasElement
        get() = pag.unsafeCast<HTMLCanvasElement>()

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            SRC -> {
                setSrc(propValue)
                return true
            }
            REPEAT_COUNT -> {
                repeatCount = propValue as Int
                return true
            }
            AUTO_PLAY -> {
                autoPlay(propValue)
                return true
            }
            LOAD_FAIL -> {
                loadFailureCallback = propValue as KuiklyRenderCallback
                true
            }
            ANIMATION_START -> {
                animationStartCallback = propValue as KuiklyRenderCallback
                true
            }
            ANIMATION_END -> {
                animationEndCallback = propValue as KuiklyRenderCallback
                true
            }
            ANIMATION_CANCEL -> {
                animationCancelCallback = propValue as KuiklyRenderCallback
                true
            }
            ANIMATION_REPEAT -> {
                animationRepeatCallback = propValue as KuiklyRenderCallback
                true
            }
            else -> super.setProp(propKey, propValue)
        }
    }

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_PLAY -> play()
            METHOD_STOP -> stop()
            else -> super.call(method, params, callback)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Destroy pag instance
        destroy()
    }

    /**
     * Get real event name
     */
    private fun getEventName(eventName: String): String
        // Event name conversion animationStart -> onAnimationStart
        = "on${eventName.replaceFirstChar { it.uppercaseChar() }}"

    /**
     * Register pagView event callbacks
     */
    private fun addAllEventListener() {
        if (animationStartCallback !== null && pagAnimationStartCallback === null) {
            pagAnimationStartCallback = {
                animationStartCallback?.invoke(null)
            }
            pagView?.addListener(getEventName(ANIMATION_START), pagAnimationStartCallback)
        }
        if (animationCancelCallback !== null && pagAnimationCancelCallback === null) {
            pagAnimationCancelCallback = {
                animationCancelCallback?.invoke(null)
            }
            pagView?.addListener(getEventName(ANIMATION_CANCEL), pagAnimationCancelCallback)
        }
        if (animationRepeatCallback !== null && pagAnimationRepeatCallback === null) {
            pagAnimationRepeatCallback = {
                animationRepeatCallback?.invoke(null)
            }
            pagView?.addListener(getEventName(ANIMATION_REPEAT), pagAnimationRepeatCallback)
        }
        if (animationEndCallback !== null && pagAnimationEndCallback === null) {
            pagAnimationEndCallback = {
                animationEndCallback?.invoke(null)
            }
            pagView?.addListener(getEventName(ANIMATION_END), pagAnimationEndCallback)
        }
    }

    /**
     * Remove event callbacks
     */
    private fun removeAllEventListener() {
        if (pagAnimationStartCallback !== null) {
            pagView?.removeListener(getEventName(ANIMATION_START), pagAnimationStartCallback)
        }
        if (pagAnimationCancelCallback !== null) {
            pagView?.removeListener(getEventName(ANIMATION_CANCEL), pagAnimationCancelCallback)
        }
        if (pagAnimationRepeatCallback !== null) {
            pagView?.removeListener(getEventName(ANIMATION_REPEAT), pagAnimationRepeatCallback)
        }
        if (pagAnimationEndCallback !== null) {
            pagView?.removeListener(getEventName(ANIMATION_END), pagAnimationEndCallback)
        }
    }

    private fun stop() {
        autoPlay = false
        if (!hadStop) {
            hadStop = true
            pagView?.stop()
        }
    }

    private fun play() {
        // Register event callbacks
        addAllEventListener()
        // Set to auto-play
        autoPlay = true
        // Not stopped
        hadStop = false
        // Call play
        pagView?.play()
    }

    private fun destroy() {
        // Stop playback
        stop()
        // Remove event callbacks
        removeAllEventListener()
        // Remove instance
        pagView?.destroy()
    }


    /**
     * Try auto-play
     */
    private fun tryAutoPlay() {
        if (autoPlay) {
            pagView?.play()
        }
    }

    /**
     * set auto play
     */
    private fun autoPlay(propValue: Any) {
        autoPlay = propValue as Int == 1
        if (autoPlay) {
            tryAutoPlay()
        }
    }

    /**
     * Load pag file
     */
    private fun loadPagFile(buffer: ArrayBuffer) {
        // Global pag instance
        val pagInstance = kuiklyWindow.asDynamic().PAGInstance
        // Load source file buffer content
        pagInstance.PAGFile.load(buffer).then { pagFile ->
            // Successfully loaded pag file
            if (pagFile !== undefined) {
                // Set dimensions
                ele.width = pagFile.width().unsafeCast<Int>()
                ele.height = pagFile.height().unsafeCast<Int>()
                // Create pagView
                pagInstance.PAGView.init(pagFile, ele).then { realPagView ->
                    if (realPagView !== undefined) {
                        // Save the obtained pagView
                        pagView = realPagView
                        // Set repeat count
                        pagView.setRepeatCount(repeatCount)
                        // Register event callbacks
                        addAllEventListener()
                        // If auto-play is enabled, call play
                        tryAutoPlay()
                    }
                }
            }
        }.catch {
            loadFailureCallback?.invoke(null)
        }
    }

    /**
     * Initialize Web host Pag file
     */
    private fun initPag(src: String) {
        // Load pag source file
        kuiklyWindow.fetch(src).then { response ->
            if (!response.ok) {
                loadFailureCallback?.invoke(null)
                return@then
            }
            // Get source file binary content
            response.arrayBuffer().then { buffer ->
                val pagInstance = kuiklyWindow.asDynamic().PAGInstance
                if (pagInstance !== undefined) {
                    // If already instantiated, use it directly
                    loadPagFile(buffer)
                } else {
                    // Get pag instance
                    kuiklyWindow.asDynamic().libpag.PAGInit().then { instance ->
                        if (instance !== undefined) {
                            // Save the pag instance globally for other PagViews on the current page
                            kuiklyWindow.asDynamic().PAGInstance = instance
                            // Load pag file
                            loadPagFile(buffer)
                        }
                    }
                }
            }
        }.catch {
            // Load exception, callback
            loadFailureCallback?.invoke(null)
        }
    }

    /**
     * Set source file
     */
    private fun setSrc(params: Any) {
        // Network links and direct file content need to be loaded first
        val newSrc = params.unsafeCast<String>()
        if (src == newSrc || !newSrc.startsWith("https")) {
            // Source file unchanged, or non-https source file, skip processing
            return
        }
        src = newSrc
        // Load pag source file
        initPag(newSrc)
    }

    companion object {
        private const val SRC = "src"
        private const val REPEAT_COUNT = "repeatCount"
        private const val AUTO_PLAY = "autoPlay"
        private const val LOAD_FAIL = "loadFailure"
        private const val ANIMATION_START = "animationStart"
        private const val ANIMATION_END = "animationEnd"
        private const val ANIMATION_CANCEL = "animationCancel"
        private const val ANIMATION_REPEAT = "animationRepeat"
        private const val METHOD_PLAY = "play"
        private const val METHOD_STOP = "stop"

        const val VIEW_NAME = "KRPAGView"
    }
}
