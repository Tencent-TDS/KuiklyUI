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
 * iOS native UISlider component with glass effect styling.
 */
fun ViewContainer<*, *>.iOSSlider(init: iOSSlider.() -> Unit) {
    addChild(iOSSlider(), init)
}

/**
 * iOS native slider component that supports glass effect styling.
 */
class iOSSlider : DeclarativeBaseView<iOSSliderAttr, Event>() {
    override fun createAttr(): iOSSliderAttr = iOSSliderAttr()
    override fun createEvent(): Event = Event()
    override fun viewName(): String = ViewConst.TYPE_IOS_SLIDER
}

/**
 * Attributes for iOS native slider.
 */
class iOSSliderAttr : Attr() {
    /**
     * Sets the current value of the slider (0.0 - 1.0).
     * @param value The slider value between 0 and 1
     */
    fun value(value: Float): iOSSliderAttr {
        require(value in 0f..1f) { "Slider value must be between 0 and 1" }
        "value" with value
        return this
    }

    /**
     * Sets the minimum value of the slider.
     * @param minValue The minimum value (default: 0.0)
     */
    fun minValue(minValue: Float): iOSSliderAttr {
        "minValue" with minValue
        return this
    }

    /**
     * Sets the maximum value of the slider.
     * @param maxValue The maximum value (default: 1.0)
     */
    fun maxValue(maxValue: Float): iOSSliderAttr {
        "maxValue" with maxValue
        return this
    }

    /**
     * Enables/disables glass effect styling.
     * @param enabled true to enable glass effect
     */
    fun glassEffect(enabled: Boolean): iOSSliderAttr {
        "glassEffect" with enabled
        return this
    }
}