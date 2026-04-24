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

package com.tencent.kuikly.core.render.web.runtime.miniapp.expand.components.wx

import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.EventHandler
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.wx.MiniWXVideoViewElement
import org.w3c.dom.Element

/**
 * Render view export for the WeChat mini-program native `video` component.
 *
 * Delegates property setting and event forwarding to [MiniWXVideoViewElement]. Native
 * events' `detail` is serialized as a JSON string and forwarded under the `data` key.
 */
class KRWXVideoView : IKuiklyRenderViewExport {
    private val videoElement = MiniWXVideoViewElement()

    override val ele: Element
        get() = videoElement.unsafeCast<Element>()

    // Event callbacks
    private var playCallback: KuiklyRenderCallback? = null
    private var pauseCallback: KuiklyRenderCallback? = null
    private var endedCallback: KuiklyRenderCallback? = null
    private var timeUpdateCallback: KuiklyRenderCallback? = null
    private var fullscreenChangeCallback: KuiklyRenderCallback? = null
    private var waitingCallback: KuiklyRenderCallback? = null
    private var errorCallback: KuiklyRenderCallback? = null
    private var progressCallback: KuiklyRenderCallback? = null
    private var loadedMetadataCallback: KuiklyRenderCallback? = null
    private var controlsToggleCallback: KuiklyRenderCallback? = null
    private var enterPipCallback: KuiklyRenderCallback? = null
    private var leavePipCallback: KuiklyRenderCallback? = null
    private var seekCompleteCallback: KuiklyRenderCallback? = null

    init {
        videoElement.addEventListener(EVENT_PLAY, createEventForwarder { playCallback })
        videoElement.addEventListener(EVENT_PAUSE, createEventForwarder { pauseCallback })
        videoElement.addEventListener(EVENT_ENDED, createEventForwarder { endedCallback })
        videoElement.addEventListener(EVENT_TIME_UPDATE, createEventForwarder { timeUpdateCallback })
        videoElement.addEventListener(
            EVENT_FULLSCREEN_CHANGE,
            createEventForwarder { fullscreenChangeCallback }
        )
        videoElement.addEventListener(EVENT_WAITING, createEventForwarder { waitingCallback })
        videoElement.addEventListener(EVENT_ERROR, createEventForwarder { errorCallback })
        videoElement.addEventListener(EVENT_PROGRESS, createEventForwarder { progressCallback })
        videoElement.addEventListener(
            EVENT_LOADED_METADATA,
            createEventForwarder { loadedMetadataCallback }
        )
        videoElement.addEventListener(
            EVENT_CONTROLS_TOGGLE,
            createEventForwarder { controlsToggleCallback }
        )
        videoElement.addEventListener(EVENT_ENTER_PIP, createEventForwarder { enterPipCallback })
        videoElement.addEventListener(EVENT_LEAVE_PIP, createEventForwarder { leavePipCallback })
        videoElement.addEventListener(
            EVENT_SEEK_COMPLETE,
            createEventForwarder { seekCompleteCallback }
        )
    }

    private fun createEventForwarder(callbackSupplier: () -> KuiklyRenderCallback?): EventHandler {
        return { event: dynamic ->
            val data: dynamic = JSON.stringify(event.detail)
            callbackSupplier()?.invoke(mapOf<String, Any>(KEY_DATA to data.unsafeCast<String>()))
        }
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            PROP_SRC -> { videoElement.src = propValue as String; true }
            PROP_DURATION -> { videoElement.duration = toInt(propValue); true }
            PROP_CONTROLS -> { videoElement.controls = toBoolean(propValue); true }
            PROP_DANMU_LIST -> { videoElement.danmuList = propValue as String; true }
            PROP_DANMU_BTN -> { videoElement.danmuBtn = toBoolean(propValue); true }
            PROP_ENABLE_DANMU -> { videoElement.enableDanmu = toBoolean(propValue); true }
            PROP_AUTOPLAY -> { videoElement.autoplay = toBoolean(propValue); true }
            PROP_LOOP -> { videoElement.loop = toBoolean(propValue); true }
            PROP_MUTED -> { videoElement.muted = toBoolean(propValue); true }
            PROP_INITIAL_TIME -> { videoElement.initialTime = toInt(propValue); true }
            PROP_PAGE_GESTURE -> { videoElement.pageGesture = toBoolean(propValue); true }
            PROP_DIRECTION -> { videoElement.direction = toInt(propValue); true }
            PROP_SHOW_PROGRESS -> { videoElement.showProgress = toBoolean(propValue); true }
            PROP_SHOW_FULLSCREEN_BTN -> { videoElement.showFullscreenBtn = toBoolean(propValue); true }
            PROP_SHOW_PLAY_BTN -> { videoElement.showPlayBtn = toBoolean(propValue); true }
            PROP_SHOW_CENTER_PLAY_BTN -> { videoElement.showCenterPlayBtn = toBoolean(propValue); true }
            PROP_ENABLE_PROGRESS_GESTURE -> {
                videoElement.enableProgressGesture = toBoolean(propValue); true
            }
            PROP_OBJECT_FIT -> { videoElement.objectFit = propValue as String; true }
            PROP_POSTER -> { videoElement.poster = propValue as String; true }
            PROP_SHOW_MUTE_BTN -> { videoElement.showMuteBtn = toBoolean(propValue); true }
            PROP_TITLE -> { videoElement.title = propValue as String; true }
            PROP_PLAY_BTN_POSITION -> { videoElement.playBtnPosition = propValue as String; true }
            PROP_ENABLE_PLAY_GESTURE -> { videoElement.enablePlayGesture = toBoolean(propValue); true }
            PROP_AUTO_PAUSE_IF_NAVIGATE -> {
                videoElement.autoPauseIfNavigate = toBoolean(propValue); true
            }
            PROP_AUTO_PAUSE_IF_OPEN_NATIVE -> {
                videoElement.autoPauseIfOpenNative = toBoolean(propValue); true
            }
            PROP_VSLIDE_GESTURE -> { videoElement.vslideGesture = toBoolean(propValue); true }
            PROP_VSLIDE_GESTURE_IN_FULLSCREEN -> {
                videoElement.vslideGestureInFullscreen = toBoolean(propValue); true
            }
            PROP_SHOW_BOTTOM_PROGRESS -> {
                videoElement.showBottomProgress = toBoolean(propValue); true
            }
            PROP_AD_UNIT_ID -> { videoElement.adUnitId = propValue as String; true }
            PROP_POSTER_FOR_CRAWLER -> { videoElement.posterForCrawler = propValue as String; true }
            PROP_SHOW_CASTING_BUTTON -> {
                videoElement.showCastingButton = toBoolean(propValue); true
            }
            PROP_PICTURE_IN_PICTURE_MODE -> {
                videoElement.pictureInPictureMode = propValue as String; true
            }
            PROP_ENABLE_AUTO_ROTATION -> {
                videoElement.enableAutoRotation = toBoolean(propValue); true
            }
            PROP_SHOW_SCREEN_LOCK_BUTTON -> {
                videoElement.showScreenLockButton = toBoolean(propValue); true
            }
            PROP_SHOW_SNAPSHOT_BUTTON -> {
                videoElement.showSnapshotButton = toBoolean(propValue); true
            }
            PROP_SHOW_BACKGROUND_PLAYBACK_BUTTON -> {
                videoElement.showBackgroundPlaybackButton = toBoolean(propValue); true
            }
            PROP_BACKGROUND_POSTER -> { videoElement.backgroundPoster = propValue as String; true }
            PROP_REFERRER_POLICY -> { videoElement.referrerPolicy = propValue as String; true }
            PROP_IS_DRM -> { videoElement.isDrm = toBoolean(propValue); true }
            PROP_IS_LIVE -> { videoElement.isLive = toBoolean(propValue); true }
            PROP_PROVISION_URL -> { videoElement.provisionUrl = propValue as String; true }
            PROP_CERTIFICATE_URL -> { videoElement.certificateUrl = propValue as String; true }
            PROP_LICENSE_URL -> { videoElement.licenseUrl = propValue as String; true }
            PROP_PREFERRED_PEAK_BIT_RATE -> {
                videoElement.preferredPeakBitRate = toInt(propValue); true
            }
            CALLBACK_PLAY -> { playCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true }
            CALLBACK_PAUSE -> { pauseCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true }
            CALLBACK_ENDED -> { endedCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true }
            CALLBACK_TIME_UPDATE -> {
                timeUpdateCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true
            }
            CALLBACK_FULLSCREEN_CHANGE -> {
                fullscreenChangeCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true
            }
            CALLBACK_WAITING -> { waitingCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true }
            CALLBACK_ERROR -> { errorCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true }
            CALLBACK_PROGRESS -> { progressCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true }
            CALLBACK_LOADED_METADATA -> {
                loadedMetadataCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true
            }
            CALLBACK_CONTROLS_TOGGLE -> {
                controlsToggleCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true
            }
            CALLBACK_ENTER_PIP -> {
                enterPipCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true
            }
            CALLBACK_LEAVE_PIP -> {
                leavePipCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true
            }
            CALLBACK_SEEK_COMPLETE -> {
                seekCompleteCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true
            }
            else -> super.setProp(propKey, propValue)
        }
    }

    private fun toBoolean(value: Any): Boolean = when (value) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> value == "1" || value.equals("true", ignoreCase = true)
        else -> false
    }

    private fun toInt(value: Any): Int = when (value) {
        is Int -> value
        is Number -> value.toInt()
        is String -> value.toIntOrNull() ?: 0
        else -> 0
    }

    companion object {
        const val VIEW_NAME = "KRWXVideoView"

        // Props
        const val PROP_SRC = "src"
        const val PROP_DURATION = "duration"
        const val PROP_CONTROLS = "controls"
        const val PROP_DANMU_LIST = "danmuList"
        const val PROP_DANMU_BTN = "danmuBtn"
        const val PROP_ENABLE_DANMU = "enableDanmu"
        const val PROP_AUTOPLAY = "autoplay"
        const val PROP_LOOP = "loop"
        const val PROP_MUTED = "muted"
        const val PROP_INITIAL_TIME = "initialTime"
        const val PROP_PAGE_GESTURE = "pageGesture"
        const val PROP_DIRECTION = "direction"
        const val PROP_SHOW_PROGRESS = "showProgress"
        const val PROP_SHOW_FULLSCREEN_BTN = "showFullscreenBtn"
        const val PROP_SHOW_PLAY_BTN = "showPlayBtn"
        const val PROP_SHOW_CENTER_PLAY_BTN = "showCenterPlayBtn"
        const val PROP_ENABLE_PROGRESS_GESTURE = "enableProgressGesture"
        const val PROP_OBJECT_FIT = "objectFit"
        const val PROP_POSTER = "poster"
        const val PROP_SHOW_MUTE_BTN = "showMuteBtn"
        const val PROP_TITLE = "title"
        const val PROP_PLAY_BTN_POSITION = "playBtnPosition"
        const val PROP_ENABLE_PLAY_GESTURE = "enablePlayGesture"
        const val PROP_AUTO_PAUSE_IF_NAVIGATE = "autoPauseIfNavigate"
        const val PROP_AUTO_PAUSE_IF_OPEN_NATIVE = "autoPauseIfOpenNative"
        const val PROP_VSLIDE_GESTURE = "vslideGesture"
        const val PROP_VSLIDE_GESTURE_IN_FULLSCREEN = "vslideGestureInFullscreen"
        const val PROP_SHOW_BOTTOM_PROGRESS = "showBottomProgress"
        const val PROP_AD_UNIT_ID = "adUnitId"
        const val PROP_POSTER_FOR_CRAWLER = "posterForCrawler"
        const val PROP_SHOW_CASTING_BUTTON = "showCastingButton"
        const val PROP_PICTURE_IN_PICTURE_MODE = "pictureInPictureMode"
        const val PROP_ENABLE_AUTO_ROTATION = "enableAutoRotation"
        const val PROP_SHOW_SCREEN_LOCK_BUTTON = "showScreenLockButton"
        const val PROP_SHOW_SNAPSHOT_BUTTON = "showSnapshotButton"
        const val PROP_SHOW_BACKGROUND_PLAYBACK_BUTTON = "showBackgroundPlaybackButton"
        const val PROP_BACKGROUND_POSTER = "backgroundPoster"
        const val PROP_REFERRER_POLICY = "referrerPolicy"
        const val PROP_IS_DRM = "isDrm"
        const val PROP_IS_LIVE = "isLive"
        const val PROP_PROVISION_URL = "provisionUrl"
        const val PROP_CERTIFICATE_URL = "certificateUrl"
        const val PROP_LICENSE_URL = "licenseUrl"
        const val PROP_PREFERRED_PEAK_BIT_RATE = "preferredPeakBitRate"

        // Callbacks
        const val CALLBACK_PLAY = "playCallback"
        const val CALLBACK_PAUSE = "pauseCallback"
        const val CALLBACK_ENDED = "endedCallback"
        const val CALLBACK_TIME_UPDATE = "timeUpdateCallback"
        const val CALLBACK_FULLSCREEN_CHANGE = "fullscreenChangeCallback"
        const val CALLBACK_WAITING = "waitingCallback"
        const val CALLBACK_ERROR = "errorCallback"
        const val CALLBACK_PROGRESS = "progressCallback"
        const val CALLBACK_LOADED_METADATA = "loadedMetadataCallback"
        const val CALLBACK_CONTROLS_TOGGLE = "controlsToggleCallback"
        const val CALLBACK_ENTER_PIP = "enterPictureInPictureCallback"
        const val CALLBACK_LEAVE_PIP = "leavePictureInPictureCallback"
        const val CALLBACK_SEEK_COMPLETE = "seekCompleteCallback"

        // Mini-program native event names
        private const val EVENT_PLAY = "play"
        private const val EVENT_PAUSE = "pause"
        private const val EVENT_ENDED = "ended"
        private const val EVENT_TIME_UPDATE = "timeupdate"
        private const val EVENT_FULLSCREEN_CHANGE = "fullscreenchange"
        private const val EVENT_WAITING = "waiting"
        private const val EVENT_ERROR = "error"
        private const val EVENT_PROGRESS = "progress"
        private const val EVENT_LOADED_METADATA = "loadedmetadata"
        private const val EVENT_CONTROLS_TOGGLE = "controlstoggle"
        private const val EVENT_ENTER_PIP = "enterpictureinpicture"
        private const val EVENT_LEAVE_PIP = "leavepictureinpicture"
        private const val EVENT_SEEK_COMPLETE = "seekcomplete"

        private const val KEY_DATA = "data"
    }
}
