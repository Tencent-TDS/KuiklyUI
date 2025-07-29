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

package com.tencent.kuikly.demo.pages.view

import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.base.event.didAppear
import com.tencent.kuikly.core.base.event.didDisappear
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.IPagerEventObserver
import com.tencent.kuikly.demo.pages.base.BasePager

/*
 * @brief 页面组件，用于页面@Page("xxxxx")拆分实现，通过这个组件来组合拼凑出原来的页面，实现一个复杂页面分包目标
 */
internal fun ViewContainer<*, *>.KuiklyPage(init: KuiklyPageView.() -> Unit) {
    addChild(KuiklyPageView(), init)
}

internal class KuiklyPageAttr : Attr() {
    /*
     * 页面名，@Page("xxxxx") 中的xxxx
     */
    fun pageName(name: String) {
        "pageName" with name
    }
    /*
    * 页面名，@Page("xxxxx") 中的xxxx
    */
    fun pageData(data: JSONObject) {
        "pageData" with data.toString()
    }

}

internal class KuiklyPageEvent : Event() {
    /*
     * 页面加载成功回调
     */
    fun loadSuccess(eventHandlerFn: () -> Unit) {
        register(EVENT_LOAD_SUCCESS) {
            eventHandlerFn()
        }
    }

    /*
     * 首屏加载阶段的失败回调（页面加载失败）
     */
    fun loadFailure(eventHandlerFn: () -> Unit) {
        register(EVENT_LOAD_FAILURE) {
            eventHandlerFn()
        }
    }
    companion object {
        private const val EVENT_LOAD_SUCCESS = "loadSuccess"
        private const val EVENT_LOAD_FAILURE = "loadFailure"
    }
}

internal class KuiklyPageView : DeclarativeBaseView<KuiklyPageAttr, KuiklyPageEvent>(), IPagerEventObserver {
    override fun createAttr() = KuiklyPageAttr()
    override fun createEvent() = KuiklyPageEvent()
    override fun viewName() = "KuiklyPageView"

    override fun willInit() {
        super.willInit()
        event {
            didAppear {
                this@KuiklyPageView.sendPagerEvent("didAppear")
            }
            didDisappear {
                this@KuiklyPageView.sendPagerEvent("didDisappear")
            }
        }
    }

    override fun didMoveToParentView() {
        super.didMoveToParentView()
        getPager().addPagerEventObserver(this)
        if ((getPager() as? BasePager)?.pagerAppear == true) {
            sendPagerEvent("viewDidAppear")
        }
    }

    override fun didRemoveFromParentView() {
        super.didRemoveFromParentView()
        getPager().removePagerEventObserver(this)
    }

    // public method
    // 发送事件到该Pager侧，Pager侧通过onReceivePagerEvent/addPagerEventObserver方法接收
    fun sendPagerEvent(event: String, data: JSONObject? = null) {
        val params = JSONObject().apply {
            put("event", event)
            put("data", (data ?: JSONObject()))
        }
        callRenderViewMethod("sendEvent", params.toString())
    }

    fun pageDidAppear() {
        sendPagerEvent("viewDidAppear")
    }

    fun pageDidDisappear() {
        sendPagerEvent("viewDidDisappear")
    }

    override fun onPagerEvent(pagerEvent: String, eventData: JSONObject) {
        sendPagerEvent(pagerEvent, eventData)
    }

}
