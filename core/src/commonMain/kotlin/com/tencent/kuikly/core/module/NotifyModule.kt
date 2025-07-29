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

package com.tencent.kuikly.core.module

import com.tencent.kuikly.core.collection.fastLinkedMapOf
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
/*
 * Native与Kuikly 或 Kuikly与Kuikly 之间的通信能力模块
 */
class NotifyModule : Module() {

    private val cbIdMap = fastLinkedMapOf<String, String>()
    private var cbIdProducer = 0

    override fun moduleName(): String {
        return MODULE_NAME
    }

    /*
     * @brief 添加通知监听
     * @param eventName 通知事件名
     * @param crossProcess 安卓跨进程监听能力（为true代表支持native侧其他进程发起通知，kuikly侧也能收到）
     * @return  返回值callbackRef可保存用作合适时机通过removeNotify删除该通知监听）
     */
    fun addNotify(eventName: String, crossProcess: Boolean = false, cb: CallbackFn): CallbackRef {
        cbIdProducer++
        val id = cbIdProducer.toString()
        val params = JSONObject()
            .put(KEY_ID, id)
            .put(KEY_EVENT_NAME, eventName)
            .put(KEY_CROSS_PROCESS, crossProcess)
            .toString()
        val cbRef = toNative(
            true,
            METHOD_ADD_NOTIFY,
            params,
            cb
        ).callbackRef!!
        cbIdMap[cbRef] = id
        return cbRef
    }

    /*
     * @brief 删除通知
     * @param eventName 事件名
     * @param callbackRef 需要删除的通知id，该参数为当时addNotify()返回
     */
    fun removeNotify(eventName: String, callbackRef: CallbackRef) {
        val id = cbIdMap.remove(callbackRef)
        val params = JSONObject()
            .put(KEY_ID, id ?: "")
            .put(KEY_EVENT_NAME, eventName)
            .toString()
        toNative(
            false,
            METHOD_REMOVE_NOTIFY,
            params
        )
        removeCallback(callbackRef)
    }

    /*
     * @brief 发送通知
     * @param eventName 事件名
     * @param eventData 事件数据
     * @param crossProcess 是否需要跨进程发送（安卓）
     */
    fun postNotify(eventName: String, eventData: JSONObject, crossProcess: Boolean = false) {
        val params = JSONObject()
            .put(KEY_EVENT_NAME, eventName)
            .put(KEY_DATA, eventData)
            .put(KEY_CROSS_PROCESS, crossProcess)
            .toString()
        toNative(
            true,
            METHOD_POST_NOTIFY,
            params
        )
    }

    companion object {
        const val MODULE_NAME = ModuleConst.NOTIFY
        const val METHOD_ADD_NOTIFY = "addNotify"
        const val METHOD_REMOVE_NOTIFY = "removeNotify"
        const val METHOD_POST_NOTIFY = "postNotify"
        private const val KEY_EVENT_NAME = "eventName"
        private const val KEY_ID = "id"
        private const val KEY_DATA = "data"
        private const val KEY_CROSS_PROCESS = "crossProcess" // 是否支持跨进程发送与接收，仅用于android
    }

}