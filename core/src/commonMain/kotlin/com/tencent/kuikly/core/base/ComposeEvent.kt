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

package com.tencent.kuikly.core.base

import com.tencent.kuikly.core.base.event.ClickParams
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.base.event.EventHandlerFn
import com.tencent.kuikly.core.base.event.EventName
import com.tencent.kuikly.core.collection.fastHashMapOf

open class ComposeEvent : Event() {

    private val composeEventMap = fastHashMapOf<String, EventHandlerFn>()

    fun registerEvent(eventName: String, handlerFn: EventHandlerFn) {
        composeEventMap[eventName] = handlerFn
    }

    override fun onFireEvent(eventName: String, data: Any?):Boolean {
        if (composeEventMap[eventName] != null) {
            composeEventMap[eventName]?.invoke(data)
        } else {
            super.onFireEvent(eventName, data)
        }
        return true
    }

    override fun click(handler: (ClickParams) -> Unit) {
        handlerWithEventName(EventName.CLICK.value)?.also {
            composeEventMap[EventName.CLICK.value] = it
        }
        super.click(handler)
    }

     override fun onViewDidRemove() {
        super.onViewDidRemove()
        composeEventMap.clear()
    }

}