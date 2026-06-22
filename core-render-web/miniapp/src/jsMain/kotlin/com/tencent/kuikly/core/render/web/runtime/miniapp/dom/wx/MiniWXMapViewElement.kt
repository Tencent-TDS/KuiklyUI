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
 * DOM wrapper for the WeChat mini-program native `map` component.
 *
 * Registered under node name `wx-map`, mapped to `tmpl_0_82` in `base.wxml` via
 * `componentsAlias._num = '82'`. The actual output tag is still the native `<map>`.
 *
 * Array-like attributes (markers / polyline / polygons / circles / controls / includePoints)
 * are passed as JSON strings; WXML forwards them as-is to the native component.
 */
class MiniWXMapViewElement(
    nodeName: String = NODE_NAME,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType) {

    @JsName("longitude")
    var longitude: Double = 0.0
        set(value) { setAttribute("longitude", value); field = value }

    @JsName("latitude")
    var latitude: Double = 0.0
        set(value) { setAttribute("latitude", value); field = value }

    @JsName("scale")
    var scale: Double = 16.0
        set(value) { setAttribute("scale", value); field = value }

    @JsName("minScale")
    var minScale: Double = 3.0
        set(value) { setAttribute("minScale", value); field = value }

    @JsName("maxScale")
    var maxScale: Double = 20.0
        set(value) { setAttribute("maxScale", value); field = value }

    @JsName("markers")
    var markers: String = "[]"
        set(value) { setAttribute("markers", value); field = value }

    @JsName("polyline")
    var polyline: String = "[]"
        set(value) { setAttribute("polyline", value); field = value }

    @JsName("polygons")
    var polygons: String = "[]"
        set(value) { setAttribute("polygons", value); field = value }

    @JsName("circles")
    var circles: String = "[]"
        set(value) { setAttribute("circles", value); field = value }

    @JsName("controls")
    var controls: String = "[]"
        set(value) { setAttribute("controls", value); field = value }

    @JsName("includePoints")
    var includePoints: String = "[]"
        set(value) { setAttribute("includePoints", value); field = value }

    @JsName("showCompass")
    var showCompass: Boolean = false
        set(value) { setAttribute("showCompass", value); field = value }

    @JsName("showScale")
    var showScale: Boolean = false
        set(value) { setAttribute("showScale", value); field = value }

    @JsName("enable3D")
    var enable3D: Boolean = false
        set(value) { setAttribute("enable3D", value); field = value }

    @JsName("enableOverlooking")
    var enableOverlooking: Boolean = false
        set(value) { setAttribute("enableOverlooking", value); field = value }

    @JsName("enableZoom")
    var enableZoom: Boolean = true
        set(value) { setAttribute("enableZoom", value); field = value }

    @JsName("enableScroll")
    var enableScroll: Boolean = true
        set(value) { setAttribute("enableScroll", value); field = value }

    @JsName("enableRotate")
    var enableRotate: Boolean = false
        set(value) { setAttribute("enableRotate", value); field = value }

    @JsName("enableBuilding")
    var enableBuilding: Boolean = false
        set(value) { setAttribute("enableBuilding", value); field = value }

    @JsName("showLocation")
    var showLocation: Boolean = false
        set(value) { setAttribute("showLocation", value); field = value }

    @JsName("enableSatellite")
    var enableSatellite: Boolean = false
        set(value) { setAttribute("enableSatellite", value); field = value }

    @JsName("enableTraffic")
    var enableTraffic: Boolean = false
        set(value) { setAttribute("enableTraffic", value); field = value }

    @JsName("enableIndoorMap")
    var enableIndoorMap: Boolean = false
        set(value) { setAttribute("enableIndoorMap", value); field = value }

    @JsName("layerStyle")
    var layerStyle: Int = 1
        set(value) { setAttribute("layerStyle", value); field = value }

    @JsName("subkey")
    var subkey: String = ""
        set(value) { setAttribute("subkey", value); field = value }

    @JsName("rotate")
    var rotate: Double = 0.0
        set(value) { setAttribute("rotate", value); field = value }

    @JsName("skew")
    var skew: Double = 0.0
        set(value) { setAttribute("skew", value); field = value }

    @JsName("settingId")
    var settingId: String = ""
        set(value) { setAttribute("settingId", value); field = value }

    companion object {
        /** Internal node name. Mini-program output tag is `map`, see template `tmpl_0_82`. */
        const val NODE_NAME = "wx-map"

        /** Component alias. `_num = '82'` matches `tmpl_0_82` in `base.wxml`. */
        val componentsAlias: dynamic = js(
            """
            {
                _num: '82',
                class: 'cl',
                animation: 'p0',
                longitude: 'longitude',
                latitude: 'latitude',
                scale: 'scale',
                minScale: 'minScale',
                maxScale: 'maxScale',
                markers: 'markers',
                polyline: 'polyline',
                polygons: 'polygons',
                circles: 'circles',
                controls: 'controls',
                includePoints: 'includePoints',
                showCompass: 'showCompass',
                showScale: 'showScale',
                enable3D: 'enable3D',
                enableOverlooking: 'enableOverlooking',
                enableZoom: 'enableZoom',
                enableScroll: 'enableScroll',
                enableRotate: 'enableRotate',
                enableBuilding: 'enableBuilding',
                showLocation: 'showLocation',
                enableSatellite: 'enableSatellite',
                enableTraffic: 'enableTraffic',
                enableIndoorMap: 'enableIndoorMap',
                layerStyle: 'layerStyle',
                subkey: 'subkey',
                rotate: 'rotate',
                skew: 'skew',
                settingId: 'settingId'
            }
            """
        )
    }
}
