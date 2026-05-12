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

package com.tencent.kuikly.core.render.web.runtime.miniapp.dom.wx

import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniElementUtil

/**
 * DOM wrapper for the WeChat mini-program native `video` component.
 *
 * The framework's built-in video uses `tmpl_0_72` with `p0..p44` indexed aliases. To avoid
 * conflicting with that, we register as `wx-video` and bind a dedicated template
 * `tmpl_0_80` that uses direct attribute names.
 */
class MiniWXVideoViewElement(
    nodeName: String = NODE_NAME,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType) {

    @JsName("src")
    var src: String = ""
        set(value) { setAttribute("src", value); field = value }

    @JsName("duration")
    var duration: Int = 0
        set(value) { setAttribute("duration", value); field = value }

    @JsName("controls")
    var controls: Boolean = true
        set(value) { setAttribute("controls", value); field = value }

    /** Danmu list, passed as JSON string; WXML sends it to the native component as-is. */
    @JsName("danmuList")
    var danmuList: String = "[]"
        set(value) { setAttribute("danmuList", value); field = value }

    @JsName("danmuBtn")
    var danmuBtn: Boolean = false
        set(value) { setAttribute("danmuBtn", value); field = value }

    @JsName("enableDanmu")
    var enableDanmu: Boolean = false
        set(value) { setAttribute("enableDanmu", value); field = value }

    @JsName("autoplay")
    var autoplay: Boolean = false
        set(value) { setAttribute("autoplay", value); field = value }

    @JsName("loop")
    var loop: Boolean = false
        set(value) { setAttribute("loop", value); field = value }

    @JsName("muted")
    var muted: Boolean = false
        set(value) { setAttribute("muted", value); field = value }

    @JsName("initialTime")
    var initialTime: Int = 0
        set(value) { setAttribute("initialTime", value); field = value }

    @JsName("pageGesture")
    var pageGesture: Boolean = false
        set(value) { setAttribute("pageGesture", value); field = value }

    @JsName("direction")
    var direction: Int = 0
        set(value) { setAttribute("direction", value); field = value }

    @JsName("showProgress")
    var showProgress: Boolean = true
        set(value) { setAttribute("showProgress", value); field = value }

    @JsName("showFullscreenBtn")
    var showFullscreenBtn: Boolean = true
        set(value) { setAttribute("showFullscreenBtn", value); field = value }

    @JsName("showPlayBtn")
    var showPlayBtn: Boolean = true
        set(value) { setAttribute("showPlayBtn", value); field = value }

    @JsName("showCenterPlayBtn")
    var showCenterPlayBtn: Boolean = true
        set(value) { setAttribute("showCenterPlayBtn", value); field = value }

    @JsName("enableProgressGesture")
    var enableProgressGesture: Boolean = true
        set(value) { setAttribute("enableProgressGesture", value); field = value }

    @JsName("objectFit")
    var objectFit: String = "contain"
        set(value) { setAttribute("objectFit", value); field = value }

    @JsName("poster")
    var poster: String = ""
        set(value) { setAttribute("poster", value); field = value }

    @JsName("showMuteBtn")
    var showMuteBtn: Boolean = false
        set(value) { setAttribute("showMuteBtn", value); field = value }

    @JsName("title")
    var title: String = ""
        set(value) { setAttribute("title", value); field = value }

    @JsName("playBtnPosition")
    var playBtnPosition: String = "bottom"
        set(value) { setAttribute("playBtnPosition", value); field = value }

    @JsName("enablePlayGesture")
    var enablePlayGesture: Boolean = false
        set(value) { setAttribute("enablePlayGesture", value); field = value }

    @JsName("autoPauseIfNavigate")
    var autoPauseIfNavigate: Boolean = true
        set(value) { setAttribute("autoPauseIfNavigate", value); field = value }

    @JsName("autoPauseIfOpenNative")
    var autoPauseIfOpenNative: Boolean = true
        set(value) { setAttribute("autoPauseIfOpenNative", value); field = value }

    @JsName("vslideGesture")
    var vslideGesture: Boolean = false
        set(value) { setAttribute("vslideGesture", value); field = value }

    @JsName("vslideGestureInFullscreen")
    var vslideGestureInFullscreen: Boolean = true
        set(value) { setAttribute("vslideGestureInFullscreen", value); field = value }

    @JsName("showBottomProgress")
    var showBottomProgress: Boolean = true
        set(value) { setAttribute("showBottomProgress", value); field = value }

    @JsName("adUnitId")
    var adUnitId: String = ""
        set(value) { setAttribute("adUnitId", value); field = value }

    @JsName("posterForCrawler")
    var posterForCrawler: String = ""
        set(value) { setAttribute("posterForCrawler", value); field = value }

    @JsName("showCastingButton")
    var showCastingButton: Boolean = false
        set(value) { setAttribute("showCastingButton", value); field = value }

    /** Picture-in-picture modes, passed as JSON string. */
    @JsName("pictureInPictureMode")
    var pictureInPictureMode: String = "[]"
        set(value) { setAttribute("pictureInPictureMode", value); field = value }

    @JsName("enableAutoRotation")
    var enableAutoRotation: Boolean = false
        set(value) { setAttribute("enableAutoRotation", value); field = value }

    @JsName("showScreenLockButton")
    var showScreenLockButton: Boolean = false
        set(value) { setAttribute("showScreenLockButton", value); field = value }

    @JsName("showSnapshotButton")
    var showSnapshotButton: Boolean = false
        set(value) { setAttribute("showSnapshotButton", value); field = value }

    @JsName("showBackgroundPlaybackButton")
    var showBackgroundPlaybackButton: Boolean = false
        set(value) { setAttribute("showBackgroundPlaybackButton", value); field = value }

    @JsName("backgroundPoster")
    var backgroundPoster: String = ""
        set(value) { setAttribute("backgroundPoster", value); field = value }

    @JsName("referrerPolicy")
    var referrerPolicy: String = "no-referrer"
        set(value) { setAttribute("referrerPolicy", value); field = value }

    @JsName("isDrm")
    var isDrm: Boolean = false
        set(value) { setAttribute("isDrm", value); field = value }

    @JsName("isLive")
    var isLive: Boolean = false
        set(value) { setAttribute("isLive", value); field = value }

    @JsName("provisionUrl")
    var provisionUrl: String = ""
        set(value) { setAttribute("provisionUrl", value); field = value }

    @JsName("certificateUrl")
    var certificateUrl: String = ""
        set(value) { setAttribute("certificateUrl", value); field = value }

    @JsName("licenseUrl")
    var licenseUrl: String = ""
        set(value) { setAttribute("licenseUrl", value); field = value }

    @JsName("preferredPeakBitRate")
    var preferredPeakBitRate: Int = 0
        set(value) { setAttribute("preferredPeakBitRate", value); field = value }

    companion object {
        /** Internal node name. Mini-program output tag is `video`, see template `tmpl_0_80`. */
        const val NODE_NAME = "wx-video"

        /** Component alias. `_num = '80'` matches `tmpl_0_80` in `base.wxml`. */
        val componentsAlias: dynamic = js(
            """
            {
                _num: '80',
                class: 'cl',
                animation: 'p0',
                src: 'src',
                duration: 'duration',
                controls: 'controls',
                danmuList: 'danmuList',
                danmuBtn: 'danmuBtn',
                enableDanmu: 'enableDanmu',
                autoplay: 'autoplay',
                loop: 'loop',
                muted: 'muted',
                initialTime: 'initialTime',
                pageGesture: 'pageGesture',
                direction: 'direction',
                showProgress: 'showProgress',
                showFullscreenBtn: 'showFullscreenBtn',
                showPlayBtn: 'showPlayBtn',
                showCenterPlayBtn: 'showCenterPlayBtn',
                enableProgressGesture: 'enableProgressGesture',
                objectFit: 'objectFit',
                poster: 'poster',
                showMuteBtn: 'showMuteBtn',
                title: 'title',
                playBtnPosition: 'playBtnPosition',
                enablePlayGesture: 'enablePlayGesture',
                autoPauseIfNavigate: 'autoPauseIfNavigate',
                autoPauseIfOpenNative: 'autoPauseIfOpenNative',
                vslideGesture: 'vslideGesture',
                vslideGestureInFullscreen: 'vslideGestureInFullscreen',
                showBottomProgress: 'showBottomProgress',
                adUnitId: 'adUnitId',
                posterForCrawler: 'posterForCrawler',
                showCastingButton: 'showCastingButton',
                pictureInPictureMode: 'pictureInPictureMode',
                enableAutoRotation: 'enableAutoRotation',
                showScreenLockButton: 'showScreenLockButton',
                showSnapshotButton: 'showSnapshotButton',
                showBackgroundPlaybackButton: 'showBackgroundPlaybackButton',
                backgroundPoster: 'backgroundPoster',
                referrerPolicy: 'referrerPolicy',
                isDrm: 'isDrm',
                isLive: 'isLive',
                provisionUrl: 'provisionUrl',
                certificateUrl: 'certificateUrl',
                licenseUrl: 'licenseUrl',
                preferredPeakBitRate: 'preferredPeakBitRate'
            }
            """
        )
    }
}
