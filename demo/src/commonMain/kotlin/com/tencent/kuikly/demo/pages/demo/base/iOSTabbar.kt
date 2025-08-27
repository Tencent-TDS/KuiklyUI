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

package com.tencent.kuikly.demo.pages.demo.base

import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * iOS native UITabBar component with glass effect styling.
 */
fun ViewContainer<*, *>.TabbarIOS(init: TabbarIOSView.() -> Unit) {
    addChild(TabbarIOSView(), init)
}

/**
 * iOS native tab bar component that supports glass effect styling.
 */
class TabbarIOSView : DeclarativeBaseView<TabbarIOSAttr, TabbarIOSEvent>() {
    override fun createAttr() = TabbarIOSAttr()
    override fun createEvent() = TabbarIOSEvent()
    override fun viewName() = ViewConst.TYPE_IOS_TABBAR

    /**
     * Sets badge for tab item.
     * @param index The tab index
     * @param badge The badge text (empty string to remove)
     */
    fun setBadge(index: Int, badge: String) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("setBadge", JSONObject().apply {
                put("index", index)
                put("badge", badge)
            }.toString())
        }
    }
}

/**
 * Attributes for iOS native tab bar.
 */
class TabbarIOSAttr : Attr() {
    /**
     * Sets the tab items.
     * @param items List of tab items
     */
    fun items(items: List<TabbarIOSItem>): TabbarIOSAttr {
        "items" with items.map { it.toMap() }
        return this
    }

    /**
     * Sets the currently selected tab index.
     * @param index The selected tab index
     */
    fun selectedIndex(index: Int): TabbarIOSAttr {
        "selectedIndex" with index
        return this
    }
}

/**
 * Events for iOS native tab bar.
 */
class TabbarIOSEvent : Event() {
    /**
     * Tab selection change event.
     * @param handler Callback with selected tab index
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

/**
 * Represents a tab bar item.
 */
data class TabbarIOSItem(
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

/**
 * Parameters for tab selection events.
 */
data class TabSelectedParams(val index: Int) {
    companion object {
        fun decode(params: Any?): TabSelectedParams {
            val temp = params as? JSONObject ?: JSONObject()
            return TabSelectedParams(temp.optInt("index", 0))
        }
    }
}