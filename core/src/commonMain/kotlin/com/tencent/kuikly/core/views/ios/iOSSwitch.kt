/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

/**
 * iOS native UISwitch component with glass effect styling.
 */
fun ViewContainer<*, *>.iOSSwitch(init: iOSSwitch.() -> Unit) {
    addChild(iOSSwitch(), init)
}

/**
 * iOS native switch component that supports glass effect styling.
 */
class iOSSwitch : DeclarativeBaseView<iOSSwitchAttr, Event>() {
    override fun createAttr(): iOSSwitchAttr = iOSSwitchAttr()
    override fun createEvent(): Event = Event()
    override fun viewName(): String = ViewConst.TYPE_IOS_SWITCH
}

/**
 * Attributes for iOS native switch.
 */
class iOSSwitchAttr : Attr() {
    /**
     * Sets the on/off state of the switch.
     * @param isOn true for on state, false for off state
     */
    fun value(isOn: Boolean): iOSSwitchAttr {
        "value" with isOn.toString()
        return this
    }

    /**
     * Enables/disables the switch.
     * @param enabled true to enable user interaction
     */
    fun enabled(enabled: Boolean): iOSSwitchAttr {
        "enabled" with enabled.toString()
        return this
    }

    /**
     * Enables/disables glass effect styling.
     * @param enabled true to enable glass effect
     */
    fun glassEffect(enabled: Boolean): iOSSwitchAttr {
        "glassEffect" with enabled
        return this
    }
}