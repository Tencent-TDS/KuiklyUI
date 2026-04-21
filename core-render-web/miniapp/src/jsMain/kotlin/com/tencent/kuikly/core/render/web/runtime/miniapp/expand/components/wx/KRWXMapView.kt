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
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.wx.MiniWXMapViewElement
import org.w3c.dom.Element

/**
 * Render view export for the WeChat mini-program native `map` component.
 *
 * Delegates property setting and event forwarding to [MiniWXMapViewElement]. Native
 * events' `detail` is serialized as a JSON string and forwarded under the `data` key.
 */
class KRWXMapView : IKuiklyRenderViewExport {
    private val mapElement = MiniWXMapViewElement()

    override val ele: Element
        get() = mapElement.unsafeCast<Element>()

    // Event callbacks
    private var tapCallback: KuiklyRenderCallback? = null
    private var markerTapCallback: KuiklyRenderCallback? = null
    private var controlTapCallback: KuiklyRenderCallback? = null
    private var calloutTapCallback: KuiklyRenderCallback? = null
    private var labelTapCallback: KuiklyRenderCallback? = null
    private var updatedCallback: KuiklyRenderCallback? = null
    private var regionChangeCallback: KuiklyRenderCallback? = null
    private var poiTapCallback: KuiklyRenderCallback? = null
    private var anchorPointTapCallback: KuiklyRenderCallback? = null
    private var errorCallback: KuiklyRenderCallback? = null

    init {
        mapElement.addEventListener(EVENT_TAP, createEventForwarder { tapCallback })
        mapElement.addEventListener(EVENT_MARKER_TAP, createEventForwarder { markerTapCallback })
        mapElement.addEventListener(EVENT_CONTROL_TAP, createEventForwarder { controlTapCallback })
        mapElement.addEventListener(EVENT_CALLOUT_TAP, createEventForwarder { calloutTapCallback })
        mapElement.addEventListener(EVENT_LABEL_TAP, createEventForwarder { labelTapCallback })
        mapElement.addEventListener(EVENT_UPDATED, createEventForwarder { updatedCallback })
        mapElement.addEventListener(
            EVENT_REGION_CHANGE,
            createEventForwarder { regionChangeCallback }
        )
        mapElement.addEventListener(EVENT_POI_TAP, createEventForwarder { poiTapCallback })
        mapElement.addEventListener(
            EVENT_ANCHOR_POINT_TAP,
            createEventForwarder { anchorPointTapCallback }
        )
        mapElement.addEventListener(EVENT_ERROR, createEventForwarder { errorCallback })
    }

    private fun createEventForwarder(callbackSupplier: () -> KuiklyRenderCallback?): EventHandler {
        return { event: dynamic ->
            val data: dynamic = JSON.stringify(event.detail)
            callbackSupplier()?.invoke(mapOf<String, Any>(KEY_DATA to data.unsafeCast<String>()))
        }
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            PROP_LONGITUDE -> { mapElement.longitude = toDouble(propValue); true }
            PROP_LATITUDE -> { mapElement.latitude = toDouble(propValue); true }
            PROP_SCALE -> { mapElement.scale = toDouble(propValue); true }
            PROP_MIN_SCALE -> { mapElement.minScale = toDouble(propValue); true }
            PROP_MAX_SCALE -> { mapElement.maxScale = toDouble(propValue); true }
            PROP_MARKERS -> { mapElement.markers = propValue as String; true }
            PROP_POLYLINE -> { mapElement.polyline = propValue as String; true }
            PROP_POLYGONS -> { mapElement.polygons = propValue as String; true }
            PROP_CIRCLES -> { mapElement.circles = propValue as String; true }
            PROP_CONTROLS -> { mapElement.controls = propValue as String; true }
            PROP_INCLUDE_POINTS -> { mapElement.includePoints = propValue as String; true }
            PROP_SHOW_COMPASS -> { mapElement.showCompass = toBoolean(propValue); true }
            PROP_SHOW_SCALE -> { mapElement.showScale = toBoolean(propValue); true }
            PROP_ENABLE_3D -> { mapElement.enable3D = toBoolean(propValue); true }
            PROP_ENABLE_OVERLOOKING -> {
                mapElement.enableOverlooking = toBoolean(propValue); true
            }
            PROP_ENABLE_ZOOM -> { mapElement.enableZoom = toBoolean(propValue); true }
            PROP_ENABLE_SCROLL -> { mapElement.enableScroll = toBoolean(propValue); true }
            PROP_ENABLE_ROTATE -> { mapElement.enableRotate = toBoolean(propValue); true }
            PROP_ENABLE_BUILDING -> { mapElement.enableBuilding = toBoolean(propValue); true }
            PROP_SHOW_LOCATION -> { mapElement.showLocation = toBoolean(propValue); true }
            PROP_ENABLE_SATELLITE -> { mapElement.enableSatellite = toBoolean(propValue); true }
            PROP_ENABLE_TRAFFIC -> { mapElement.enableTraffic = toBoolean(propValue); true }
            PROP_ENABLE_INDOOR_MAP -> {
                mapElement.enableIndoorMap = toBoolean(propValue); true
            }
            PROP_LAYER_STYLE -> { mapElement.layerStyle = toInt(propValue); true }
            PROP_SUBKEY -> { mapElement.subkey = propValue as String; true }
            PROP_ROTATE -> { mapElement.rotate = toDouble(propValue); true }
            PROP_SKEW -> { mapElement.skew = toDouble(propValue); true }
            PROP_SETTING_ID -> { mapElement.settingId = propValue as String; true }
            CALLBACK_TAP -> { tapCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true }
            CALLBACK_MARKER_TAP -> {
                markerTapCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true
            }
            CALLBACK_CONTROL_TAP -> {
                controlTapCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true
            }
            CALLBACK_CALLOUT_TAP -> {
                calloutTapCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true
            }
            CALLBACK_LABEL_TAP -> {
                labelTapCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true
            }
            CALLBACK_UPDATED -> {
                updatedCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true
            }
            CALLBACK_REGION_CHANGE -> {
                regionChangeCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true
            }
            CALLBACK_POI_TAP -> {
                poiTapCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true
            }
            CALLBACK_ANCHOR_POINT_TAP -> {
                anchorPointTapCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true
            }
            CALLBACK_ERROR -> {
                errorCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true
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

    private fun toDouble(value: Any): Double = when (value) {
        is Double -> value
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: 0.0
        else -> 0.0
    }

    companion object {
        const val VIEW_NAME = "KRWXMapView"

        // Props
        const val PROP_LONGITUDE = "longitude"
        const val PROP_LATITUDE = "latitude"
        const val PROP_SCALE = "scale"
        const val PROP_MIN_SCALE = "minScale"
        const val PROP_MAX_SCALE = "maxScale"
        const val PROP_MARKERS = "markers"
        const val PROP_POLYLINE = "polyline"
        const val PROP_POLYGONS = "polygons"
        const val PROP_CIRCLES = "circles"
        const val PROP_CONTROLS = "controls"
        const val PROP_INCLUDE_POINTS = "includePoints"
        const val PROP_SHOW_COMPASS = "showCompass"
        const val PROP_SHOW_SCALE = "showScale"
        const val PROP_ENABLE_3D = "enable3D"
        const val PROP_ENABLE_OVERLOOKING = "enableOverlooking"
        const val PROP_ENABLE_ZOOM = "enableZoom"
        const val PROP_ENABLE_SCROLL = "enableScroll"
        const val PROP_ENABLE_ROTATE = "enableRotate"
        const val PROP_ENABLE_BUILDING = "enableBuilding"
        const val PROP_SHOW_LOCATION = "showLocation"
        const val PROP_ENABLE_SATELLITE = "enableSatellite"
        const val PROP_ENABLE_TRAFFIC = "enableTraffic"
        const val PROP_ENABLE_INDOOR_MAP = "enableIndoorMap"
        const val PROP_LAYER_STYLE = "layerStyle"
        const val PROP_SUBKEY = "subkey"
        const val PROP_ROTATE = "rotate"
        const val PROP_SKEW = "skew"
        const val PROP_SETTING_ID = "settingId"

        // Callbacks
        const val CALLBACK_TAP = "tapCallback"
        const val CALLBACK_MARKER_TAP = "markerTapCallback"
        const val CALLBACK_CONTROL_TAP = "controlTapCallback"
        const val CALLBACK_CALLOUT_TAP = "calloutTapCallback"
        const val CALLBACK_LABEL_TAP = "labelTapCallback"
        const val CALLBACK_UPDATED = "updatedCallback"
        const val CALLBACK_REGION_CHANGE = "regionChangeCallback"
        const val CALLBACK_POI_TAP = "poiTapCallback"
        const val CALLBACK_ANCHOR_POINT_TAP = "anchorPointTapCallback"
        const val CALLBACK_ERROR = "errorCallback"

        // Mini-program native event names
        private const val EVENT_TAP = "tap"
        private const val EVENT_MARKER_TAP = "markertap"
        private const val EVENT_CONTROL_TAP = "controltap"
        private const val EVENT_CALLOUT_TAP = "callouttap"
        private const val EVENT_LABEL_TAP = "labeltap"
        private const val EVENT_UPDATED = "updated"
        private const val EVENT_REGION_CHANGE = "regionchange"
        private const val EVENT_POI_TAP = "poitap"
        private const val EVENT_ANCHOR_POINT_TAP = "anchorpointtap"
        private const val EVENT_ERROR = "error"

        private const val KEY_DATA = "data"
    }
}
