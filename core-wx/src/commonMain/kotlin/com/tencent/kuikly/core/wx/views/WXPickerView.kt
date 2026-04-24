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
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * WeChat mini-program native `picker` component mode values.
 */
object WXPickerMode {
    /** Single column selector. `range` is a string list or an object list. */
    const val SELECTOR = "selector"

    /** Multi-column selector. `range` is a 2-D list. */
    const val MULTI_SELECTOR = "multiSelector"

    /** Time picker. */
    const val TIME = "time"

    /** Date picker. */
    const val DATE = "date"

    /** Region (province-city-district) picker. */
    const val REGION = "region"
}

/**
 * Cross-platform Compose-style wrapper of the WeChat mini-program native `picker` component.
 *
 * On mini-program platform (`pageData.params.is_miniprogram == "1"`) it renders the native
 * `<picker/>` via `KRWXPickerView`; on other platforms it falls back to a plain view so its
 * children still render normally.
 */
class WXPickerView : ComposeView<WXPickerAttr, WXPickerEvent>() {

    override fun createEvent(): WXPickerEvent = WXPickerEvent()

    override fun createAttr(): WXPickerAttr = WXPickerAttr()

    override fun viewName(): String {
        val pageData = getPager().pageData
        if (pageData.params.optString(IS_MINI_PROGRAM) == "1") {
            return VIEW_NAME_MINI_PROGRAM
        }
        return ViewConst.TYPE_VIEW
    }

    override fun body(): ViewBuilder {
        return {
            // Leave empty; children of WXPicker will be rendered by the host container.
        }
    }

    companion object {
        internal const val IS_MINI_PROGRAM = "is_miniprogram"
        internal const val VIEW_NAME_MINI_PROGRAM = "KRWXPickerView"
    }
}

/**
 * Attributes for [WXPickerView]. Mirrors native `picker` attributes.
 *
 * Because the semantic of `value` / `range` depends on [WXPickerMode], we expose typed helpers
 * and also raw string/JSON setters for flexibility.
 */
class WXPickerAttr : ComposeAttr() {

    /** Picker mode. See [WXPickerMode]. */
    fun mode(v: String): WXPickerAttr { PROP_MODE with v; return this }

    /** Whether the picker is disabled. */
    fun disabled(v: Boolean): WXPickerAttr { PROP_DISABLED with v; return this }

    /** Form control name. */
    fun name(v: String): WXPickerAttr { PROP_NAME with v; return this }

    /**
     * Range for `selector`/`multiSelector` mode as a JSON array string.
     *
     * Example: `range("[\"a\",\"b\",\"c\"]")` or
     * `range("[[\"a1\",\"a2\"],[\"b1\"]]")`.
     */
    fun rangeJson(json: String): WXPickerAttr { PROP_RANGE with json; return this }

    /** Convenience: pass a string list range (single column selector). */
    fun range(list: List<String>): WXPickerAttr {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        PROP_RANGE with arr.toString()
        return this
    }

    /** `range-key` attribute: for object list, the field used as display text. */
    fun rangeKey(v: String): WXPickerAttr { PROP_RANGE_KEY with v; return this }

    /**
     * Current value.
     *
     * - `selector` mode: index as stringified int, e.g. `"0"`.
     * - `multiSelector` mode: JSON array of indices, e.g. `"[0,1]"`.
     * - `time` mode: `"HH:mm"`.
     * - `date` mode: `"yyyy-MM-dd"`.
     * - `region` mode: JSON array of region names, e.g. `"[\"广东省\",\"广州市\",\"海珠区\"]"`.
     */
    fun value(v: String): WXPickerAttr { PROP_VALUE with v; return this }

    /** Selector mode: current selected index. */
    fun valueIndex(index: Int): WXPickerAttr {
        PROP_VALUE with index.toString()
        return this
    }

    /** Multi-selector mode: current selected indices. */
    fun valueIndices(indices: List<Int>): WXPickerAttr {
        val arr = JSONArray()
        indices.forEach { arr.put(it) }
        PROP_VALUE with arr.toString()
        return this
    }

    /** Date / time lower bound. */
    fun start(v: String): WXPickerAttr { PROP_START with v; return this }

    /** Date / time upper bound. */
    fun end(v: String): WXPickerAttr { PROP_END with v; return this }

    /** Date picker granularity: `year` / `month` / `day`. */
    fun fields(v: String): WXPickerAttr { PROP_FIELDS with v; return this }

    /** Custom header text. */
    fun headerText(v: String): WXPickerAttr { PROP_HEADER_TEXT with v; return this }

    /** Whether the picker is inside a fixed-position layer. */
    fun fixed(v: Boolean): WXPickerAttr { PROP_FIXED with v; return this }

    companion object {
        internal const val PROP_MODE = "mode"
        internal const val PROP_DISABLED = "disabled"
        internal const val PROP_NAME = "name"
        internal const val PROP_RANGE = "range"
        internal const val PROP_RANGE_KEY = "rangeKey"
        internal const val PROP_VALUE = "value"
        internal const val PROP_START = "start"
        internal const val PROP_END = "end"
        internal const val PROP_FIELDS = "fields"
        internal const val PROP_HEADER_TEXT = "headerText"
        internal const val PROP_FIXED = "fixed"
    }
}

/**
 * Events for [WXPickerView]. Every callback receives a [JSONObject] whose `data` is the
 * JSON-serialized native `detail`.
 */
class WXPickerEvent : ComposeEvent() {

    /** Called when a valid pick is confirmed (tap "Done"). */
    fun onChange(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_CHANGE, handler)

    /** Called when column changes in `multiSelector` mode. */
    fun onColumnChange(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_COLUMN_CHANGE, handler)

    /** Called when the picker is cancelled. */
    fun onCancel(handler: (JSONObject) -> Unit) = registerJsonCallback(CALLBACK_CANCEL, handler)

    private fun registerJsonCallback(eventName: String, handler: (JSONObject) -> Unit) {
        register(eventName) { handler(it as JSONObject) }
    }

    companion object {
        internal const val CALLBACK_CHANGE = "changeCallback"
        internal const val CALLBACK_COLUMN_CHANGE = "columnChangeCallback"
        internal const val CALLBACK_CANCEL = "cancelCallback"
    }
}

/**
 * DSL builder for [WXPickerView].
 *
 * `WXPicker` is a container: you usually put a child View / Text inside it to describe what
 * the user taps to open the picker wheel.
 */
fun ViewContainer<*, *>.WXPicker(init: WXPickerView.() -> Unit) {
    addChild(WXPickerView(), init)
}
