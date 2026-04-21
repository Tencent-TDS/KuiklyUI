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

package com.tencent.kuikly.core.views.wx

import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * WeChat mini-program native `camera` component `mode` values.
 */
object WXCameraMode {
    /** Normal photo/record mode. */
    const val NORMAL = "normal"
    /** Scan code mode (barcode / QR). */
    const val SCAN_CODE = "scanCode"
}

/**
 * WeChat mini-program native `camera` component `resolution` values.
 */
object WXCameraResolution {
    const val LOW = "low"
    const val MEDIUM = "medium"
    const val HIGH = "high"
}

/**
 * WeChat mini-program native `camera` component `device-position` values.
 */
object WXCameraDevicePosition {
    const val FRONT = "front"
    const val BACK = "back"
}

/**
 * WeChat mini-program native `camera` component `flash` values.
 */
object WXCameraFlash {
    const val AUTO = "auto"
    const val ON = "on"
    const val OFF = "off"
    /** Torch / always-on flashlight. */
    const val TORCH = "torch"
}

/**
 * WeChat mini-program native `camera` component `frame-size` values.
 */
object WXCameraFrameSize {
    const val SMALL = "small"
    const val MEDIUM = "medium"
    const val LARGE = "large"
}

/**
 * Cross-platform Compose-style wrapper of the WeChat mini-program native `camera` component.
 *
 * On mini-program platform (`pageData.params.is_miniprogram == "1"`) it renders the native
 * `<camera/>` via `KRWXCameraView`; on other platforms it falls back to a plain view so the
 * compose tree still renders.
 */
class WXCameraView : ComposeView<WXCameraAttr, WXCameraEvent>() {

    override fun createEvent(): WXCameraEvent = WXCameraEvent()

    override fun createAttr(): WXCameraAttr = WXCameraAttr()

    override fun viewName(): String {
        val pageData = getPager().pageData
        if (pageData.params.optString(IS_MINI_PROGRAM) == "1") {
            return VIEW_NAME_MINI_PROGRAM
        }
        return ViewConst.TYPE_VIEW
    }

    override fun body(): ViewBuilder {
        return {
            // Leave empty; native <camera> renders itself.
        }
    }

    companion object {
        internal const val IS_MINI_PROGRAM = "is_miniprogram"
        internal const val VIEW_NAME_MINI_PROGRAM = "KRWXCameraView"
    }
}

/**
 * Attributes for [WXCameraView]. Mirrors native `camera` attributes.
 */
class WXCameraAttr : ComposeAttr() {

    /** Camera mode. See [WXCameraMode]. Default `normal`. */
    fun mode(v: String): WXCameraAttr { PROP_MODE with v; return this }

    /** Camera resolution. See [WXCameraResolution]. Default `medium`. */
    fun resolution(v: String): WXCameraAttr { PROP_RESOLUTION with v; return this }

    /** Front or back camera. See [WXCameraDevicePosition]. Default `back`. */
    fun devicePosition(v: String): WXCameraAttr { PROP_DEVICE_POSITION with v; return this }

    /** Flash mode. See [WXCameraFlash]. Default `auto`. */
    fun flash(v: String): WXCameraAttr { PROP_FLASH with v; return this }

    /** Frame data size. See [WXCameraFrameSize]. Default `medium`. */
    fun frameSize(v: String): WXCameraAttr { PROP_FRAME_SIZE with v; return this }

    companion object {
        internal const val PROP_MODE = "mode"
        internal const val PROP_RESOLUTION = "resolution"
        internal const val PROP_DEVICE_POSITION = "devicePosition"
        internal const val PROP_FLASH = "flash"
        internal const val PROP_FRAME_SIZE = "frameSize"
    }
}

/**
 * Events for [WXCameraView]. All callbacks receive a [JSONObject] whose `data` field is the
 * JSON-serialized native `detail`.
 */
class WXCameraEvent : ComposeEvent() {

    /** Fired when a non-fatal error happens (e.g. permission denied, user cancel). */
    fun onStop(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_STOP, handler)

    /** Fired when camera throws an error. */
    fun onError(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_ERROR, handler)

    /** Fired when initial frame is ready (camera first usable). */
    fun onInitDone(handler: (JSONObject) -> Unit) =
        registerJsonCallback(CALLBACK_INIT_DONE, handler)

    /** Fired when the resolution / format changes. */
    fun onScanCode(handler: (JSONObject) -> Unit) =
        registerJsonCallback(CALLBACK_SCAN_CODE, handler)

    private fun registerJsonCallback(eventName: String, handler: (JSONObject) -> Unit) {
        register(eventName) { handler(it as JSONObject) }
    }

    companion object {
        internal const val CALLBACK_STOP = "stopCallback"
        internal const val CALLBACK_ERROR = "errorCallback"
        internal const val CALLBACK_INIT_DONE = "initDoneCallback"
        internal const val CALLBACK_SCAN_CODE = "scanCodeCallback"
    }
}

/**
 * DSL builder for [WXCameraView].
 */
fun ViewContainer<*, *>.WXCamera(init: WXCameraView.() -> Unit) {
    addChild(WXCameraView(), init)
}
