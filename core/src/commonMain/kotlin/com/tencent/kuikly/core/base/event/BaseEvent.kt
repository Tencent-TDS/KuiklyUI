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

package com.tencent.kuikly.core.base.event

import com.tencent.kuikly.core.base.AbstractBaseView
import com.tencent.kuikly.core.base.BaseObject
import com.tencent.kuikly.core.base.IPagerId
import com.tencent.kuikly.core.base.RenderView
import com.tencent.kuikly.core.collection.fastLinkedMapOf

/**
 * 基础的EventCenter抽象类，实现了一些公共的方法。
 */
abstract class BaseEvent: BaseObject(), IEvent, IPagerId {

    //两个必须的参数，可参考init方法的说明
    override var pagerId: String = ""
    var viewId:Int = 0

    // 用来保持注册事件和处理函数的map，注册和取消注册都是操作的该类
    protected val eventMap = fastLinkedMapOf<String, EventHandlerFn>()

    override fun init(pagerId: String, viewId: Int) {
        this.pagerId = pagerId
        this.viewId = viewId
    }
    override fun register(eventName: String, eventHandlerFn: EventHandlerFn) {
        eventMap[eventName] = eventHandlerFn
    }

    override fun unRegister(eventName: String) {
        eventMap.remove(eventName)
    }

    override fun onFireEvent(eventName: String, data: Any?): Boolean {
        return eventMap[eventName]?.also {
            it.invoke(data)
        } != null
    }

    override fun isEmpty(): Boolean {
        return eventMap.isEmpty()
    }

    override fun onViewDidRemove() {
        // 需要清理掉整个map
        eventMap.clear()
    }

    override fun getView(): AbstractBaseView<*, *>? {
        return getPager().getViewWithNativeRef(viewId)
    }

    override fun getRenderView(): RenderView? {
        return getView()?.renderView
    }

}