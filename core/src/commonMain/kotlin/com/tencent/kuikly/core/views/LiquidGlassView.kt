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

package com.tencent.kuikly.core.views

import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/*
 * iOS 26 Liquid Glass（液态玻璃）类
 */

fun ViewContainer<*, *>.LiquidGlass(init: LiquidGlassView.() -> Unit) {
    addChild(LiquidGlassView(), init)
}

fun ViewContainer<*, *>.GlassEffectContainer(init: GlassEffectContainerView.() -> Unit) {
    addChild(GlassEffectContainerView(), init)
}

class LiquidGlassView: ViewContainer<LiquidGlassViewAttr, LiquidGlassViewEvent>() {
    
    override fun createEvent(): LiquidGlassViewEvent {
        return LiquidGlassViewEvent()
    }

    override fun createAttr(): LiquidGlassViewAttr {
        return LiquidGlassViewAttr()
    }

    override fun viewName(): String {
        return ViewConst.TYPE_LIQUID_GLASS_VIEW
    }
}

enum class InterfaceStyle(val value: String) {
    AUTO("auto"),
    LIGHT("light"),
    DARK("dark")
}

class LiquidGlassViewAttr : ComposeAttr() {
    fun interactive(isInteractive: Boolean) {
        "interactive" with isInteractive
    }

    fun tintColor(color: Color) {
        "tintColor" with color.toString()
    }

    fun interfaceStyle(style: InterfaceStyle) {
        "interfaceStyle" with style.value
    }
}

class LiquidGlassViewEvent : ComposeEvent() {
    
}


// MARK: GlassEffectContainerView

class GlassEffectContainerView: ViewContainer<GlassEffectContainerAttr, ComposeEvent>() {
    override fun createAttr(): GlassEffectContainerAttr {
        return GlassEffectContainerAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }

    override fun viewName(): String {
        return "KRGlassContainerView"
    }
}

class GlassEffectContainerAttr : ComposeAttr() {
    fun spacing(spacing: Float) {
        "spacing" with spacing
    }

    fun interfaceStyle(style: InterfaceStyle) {
        "interfaceStyle" with style.value
    }
}


// MARK: iOS's UISwitch

fun ViewContainer<*, *>.iOSSwitch(init: iOSSwitch.() -> Unit) {
    addChild(iOSSwitch(), init)
}

class iOSSwitch : DeclarativeBaseView<iOSSwitchAttr, Event>() {
    override fun createAttr(): iOSSwitchAttr {
        return iOSSwitchAttr()
    }

    override fun createEvent(): Event {
        return Event()
    }

    override fun viewName(): String {
        return "KRiOSGlassSwitch"
    }

}

class iOSSwitchAttr : Attr() {

    /**
     * 设置value
     * @param isOn 是否开启
     * @return this
     */
    fun value(isOn: Boolean): iOSSwitchAttr {
        "value" with isOn.toString()
        return this
    }
}

// MARK: iOS's UISlider

fun ViewContainer<*, *>.iOSSlider(init: iOSSlider.() -> Unit) {
    addChild(iOSSlider(), init)
}

class iOSSlider : DeclarativeBaseView<iOSSliderAttr, Event>() {
    override fun createAttr(): iOSSliderAttr {
        return iOSSliderAttr()
    }

    override fun createEvent(): Event {
        return Event()
    }

    override fun viewName(): String {
        return "KRiOSGlassSlider"
    }

}

class iOSSliderAttr : Attr() {

    fun value(process: Float): iOSSliderAttr {
        "value" with process
        return this
    }
}


// MARK: iOS's UITabbar

class MyTabbarAttr : Attr() {
    /**
     * 设置 tabbar 的 items
     * @param items tabbar item 列表
     */
    fun items(items: List<TabbarItem>): MyTabbarAttr {
        "items" with items.map { it.toMap() }
        return this
    }

    /**
     * 当前选中的 index
     */
    fun selectedIndex(index: Int): MyTabbarAttr {
        "selectedIndex" with index
        return this
    }
}

data class TabbarItem(
    val title: String,
    val icon: String,
    val selectedIcon: String
) {
    fun toMap(): Map<String, Any> = mapOf(
        "title" to title,
        "icon" to icon,
        "selectedIcon" to selectedIcon
    )
}

class MyTabbarEvent : Event() {
    /**
     * tab 切换事件
     */
    fun onTabSelected(handler: (TabSelectedParams) -> Unit) {
        register(ON_TAB_SELECTED) {
            handler(TabSelectedParams.decode(it))
        }
    }
    companion object {
        const val ON_TAB_SELECTED = "onTabSelected"
    }
}

data class TabSelectedParams(val index: Int) {
    companion object {
        fun decode(params: Any?): TabSelectedParams {
            val temp = params as? JSONObject ?: JSONObject()
            return TabSelectedParams(temp.optInt("index", 0))
        }
    }
}

class TabbarIOSView : DeclarativeBaseView<MyTabbarAttr, MyTabbarEvent>() {
    override fun createAttr() = MyTabbarAttr()
    override fun createEvent() = MyTabbarEvent()
    override fun viewName() = "KRTabbarView"

    // 可选：暴露方法
    fun setBadge(index: Int, badge: String) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("setBadge", JSONObject().apply {
                put("index", index)
                put("badge", badge)
            }.toString())
        }
    }
}

fun ViewContainer<*, *>.TabbarIOS(init: TabbarIOSView.() -> Unit) {
    addChild(TabbarIOSView(), init)
}

// MARK: iOS's UISegmentController

class MySegmentedAttr : Attr() {
    /**
     * 设置分段标题
     */
    fun titles(titles: List<String>): MySegmentedAttr {
        "titles" with titles
        return this
    }

    /**
     * 当前选中 index
     */
    fun selectedIndex(index: Int): MySegmentedAttr {
        "selectedIndex" with index
        return this
    }
}

class MySegmentedEvent : Event() {
    /**
     * 选中变化事件
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

data class ValueChangedParams(val index: Int) {
    companion object {
        fun decode(params: Any?): ValueChangedParams {
            val temp = params as? JSONObject ?: JSONObject()
            return ValueChangedParams(temp.optInt("index", 0))
        }
    }
}

class MySegmentedView : DeclarativeBaseView<MySegmentedAttr, MySegmentedEvent>() {
    override fun createAttr() = MySegmentedAttr()
    override fun createEvent() = MySegmentedEvent()
    override fun viewName() = "HRSegmentedControl"

    // 可选：暴露方法
    fun setBadge(index: Int, badge: String) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("setBadge", JSONObject().apply {
                put("index", index)
                put("badge", badge)
            }.toString())
        }
    }
}

fun ViewContainer<*, *>.MySegmented(init: MySegmentedView.() -> Unit) {
    addChild(MySegmentedView(), init)
}
