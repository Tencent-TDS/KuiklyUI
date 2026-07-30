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
import com.tencent.kuikly.core.base.Size
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * iOS native UISlider component with glass effect styling.
 */
fun ViewContainer<*, *>.iOSSlider(init: iOSSlider.() -> Unit) {
    addChild(iOSSlider(), init)
}

/**
 * iOS native slider component that supports glass effect styling.
 */
class iOSSlider : DeclarativeBaseView<iOSSliderAttr, iOSSliderEvent>() {
    override fun createAttr(): iOSSliderAttr = iOSSliderAttr()
    override fun createEvent(): iOSSliderEvent = iOSSliderEvent()
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
    fun currentProgress(value: Float): iOSSliderAttr {
        require(value in 0f..1f) { "Slider value must be between 0 and 1" }
        KEY_VALUE with value
        return this
    }

    /**
     * Sets the minimum value of the slider.
     * @param minValue The minimum value (default: 0.0)
     */
    fun minValue(minValue: Float): iOSSliderAttr {
        KEY_MIN_VALUE with minValue
        return this
    }

    /**
     * Sets the maximum value of the slider.
     * @param maxValue The maximum value (default: 1.0)
     */
    fun maxValue(maxValue: Float): iOSSliderAttr {
        KEY_MAX_VALUE with maxValue
        return this
    }

    /**
     * Sets the thumb (slider handle) color.
     * @param color The color for the slider thumb
     */
    fun thumbColor(color: Color): iOSSliderAttr {
        KEY_THUMB_COLOR with color.toString()
        return this
    }

    /**
     * Sets the track (background) color.
     * @param color The color for the slider track
     */
    fun trackColor(color: Color): iOSSliderAttr {
        KEY_TRACK_COLOR with color.toString()
        return this
    }

    /**
     * Sets the progress (filled portion) color.
     * @param color The color for the slider progress
     */
    fun progressColor(color: Color): iOSSliderAttr {
        KEY_PROGRESS_COLOR with color.toString()
        return this
    }

    /**
     * Sets whether the slider sends value change events continuously during tracking.
     * @param continuous true for continuous updates, false for discrete updates
     */
    fun continuous(continuous: Boolean): iOSSliderAttr {
        KEY_CONTINUOUS with continuous
        return this
    }

    /**
     * Sets the track thickness.
     * @param thickness The thickness of the slider track
     */
    fun trackThickness(thickness: Float): iOSSliderAttr {
        KEY_TRACK_THICKNESS with thickness
        return this
    }

    /**
     * Sets the thumb size.
     * @param width The width of the slider thumb
     * @param height The height of the slider thumb
     */
    fun thumbSize(size: Size): iOSSliderAttr {
        KEY_THUMB_SIZE with mapOf(KEY_WIDTH to size.width, KEY_HEIGHT to size.height)
        return this
    }

    /**
     * Sets the slider direction.
     * @param isHorizontal true for horizontal, false for vertical
     */
    fun sliderDirection(isHorizontal: Boolean): iOSSliderAttr {
        KEY_DIRECTION_HORIZONTAL with isHorizontal
        return this
    }

    companion object {
        const val KEY_VALUE = "value"
        const val KEY_MIN_VALUE = "minValue"
        const val KEY_MAX_VALUE = "maxValue"
        const val KEY_THUMB_COLOR = "thumbColor"
        const val KEY_TRACK_COLOR = "trackColor"
        const val KEY_PROGRESS_COLOR = "progressColor"
        const val KEY_CONTINUOUS = "continuous"
        const val KEY_TRACK_THICKNESS = "trackThickness"
        const val KEY_THUMB_SIZE = "thumbSize"
        const val KEY_DIRECTION_HORIZONTAL = "directionHorizontal"
        const val KEY_WIDTH = "width"
        const val KEY_HEIGHT = "height"
    }
}

/**
 * Event handler for iOS slider.
 */
class iOSSliderEvent : Event() {
    /**
     * Called when the slider value changes.
     * @param handler The callback function that receives the new value
     */
    fun onValueChanged(handler: (SliderValueChangedParams) -> Unit) {
        register(ON_VALUE_CHANGED) {
            handler(SliderValueChangedParams.decode(it))
        }
    }

    /**
     * Called when the user starts touching the slider.
     * @param handler The callback function that receives the touch down event
     */
    fun onTouchDown(handler: (SliderTouchParams) -> Unit) {
        register(ON_TOUCH_DOWN) {
            handler(SliderTouchParams.decode(it))
        }
    }

    /**
     * Called when the user stops touching the slider.
     * @param handler The callback function that receives the touch up event
     */
    fun onTouchUp(handler: (SliderTouchParams) -> Unit) {
        register(ON_TOUCH_UP) {
            handler(SliderTouchParams.decode(it))
        }
    }

    companion object {
        const val ON_VALUE_CHANGED = "onValueChanged"
        const val ON_TOUCH_DOWN = "onTouchDown"
        const val ON_TOUCH_UP = "onTouchUp"
    }
}

/**
 * Parameters for slider value change event.
 */
data class SliderValueChangedParams(val value: Float) {
    companion object {
        fun decode(params: Any?): SliderValueChangedParams {
            val temp = params as JSONObject
            return SliderValueChangedParams(temp.optDouble("value", 0.0).toFloat())
        }
    }
}

/**
 * Parameters for slider touch events (touch down/up).
 */
data class SliderTouchParams(val value: Float,
                             val x: Float = 0f,
                             val y: Float = 0f,
                             val pageX: Float = 0f,
                             val pageY: Float = 0f) {
    companion object {
        fun decode(params: Any?): SliderTouchParams {
            val temp = params as JSONObject
            return SliderTouchParams(
                value = temp.optDouble("value", 0.0).toFloat(),
                x = temp.optDouble("x", 0.0).toFloat(),
                y = temp.optDouble("y", 0.0).toFloat(),
                pageX = temp.optDouble("pageX", 0.0).toFloat(),
                pageY = temp.optDouble("pageY", 0.0).toFloat()
            )
        }
    }
}