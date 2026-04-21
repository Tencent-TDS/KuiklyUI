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
 * WeChat mini-program native `map` component layer style values.
 */
object WXMapLayerStyle {
    const val NORMAL = 1
    const val SATELLITE = 2
}

/**
 * Cross-platform Compose-style wrapper of the WeChat mini-program native `map` component.
 *
 * On mini-program platform (`pageData.params.is_miniprogram == "1"`) it renders the native
 * `<map/>` via `KRWXMapView`; on other platforms it falls back to a plain view so the
 * compose tree still renders.
 */
class WXMapView : ComposeView<WXMapAttr, WXMapEvent>() {

    override fun createEvent(): WXMapEvent = WXMapEvent()

    override fun createAttr(): WXMapAttr = WXMapAttr()

    override fun viewName(): String {
        val pageData = getPager().pageData
        if (pageData.params.optString(IS_MINI_PROGRAM) == "1") {
            return VIEW_NAME_MINI_PROGRAM
        }
        return ViewConst.TYPE_VIEW
    }

    override fun body(): ViewBuilder {
        return {
            // Leave empty; native <map> renders itself.
        }
    }

    companion object {
        internal const val IS_MINI_PROGRAM = "is_miniprogram"
        internal const val VIEW_NAME_MINI_PROGRAM = "KRWXMapView"
    }
}

/**
 * Attributes for [WXMapView]. Mirrors native `map` attributes.
 *
 * Collection-typed properties (markers / polyline / polygons / circles / controls / includePoints)
 * are accepted as JSON strings to keep the cross-platform API simple.
 */
class WXMapAttr : ComposeAttr() {

    /** Longitude of the map center. Required. */
    fun longitude(v: Double): WXMapAttr { PROP_LONGITUDE with v; return this }

    /** Latitude of the map center. Required. */
    fun latitude(v: Double): WXMapAttr { PROP_LATITUDE with v; return this }

    /** Zoom level. Default 16. Range: [3, 20]. */
    fun scale(v: Double): WXMapAttr { PROP_SCALE with v; return this }

    /** Minimum zoom level. Default 3. */
    fun minScale(v: Double): WXMapAttr { PROP_MIN_SCALE with v; return this }

    /** Maximum zoom level. Default 20. */
    fun maxScale(v: Double): WXMapAttr { PROP_MAX_SCALE with v; return this }

    /** Markers JSON array string. */
    fun markersJson(json: String): WXMapAttr { PROP_MARKERS with json; return this }

    /** Polyline JSON array string. */
    fun polylineJson(json: String): WXMapAttr { PROP_POLYLINE with json; return this }

    /** Polygons JSON array string. */
    fun polygonsJson(json: String): WXMapAttr { PROP_POLYGONS with json; return this }

    /** Circles JSON array string. */
    fun circlesJson(json: String): WXMapAttr { PROP_CIRCLES with json; return this }

    /** Controls JSON array string. (Deprecated by WeChat, kept for compatibility.) */
    fun controlsJson(json: String): WXMapAttr { PROP_CONTROLS with json; return this }

    /** Include-points JSON array string, used to auto-fit viewport. */
    fun includePointsJson(json: String): WXMapAttr { PROP_INCLUDE_POINTS with json; return this }

    /** Show compass on the map. Default false. */
    fun showCompass(v: Boolean): WXMapAttr { PROP_SHOW_COMPASS with v; return this }

    /** Show scale on the map. Default false. */
    fun showScale(v: Boolean): WXMapAttr { PROP_SHOW_SCALE with v; return this }

    /** Enable 3D perspective. Default false. */
    fun enable3D(v: Boolean): WXMapAttr { PROP_ENABLE_3D with v; return this }

    /** Enable gesture overlook. Default false. */
    fun enableOverlooking(v: Boolean): WXMapAttr { PROP_ENABLE_OVERLOOKING with v; return this }

    /** Enable user-zoom gesture. Default true. */
    fun enableZoom(v: Boolean): WXMapAttr { PROP_ENABLE_ZOOM with v; return this }

    /** Enable user-scroll gesture. Default true. */
    fun enableScroll(v: Boolean): WXMapAttr { PROP_ENABLE_SCROLL with v; return this }

    /** Enable user-rotate gesture. Default false. */
    fun enableRotate(v: Boolean): WXMapAttr { PROP_ENABLE_ROTATE with v; return this }

    /** Enable building 3D render. Default false. */
    fun enableBuilding(v: Boolean): WXMapAttr { PROP_ENABLE_BUILDING with v; return this }

    /** Show location indicator. Default false. */
    fun showLocation(v: Boolean): WXMapAttr { PROP_SHOW_LOCATION with v; return this }

    /** Enable satellite layer. Default false. */
    fun enableSatellite(v: Boolean): WXMapAttr { PROP_ENABLE_SATELLITE with v; return this }

    /** Enable real-time road traffic layer. Default false. */
    fun enableTraffic(v: Boolean): WXMapAttr { PROP_ENABLE_TRAFFIC with v; return this }

    /** Show indoor map (sub-scene). Default false. */
    fun enableIndoorMap(v: Boolean): WXMapAttr { PROP_ENABLE_INDOOR_MAP with v; return this }

    /** Setting style id. Default 1. See [WXMapLayerStyle]. */
    fun layerStyle(v: Int): WXMapAttr { PROP_LAYER_STYLE with v; return this }

    /** Subkey (personalized map style identifier). */
    fun subkey(v: String): WXMapAttr { PROP_SUBKEY with v; return this }

    /** Map rotation angle in degrees, 0~360. Default 0. */
    fun rotate(v: Double): WXMapAttr { PROP_ROTATE with v; return this }

    /** Map skew angle, 0~40. Default 0. */
    fun skew(v: Double): WXMapAttr { PROP_SKEW with v; return this }

    /** Setting object id for custom style. */
    fun settingId(v: String): WXMapAttr { PROP_SETTING_ID with v; return this }

    companion object {
        internal const val PROP_LONGITUDE = "longitude"
        internal const val PROP_LATITUDE = "latitude"
        internal const val PROP_SCALE = "scale"
        internal const val PROP_MIN_SCALE = "minScale"
        internal const val PROP_MAX_SCALE = "maxScale"
        internal const val PROP_MARKERS = "markers"
        internal const val PROP_POLYLINE = "polyline"
        internal const val PROP_POLYGONS = "polygons"
        internal const val PROP_CIRCLES = "circles"
        internal const val PROP_CONTROLS = "controls"
        internal const val PROP_INCLUDE_POINTS = "includePoints"
        internal const val PROP_SHOW_COMPASS = "showCompass"
        internal const val PROP_SHOW_SCALE = "showScale"
        internal const val PROP_ENABLE_3D = "enable3D"
        internal const val PROP_ENABLE_OVERLOOKING = "enableOverlooking"
        internal const val PROP_ENABLE_ZOOM = "enableZoom"
        internal const val PROP_ENABLE_SCROLL = "enableScroll"
        internal const val PROP_ENABLE_ROTATE = "enableRotate"
        internal const val PROP_ENABLE_BUILDING = "enableBuilding"
        internal const val PROP_SHOW_LOCATION = "showLocation"
        internal const val PROP_ENABLE_SATELLITE = "enableSatellite"
        internal const val PROP_ENABLE_TRAFFIC = "enableTraffic"
        internal const val PROP_ENABLE_INDOOR_MAP = "enableIndoorMap"
        internal const val PROP_LAYER_STYLE = "layerStyle"
        internal const val PROP_SUBKEY = "subkey"
        internal const val PROP_ROTATE = "rotate"
        internal const val PROP_SKEW = "skew"
        internal const val PROP_SETTING_ID = "settingId"
    }
}

/**
 * Events for [WXMapView]. All callbacks receive a [JSONObject] whose `data` field is the
 * JSON-serialized native `detail`.
 */
class WXMapEvent : ComposeEvent() {

    fun onTap(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_TAP, handler)
    fun onMarkerTap(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_MARKER_TAP, handler)
    fun onControlTap(handler: (JSONObject) -> Unit) =
        registerJsonCallback(CALLBACK_CONTROL_TAP, handler)
    fun onCalloutTap(handler: (JSONObject) -> Unit) =
        registerJsonCallback(CALLBACK_CALLOUT_TAP, handler)
    fun onLabelTap(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_LABEL_TAP, handler)
    fun onUpdated(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_UPDATED, handler)
    fun onRegionChange(handler: (JSONObject) -> Unit) =
        registerJsonCallback(CALLBACK_REGION_CHANGE, handler)
    fun onPoiTap(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_POI_TAP, handler)
    fun onAnchorPointTap(handler: (JSONObject) -> Unit) =
        registerJsonCallback(CALLBACK_ANCHOR_POINT_TAP, handler)
    fun onError(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_ERROR, handler)

    private fun registerJsonCallback(eventName: String, handler: (JSONObject) -> Unit) {
        register(eventName) { handler(it as JSONObject) }
    }

    companion object {
        internal const val CALLBACK_TAP = "tapCallback"
        internal const val CALLBACK_MARKER_TAP = "markerTapCallback"
        internal const val CALLBACK_CONTROL_TAP = "controlTapCallback"
        internal const val CALLBACK_CALLOUT_TAP = "calloutTapCallback"
        internal const val CALLBACK_LABEL_TAP = "labelTapCallback"
        internal const val CALLBACK_UPDATED = "updatedCallback"
        internal const val CALLBACK_REGION_CHANGE = "regionChangeCallback"
        internal const val CALLBACK_POI_TAP = "poiTapCallback"
        internal const val CALLBACK_ANCHOR_POINT_TAP = "anchorPointTapCallback"
        internal const val CALLBACK_ERROR = "errorCallback"
    }
}

/**
 * DSL builder for [WXMapView].
 */
fun ViewContainer<*, *>.WXMap(init: WXMapView.() -> Unit) {
    addChild(WXMapView(), init)
}
