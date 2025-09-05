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

package com.tencent.kuikly.core.views.ios

import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * iOS native UISegmentedControl component with glass effect styling.
 */
fun ViewContainer<*, *>.SegmentedControlIOS(init: iOSSegmentedControlView.() -> Unit) {
    addChild(iOSSegmentedControlView(), init)
}

/**
 * iOS native segmented control component that supports glass effect styling.
 */
open class iOSSegmentedControlView : DeclarativeBaseView<iOSSegmentedControlAttr, iOSSegmentedControlEvent>() {
    override fun createAttr() = iOSSegmentedControlAttr()
    override fun createEvent() = iOSSegmentedControlEvent()
    override fun viewName() = ViewConst.TYPE_IOS_SEGMENTED_CONTROL
}

/**
 * Attributes for iOS native segmented control.
 */
open class iOSSegmentedControlAttr : Attr() {
    /**
     * Sets the segment titles.
     * @param titles List of segment titles
     */
    fun titles(titles: List<String>): iOSSegmentedControlAttr {
        "titles" with titles
        return this
    }

    /**
     * Sets the currently selected segment index.
     * @param index The selected segment index
     */
    fun selectedIndex(index: Int): iOSSegmentedControlAttr {
        "selectedIndex" with index
        return this
    }
}

/**
 * Events for iOS native segmented control.
 */
open class iOSSegmentedControlEvent : Event() {
    /**
     * Segment selection change event.
     * @param handler Callback with selected segment index
     */
    fun onValueChanged(handler: (ValueChangedParams) -> Unit) {
        register(ON_VALUE_CHANGED) {
            handler(ValueChangedParams.decode(it))
        }
    }

    companion object {
        const val ON_VALUE_CHANGED = "onValueChanged"
    }
}

/**
 * Parameters for value change events.
 */
data class ValueChangedParams(val index: Int) {
    companion object {
        fun decode(params: Any?): ValueChangedParams {
            val temp = params as? JSONObject ?: JSONObject()
            return ValueChangedParams(temp.optInt("index", 0))
        }
    }
}