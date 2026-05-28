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
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.wx.MiniWXPickerViewElement
import org.w3c.dom.Element

/**
 * Render view export for the WeChat mini-program native `picker` component.
 *
 * Delegates property setting and event forwarding to [MiniWXPickerViewElement].
 * Serves as a container: children inserted via standard Kuikly DOM operations will be
 * rendered inside the `<picker>` through `tmpl_0_79` template's `wx:for` on `i.cn`.
 */
class KRWXPickerView : IKuiklyRenderViewExport {
    private val pickerElement = MiniWXPickerViewElement()

    override val ele: Element
        get() = pickerElement.unsafeCast<Element>()

    private var changeCallback: KuiklyRenderCallback? = null
    private var columnChangeCallback: KuiklyRenderCallback? = null
    private var cancelCallback: KuiklyRenderCallback? = null

    init {
        pickerElement.addEventListener(EVENT_CHANGE, createEventForwarder { changeCallback })
        pickerElement.addEventListener(EVENT_COLUMN_CHANGE, createEventForwarder { columnChangeCallback })
        pickerElement.addEventListener(EVENT_CANCEL, createEventForwarder { cancelCallback })
    }

    private fun createEventForwarder(callbackSupplier: () -> KuiklyRenderCallback?): EventHandler {
        return { event: dynamic ->
            val data: dynamic = JSON.stringify(event.detail)
            callbackSupplier()?.invoke(mapOf<String, Any>(KEY_DATA to data.unsafeCast<String>()))
        }
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            PROP_MODE -> { pickerElement.mode = propValue as String; true }
            PROP_DISABLED -> { pickerElement.disabled = toBoolean(propValue); true }
            PROP_NAME -> { pickerElement.name = propValue as String; true }
            PROP_RANGE -> { pickerElement.range = propValue as String; true }
            PROP_RANGE_KEY -> { pickerElement.rangeKey = propValue as String; true }
            PROP_VALUE -> { pickerElement.value = propValue as String; true }
            PROP_START -> { pickerElement.start = propValue as String; true }
            PROP_END -> { pickerElement.end = propValue as String; true }
            PROP_FIELDS -> { pickerElement.fields = propValue as String; true }
            PROP_HEADER_TEXT -> { pickerElement.headerText = propValue as String; true }
            PROP_FIXED -> { pickerElement.fixed = toBoolean(propValue); true }
            CALLBACK_CHANGE -> { changeCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true }
            CALLBACK_COLUMN_CHANGE -> { columnChangeCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true }
            CALLBACK_CANCEL -> { cancelCallback = propValue.unsafeCast<KuiklyRenderCallback>(); true }
            else -> super.setProp(propKey, propValue)
        }
    }

    private fun toBoolean(value: Any): Boolean = when (value) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> value == "1" || value.equals("true", ignoreCase = true)
        else -> false
    }

    companion object {
        const val VIEW_NAME = "KRWXPickerView"

        // Props
        const val PROP_MODE = "mode"
        const val PROP_DISABLED = "disabled"
        const val PROP_NAME = "name"
        const val PROP_RANGE = "range"
        const val PROP_RANGE_KEY = "rangeKey"
        const val PROP_VALUE = "value"
        const val PROP_START = "start"
        const val PROP_END = "end"
        const val PROP_FIELDS = "fields"
        const val PROP_HEADER_TEXT = "headerText"
        const val PROP_FIXED = "fixed"

        // Callbacks
        const val CALLBACK_CHANGE = "changeCallback"
        const val CALLBACK_COLUMN_CHANGE = "columnChangeCallback"
        const val CALLBACK_CANCEL = "cancelCallback"

        // Mini-program native event names
        private const val EVENT_CHANGE = "change"
        private const val EVENT_COLUMN_CHANGE = "columnchange"
        private const val EVENT_CANCEL = "cancel"

        private const val KEY_DATA = "data"
    }
}
