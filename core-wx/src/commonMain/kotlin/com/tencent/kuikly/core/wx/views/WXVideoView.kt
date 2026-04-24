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

package com.tencent.kuikly.core.wx.views

import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * WeChat mini-program native `video` component `object-fit` values.
 */
object WXVideoObjectFit {
    const val CONTAIN = "contain"
    const val COVER = "cover"
    const val FILL = "fill"
}

/**
 * WeChat mini-program native `video` component `direction` values.
 *
 * - 0: 正常竖向
 * - 90: 屏幕逆时针 90 度
 * - -90: 屏幕顺时针 90 度
 */
object WXVideoDirection {
    const val NORMAL = 0
    const val ROTATE_90 = 90
    const val ROTATE_MINUS_90 = -90
}

/**
 * WeChat mini-program native `video` component `play-btn-position` values.
 */
object WXVideoPlayBtnPosition {
    const val BOTTOM = "bottom"
    const val CENTER = "center"
}

/**
 * Cross-platform Compose-style wrapper of the WeChat mini-program native `video` component.
 *
 * On mini-program platform (`pageData.params.is_miniprogram == "1"`) it renders the native
 * `<video/>` via `KRWXVideoView`; on other platforms it falls back to a plain view so the
 * compose tree still renders.
 */
class WXVideoView : ComposeView<WXVideoAttr, WXVideoEvent>() {

    override fun createEvent(): WXVideoEvent = WXVideoEvent()

    override fun createAttr(): WXVideoAttr = WXVideoAttr()

    override fun viewName(): String {
        val pageData = getPager().pageData
        if (pageData.params.optString(IS_MINI_PROGRAM) == "1") {
            return VIEW_NAME_MINI_PROGRAM
        }
        return ViewConst.TYPE_VIEW
    }

    override fun body(): ViewBuilder {
        return {
            // Leave empty; native <video> renders itself. Children (if any) are rendered via `wx:for`.
        }
    }

    companion object {
        internal const val IS_MINI_PROGRAM = "is_miniprogram"
        internal const val VIEW_NAME_MINI_PROGRAM = "KRWXVideoView"
    }
}

/**
 * Attributes for [WXVideoView]. Mirrors native `video` attributes.
 *
 * For list/array properties (e.g. `danmu-list`, `picture-in-picture-mode`) we expose both a
 * typed helper that serializes to JSON, and a raw JSON string setter for flexibility.
 */
class WXVideoAttr : ComposeAttr() {

    /** Video source URL. */
    fun src(v: String): WXVideoAttr { PROP_SRC with v; return this }

    /** Video duration hint in seconds. */
    fun duration(v: Int): WXVideoAttr { PROP_DURATION with v; return this }

    /** Whether to show controls. Default true. */
    fun controls(v: Boolean): WXVideoAttr { PROP_CONTROLS with v; return this }

    /** Danmu list JSON string, e.g. `[{"text":"...","color":"#ff0000","time":1}]`. */
    fun danmuListJson(json: String): WXVideoAttr { PROP_DANMU_LIST with json; return this }

    /** Whether to show danmu button. */
    fun danmuBtn(v: Boolean): WXVideoAttr { PROP_DANMU_BTN with v; return this }

    /** Whether to enable danmu display. */
    fun enableDanmu(v: Boolean): WXVideoAttr { PROP_ENABLE_DANMU with v; return this }

    /** Whether to autoplay. Default false. */
    fun autoplay(v: Boolean): WXVideoAttr { PROP_AUTOPLAY with v; return this }

    /** Whether to loop. Default false. */
    fun loop(v: Boolean): WXVideoAttr { PROP_LOOP with v; return this }

    /** Whether to mute audio. Default false. */
    fun muted(v: Boolean): WXVideoAttr { PROP_MUTED with v; return this }

    /** Initial playback position in seconds. Default 0. */
    fun initialTime(v: Int): WXVideoAttr { PROP_INITIAL_TIME with v; return this }

    /** Whether to allow page gesture to control volume/brightness. Default false. */
    fun pageGesture(v: Boolean): WXVideoAttr { PROP_PAGE_GESTURE with v; return this }

    /** Video direction. See [WXVideoDirection]. */
    fun direction(v: Int): WXVideoAttr { PROP_DIRECTION with v; return this }

    /** Whether to show progress. Default true. */
    fun showProgress(v: Boolean): WXVideoAttr { PROP_SHOW_PROGRESS with v; return this }

    /** Whether to show fullscreen button. Default true. */
    fun showFullscreenBtn(v: Boolean): WXVideoAttr { PROP_SHOW_FULLSCREEN_BTN with v; return this }

    /** Whether to show play button at bottom. Default true. */
    fun showPlayBtn(v: Boolean): WXVideoAttr { PROP_SHOW_PLAY_BTN with v; return this }

    /** Whether to show centered play button. Default true. */
    fun showCenterPlayBtn(v: Boolean): WXVideoAttr { PROP_SHOW_CENTER_PLAY_BTN with v; return this }

    /** Whether to enable horizontal progress gesture. Default true. */
    fun enableProgressGesture(v: Boolean): WXVideoAttr { PROP_ENABLE_PROGRESS_GESTURE with v; return this }

    /** Video fit mode. See [WXVideoObjectFit]. Default `contain`. */
    fun objectFit(v: String): WXVideoAttr { PROP_OBJECT_FIT with v; return this }

    /** Poster image URL before playback. */
    fun poster(v: String): WXVideoAttr { PROP_POSTER with v; return this }

    /** Whether to show the mute button. Default false. */
    fun showMuteBtn(v: Boolean): WXVideoAttr { PROP_SHOW_MUTE_BTN with v; return this }

    /** Video title, shown at top-left in fullscreen mode. */
    fun title(v: String): WXVideoAttr { PROP_TITLE with v; return this }

    /** Position of play button. See [WXVideoPlayBtnPosition]. */
    fun playBtnPosition(v: String): WXVideoAttr { PROP_PLAY_BTN_POSITION with v; return this }

    /** Whether to enable tap gesture to play/pause. Default false. */
    fun enablePlayGesture(v: Boolean): WXVideoAttr { PROP_ENABLE_PLAY_GESTURE with v; return this }

    /** Auto pause when navigating away. Default true. */
    fun autoPauseIfNavigate(v: Boolean): WXVideoAttr { PROP_AUTO_PAUSE_IF_NAVIGATE with v; return this }

    /** Auto pause when another native component opens. Default true. */
    fun autoPauseIfOpenNative(v: Boolean): WXVideoAttr { PROP_AUTO_PAUSE_IF_OPEN_NATIVE with v; return this }

    /** Enable vertical slide gesture on video (non-fullscreen). Default false. */
    fun vslideGesture(v: Boolean): WXVideoAttr { PROP_VSLIDE_GESTURE with v; return this }

    /** Enable vertical slide gesture in fullscreen. Default true. */
    fun vslideGestureInFullscreen(v: Boolean): WXVideoAttr {
        PROP_VSLIDE_GESTURE_IN_FULLSCREEN with v; return this
    }

    /** Whether to show bottom thin progress bar. Default true. */
    fun showBottomProgress(v: Boolean): WXVideoAttr { PROP_SHOW_BOTTOM_PROGRESS with v; return this }

    /** Ad unit id. */
    fun adUnitId(v: String): WXVideoAttr { PROP_AD_UNIT_ID with v; return this }

    /** Poster used by crawlers. */
    fun posterForCrawler(v: String): WXVideoAttr { PROP_POSTER_FOR_CRAWLER with v; return this }

    /** Whether to show casting (Chromecast / DLNA) button. Default false. */
    fun showCastingButton(v: Boolean): WXVideoAttr { PROP_SHOW_CASTING_BUTTON with v; return this }

    /** PiP modes JSON array string, e.g. `["push","pop"]`. */
    fun pictureInPictureModeJson(json: String): WXVideoAttr {
        PROP_PICTURE_IN_PICTURE_MODE with json; return this
    }

    /** Whether to enable auto rotation on gravity sensor. Default false. */
    fun enableAutoRotation(v: Boolean): WXVideoAttr { PROP_ENABLE_AUTO_ROTATION with v; return this }

    /** Whether to show the screen lock button. Default false. */
    fun showScreenLockButton(v: Boolean): WXVideoAttr { PROP_SHOW_SCREEN_LOCK_BUTTON with v; return this }

    /** Whether to show the snapshot button. Default false. */
    fun showSnapshotButton(v: Boolean): WXVideoAttr { PROP_SHOW_SNAPSHOT_BUTTON with v; return this }

    /** Whether to show background playback button. Default false. */
    fun showBackgroundPlaybackButton(v: Boolean): WXVideoAttr {
        PROP_SHOW_BACKGROUND_PLAYBACK_BUTTON with v; return this
    }

    /** Poster used when background playback is active. */
    fun backgroundPoster(v: String): WXVideoAttr { PROP_BACKGROUND_POSTER with v; return this }

    /** Referrer policy string. Default `no-referrer`. */
    fun referrerPolicy(v: String): WXVideoAttr { PROP_REFERRER_POLICY with v; return this }

    /** Whether the video is DRM protected. Default false. */
    fun isDrm(v: Boolean): WXVideoAttr { PROP_IS_DRM with v; return this }

    /** Whether the video is a live stream. Default false. */
    fun isLive(v: Boolean): WXVideoAttr { PROP_IS_LIVE with v; return this }

    /** DRM provision URL. */
    fun provisionUrl(v: String): WXVideoAttr { PROP_PROVISION_URL with v; return this }

    /** DRM certificate URL. */
    fun certificateUrl(v: String): WXVideoAttr { PROP_CERTIFICATE_URL with v; return this }

    /** DRM license URL. */
    fun licenseUrl(v: String): WXVideoAttr { PROP_LICENSE_URL with v; return this }

    /** Preferred peak bit rate (in bps). */
    fun preferredPeakBitRate(v: Int): WXVideoAttr { PROP_PREFERRED_PEAK_BIT_RATE with v; return this }

    companion object {
        internal const val PROP_SRC = "src"
        internal const val PROP_DURATION = "duration"
        internal const val PROP_CONTROLS = "controls"
        internal const val PROP_DANMU_LIST = "danmuList"
        internal const val PROP_DANMU_BTN = "danmuBtn"
        internal const val PROP_ENABLE_DANMU = "enableDanmu"
        internal const val PROP_AUTOPLAY = "autoplay"
        internal const val PROP_LOOP = "loop"
        internal const val PROP_MUTED = "muted"
        internal const val PROP_INITIAL_TIME = "initialTime"
        internal const val PROP_PAGE_GESTURE = "pageGesture"
        internal const val PROP_DIRECTION = "direction"
        internal const val PROP_SHOW_PROGRESS = "showProgress"
        internal const val PROP_SHOW_FULLSCREEN_BTN = "showFullscreenBtn"
        internal const val PROP_SHOW_PLAY_BTN = "showPlayBtn"
        internal const val PROP_SHOW_CENTER_PLAY_BTN = "showCenterPlayBtn"
        internal const val PROP_ENABLE_PROGRESS_GESTURE = "enableProgressGesture"
        internal const val PROP_OBJECT_FIT = "objectFit"
        internal const val PROP_POSTER = "poster"
        internal const val PROP_SHOW_MUTE_BTN = "showMuteBtn"
        internal const val PROP_TITLE = "title"
        internal const val PROP_PLAY_BTN_POSITION = "playBtnPosition"
        internal const val PROP_ENABLE_PLAY_GESTURE = "enablePlayGesture"
        internal const val PROP_AUTO_PAUSE_IF_NAVIGATE = "autoPauseIfNavigate"
        internal const val PROP_AUTO_PAUSE_IF_OPEN_NATIVE = "autoPauseIfOpenNative"
        internal const val PROP_VSLIDE_GESTURE = "vslideGesture"
        internal const val PROP_VSLIDE_GESTURE_IN_FULLSCREEN = "vslideGestureInFullscreen"
        internal const val PROP_SHOW_BOTTOM_PROGRESS = "showBottomProgress"
        internal const val PROP_AD_UNIT_ID = "adUnitId"
        internal const val PROP_POSTER_FOR_CRAWLER = "posterForCrawler"
        internal const val PROP_SHOW_CASTING_BUTTON = "showCastingButton"
        internal const val PROP_PICTURE_IN_PICTURE_MODE = "pictureInPictureMode"
        internal const val PROP_ENABLE_AUTO_ROTATION = "enableAutoRotation"
        internal const val PROP_SHOW_SCREEN_LOCK_BUTTON = "showScreenLockButton"
        internal const val PROP_SHOW_SNAPSHOT_BUTTON = "showSnapshotButton"
        internal const val PROP_SHOW_BACKGROUND_PLAYBACK_BUTTON = "showBackgroundPlaybackButton"
        internal const val PROP_BACKGROUND_POSTER = "backgroundPoster"
        internal const val PROP_REFERRER_POLICY = "referrerPolicy"
        internal const val PROP_IS_DRM = "isDrm"
        internal const val PROP_IS_LIVE = "isLive"
        internal const val PROP_PROVISION_URL = "provisionUrl"
        internal const val PROP_CERTIFICATE_URL = "certificateUrl"
        internal const val PROP_LICENSE_URL = "licenseUrl"
        internal const val PROP_PREFERRED_PEAK_BIT_RATE = "preferredPeakBitRate"
    }
}

/**
 * Events for [WXVideoView]. All callbacks receive a [JSONObject] whose `data` field is the
 * JSON-serialized native `detail`.
 */
class WXVideoEvent : ComposeEvent() {

    fun onPlay(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_PLAY, handler)
    fun onPause(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_PAUSE, handler)
    fun onEnded(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_ENDED, handler)
    fun onTimeUpdate(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_TIME_UPDATE, handler)
    fun onFullscreenChange(handler: (JSONObject) -> Unit) =
        registerJsonCallback(CALLBACK_FULLSCREEN_CHANGE, handler)
    fun onWaiting(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_WAITING, handler)
    fun onError(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_ERROR, handler)
    fun onProgress(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_PROGRESS, handler)
    fun onLoadedMetadata(handler: (JSONObject) -> Unit) =
        registerJsonCallback(CALLBACK_LOADED_METADATA, handler)
    fun onControlsToggle(handler: (JSONObject) -> Unit) =
        registerJsonCallback(CALLBACK_CONTROLS_TOGGLE, handler)
    fun onEnterPictureInPicture(handler: (JSONObject) -> Unit) =
        registerJsonCallback(CALLBACK_ENTER_PIP, handler)
    fun onLeavePictureInPicture(handler: (JSONObject) -> Unit) =
        registerJsonCallback(CALLBACK_LEAVE_PIP, handler)
    fun onSeekComplete(handler: (JSONObject) -> Unit) =
        registerJsonCallback(CALLBACK_SEEK_COMPLETE, handler)

    private fun registerJsonCallback(eventName: String, handler: (JSONObject) -> Unit) {
        register(eventName) { handler(it as JSONObject) }
    }

    companion object {
        internal const val CALLBACK_PLAY = "playCallback"
        internal const val CALLBACK_PAUSE = "pauseCallback"
        internal const val CALLBACK_ENDED = "endedCallback"
        internal const val CALLBACK_TIME_UPDATE = "timeUpdateCallback"
        internal const val CALLBACK_FULLSCREEN_CHANGE = "fullscreenChangeCallback"
        internal const val CALLBACK_WAITING = "waitingCallback"
        internal const val CALLBACK_ERROR = "errorCallback"
        internal const val CALLBACK_PROGRESS = "progressCallback"
        internal const val CALLBACK_LOADED_METADATA = "loadedMetadataCallback"
        internal const val CALLBACK_CONTROLS_TOGGLE = "controlsToggleCallback"
        internal const val CALLBACK_ENTER_PIP = "enterPictureInPictureCallback"
        internal const val CALLBACK_LEAVE_PIP = "leavePictureInPictureCallback"
        internal const val CALLBACK_SEEK_COMPLETE = "seekCompleteCallback"
    }
}

/**
 * DSL builder for [WXVideoView].
 */
fun ViewContainer<*, *>.WXVideo(init: WXVideoView.() -> Unit) {
    addChild(WXVideoView(), init)
}
