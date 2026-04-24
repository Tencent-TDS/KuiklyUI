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
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.wx.MiniWXCameraViewElement
import org.w3c.dom.Element

/**
 * Render view export for the WeChat mini-program native `camera` component.
 *
 * Delegates property setting and event forwarding to [MiniWXCameraViewElement]. Native
 * events' `detail` is serialized as a JSON string and forwarded under the `data` key.
 */
class KRWXCameraView : IKuiklyRenderViewExport {
    private val cameraElement = MiniWXCameraViewElement()

    override val ele: Element
        get() = cameraElement.unsafeCast<Element>()

    // Event callbacks
    private var stopCallback: KuiklyRenderCallback? = null
    private var errorCallback: KuiklyRenderCallback? = null
    private var initDoneCallback: KuiklyRenderCallback? = null
    private var scanCodeCallback: KuiklyRenderCallback? = null

    init {
        cameraElement.addEventListener(EVENT_STOP, createEventForwarder { stopCallback })
        cameraElement.addEventListener(EVENT_ERROR, createEventForwarder { errorCallback })
        cameraElement.addEventListener(EVENT_INIT_DONE, createEventForwarder { initDoneCallback })
        cameraElement.addEventListener(EVENT_SCAN_CODE, createEventForwarder { scanCodeCallback })
    }

    private fun createEventForwarder(callbackSupplier: () -> KuiklyRenderCallback?): EventHandler {
        return { event: dynamic ->
            val data: dynamic = JSON.stringify(event.detail)
            callbackSupplier()?.invoke(mapOf<String, Any>(KEY_DATA to data.unsafeCast<String>()))
        }
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            PROP_MODE -> { cameraElement.mode = propValue as String; true }
            PROP_RESOLUTION -> { cameraElement.resolution = propValue as String; true }
            PROP_DEVICE_POSITION -> { cameraElement.devicePosition = propValue as String; true }
            PROP_FLASH -> { cameraElement.flash = propValue as String; true }
            PROP_FRAME_SIZE -> { cameraElement.frameSize = propValue as String; true }
            CALLBACK_STOP -> { stopCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true }
            CALLBACK_ERROR -> { errorCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true }
            CALLBACK_INIT_DONE -> {
                initDoneCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true
            }
            CALLBACK_SCAN_CODE -> {
                scanCodeCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true
            }
            else -> super.setProp(propKey, propValue)
        }
    }

    companion object {
        const val VIEW_NAME = "KRWXCameraView"

        // Props
        const val PROP_MODE = "mode"
        const val PROP_RESOLUTION = "resolution"
        const val PROP_DEVICE_POSITION = "devicePosition"
        const val PROP_FLASH = "flash"
        const val PROP_FRAME_SIZE = "frameSize"

        // Callbacks
        const val CALLBACK_STOP = "stopCallback"
        const val CALLBACK_ERROR = "errorCallback"
        const val CALLBACK_INIT_DONE = "initDoneCallback"
        const val CALLBACK_SCAN_CODE = "scanCodeCallback"

        // Mini-program native event names
        private const val EVENT_STOP = "stop"
        private const val EVENT_ERROR = "error"
        private const val EVENT_INIT_DONE = "initdone"
        private const val EVENT_SCAN_CODE = "scancode"

        private const val KEY_DATA = "data"
    }
}
