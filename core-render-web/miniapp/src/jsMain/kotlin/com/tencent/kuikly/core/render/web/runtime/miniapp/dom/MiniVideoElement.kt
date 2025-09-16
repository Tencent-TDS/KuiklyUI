package com.tencent.kuikly.core.render.web.runtime.miniapp.dom

import com.tencent.kuikly.core.render.web.runtime.miniapp.const.TransformConst
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.NativeApi
import com.tencent.kuikly.core.render.web.utils.Log

/**
 * Mini program video node, eventually rendered as video in mini program
 */
class MiniVideoElement(
    nodeName: String = TransformConst.VIDEO,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType) {

    // Video component context
    private val videoContext: dynamic
        get() {
            return NativeApi.getVideoContext(this.id)
        }

    // Playback timestamp
    private var currentPlayTime = 0.0

    init {
        setAttribute(CONTROLS, false)
        setAttribute(SHOW_CENTER_PLAY_BTN, false)
        // Add TIME_UPDATE listener by default to update playback timestamp
        addEventListener(TIME_UPDATE, { event ->
            currentPlayTime = event.detail.currentTime.unsafeCast<Double>()
        })

        style.onStyleSet = fun (styleName, value): Boolean {
            console.log(styleName, OBJECT_FIT, value)
            if (styleName == OBJECT_FIT) {
                setAttribute(OBJECT_FIT, value)
                return false
            }
            return true
        }
    }


    // Video source URL
    @JsName("src")
    var src: String = ""
        set(value) {
            this.setAttribute(SRC, value)
            field = value
        }

    // Whether to play muted
    @JsName("muted")
    var muted: Boolean = false
        set(value) {
            this.setAttribute(MUTED, value)
            field = value
        }

    // Playback rate
    @JsName("playbackRate")
    var playbackRate: Double = 1.0
        set(value) {
            try {
                videoContext.playbackRate(value)
                field = value
            } catch (err: dynamic) {
                Log.error("Failed to set playback rate, error is: $err")
            }
        }

    // Current playback timestamp
    @JsName("currentTime")
    var currentTime: Double
        get() = currentPlayTime
        set(value) {
            videoContext.seek(value)
        }

    /**
     * Start playback
     */
    @JsName("play")
    fun play() {
        videoContext.play()
    }

    /**
     * Pause playback
     */
    @JsName("pause")
    fun pause() {
        videoContext.pause()
    }

    companion object {
        private const val TIME_UPDATE = "timeupdate"
        private const val MUTED = "muted"
        private const val CONTROLS = "controls"
        private const val OBJECT_FIT = "objectFit"
        private const val SHOW_CENTER_PLAY_BTN = "show-center-play-btn"
        private const val SRC = "src"
    }
}
