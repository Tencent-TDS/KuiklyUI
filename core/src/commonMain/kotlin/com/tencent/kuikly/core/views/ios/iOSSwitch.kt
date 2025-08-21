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
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * iOS native UISwitch component with glass effect styling.
 */
fun ViewContainer<*, *>.iOSSwitch(init: iOSSwitch.() -> Unit) {
    addChild(iOSSwitch(), init)
}

/**
 * iOS native switch component that supports glass effect styling.
 */
class iOSSwitch : DeclarativeBaseView<iOSSwitchAttr, iOSSwitchEvent>() {
    override fun createAttr(): iOSSwitchAttr = iOSSwitchAttr()
    override fun createEvent(): iOSSwitchEvent = iOSSwitchEvent()
    override fun viewName(): String = ViewConst.TYPE_IOS_SWITCH
}

/**
 * Attributes for iOS native switch.
 */
class iOSSwitchAttr : Attr() {

    /**
     * Enables/disables the switch.
     * @param enabled true to enable user interaction
     */
    fun enabled(enabled: Boolean): iOSSwitchAttr {
        "enabled" with enabled.toString()
        return this
    }

    /**
     * Sets the on/off state of the switch.
     * @param isOn true for on state, false for off state
     */
    fun isOn(isOn: Boolean): iOSSwitchAttr {
        "isOn" with isOn.toString()
        return this
    }

    /**
     * Sets the tint color for the switch when it's in the on state.
     * @param color Color
     */
    fun onColor(color: Color): iOSSwitchAttr {
        "onColor" with color.toString()
        return this
    }

    /**
     * Sets the color for the switch when it's in the off state.
     * @param color Color
     */
    fun unOnColor(color: Color): iOSSwitchAttr {
        "unOnColor" with color.toString()
        return this
    }

    /**
     * Sets the color for the thumb (the circular part that slides).
     * @param color Color
     */
    fun thumbColor(color: Color): iOSSwitchAttr {
        "thumbColor" with color.toString()
        return this
    }
}

/**
 * Event handler for iOS switch.
 */
class iOSSwitchEvent : Event() {
    /**
     * Called when the switch value changes.
     * @param handler The callback function that receives the new value
     */
    fun switchOnChanged(handler: (SwitchValueChangedParams) -> Unit) {
        register("onValueChanged") {
            handler(SwitchValueChangedParams.decode(it))
        }
    }
}

/**
 * Parameters for switch value change event.
 */
data class SwitchValueChangedParams(val value: Boolean) {
    companion object {
        fun decode(params: Any?): SwitchValueChangedParams {
            val temp = params as JSONObject
            return SwitchValueChangedParams(temp.optBoolean("value", false))
        }
    }
}