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

import com.tencent.kuikly.core.global.GlobalFunctions
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.module.CallbackFn
import com.tencent.kuikly.core.module.CallbackRef
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

class RenderView(private val pagerId: String, private val viewRef: Int, private val viewName: String) {

    var currentFrame: Frame = Frame.zero

    var didLayout = false
       private set
    init {
        BridgeManager.createRenderView(pagerId, viewRef, viewName)
    }

    fun setProp(key: String, value: Any) {
        BridgeManager.setViewProp(pagerId, viewRef, key, value, 0)
    }

    fun setFrame(x: Float, y: Float, width: Float, height: Float) {
        currentFrame = Frame(x, y, width, height)
        BridgeManager.setRenderViewFrame(pagerId, viewRef, x, y, width, height)
        didLayout = true
    }

    fun setEvent(eventName: String, sync: Int = 0) {
        BridgeManager.setViewProp(pagerId, viewRef, eventName, 1, 1, sync)
    }

    fun setShadow() {
        BridgeManager.setShadowForView(pagerId, viewRef)
    }

    fun callMethod(methodName: String, params: String? = null, callback: CallbackFn? = null) {
        var callbackRef: CallbackRef? = null
        callback?.also { cb ->
            callbackRef = GlobalFunctions.createFunction(pagerId) { data ->
                var res : JSONObject? = null
                if (data != null && data is String) {
                    res = JSONObject(data)
                }
                cb(res)
                false
            }
        }
        BridgeManager.callViewMethod(pagerId, viewRef, methodName, params, callbackRef)
    }

    fun insertSubRenderView(subViewRef: Int, index: Int) {
        BridgeManager.insertSubRenderView(pagerId, viewRef, subViewRef, index)
    }

    fun insertToRootView() {
        BridgeManager.insertSubRenderView(pagerId, ROOT_VIEW_TAG, viewRef, 0)
    }

    fun removeFromParentRenderView() {
        BridgeManager.removeRenderView(pagerId, viewRef)
    }

    companion object {
        const val ROOT_VIEW_TAG = -1
    }
}
