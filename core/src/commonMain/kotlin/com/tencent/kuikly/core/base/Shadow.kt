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

import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.manager.BridgeManager

class Size(val width: Float, val height: Float) {

    override fun toString(): String {
        return "width:${width}, height:${height}"
    }
}

open class Shadow(private val pagerId: String, private val viewRef: Int, viewName: String) {

    init {
        BridgeManager.createShadow(pagerId, viewRef, viewName)
    }

    open fun setProp(key: String, value: Any) {
        BridgeManager.setShadowProp(pagerId, viewRef, key, value)
    }

    open fun calculateRenderViewSize(width: Float, height: Float): Size {
        val sizeStr = BridgeManager.calculateRenderViewSize(pagerId, viewRef, width, height)
        if (sizeStr.isEmpty()) {
            KLog.e("Shadow", "calculateRenderViewSize sizeStr is empty")
            return Size(0f, 0f)
        }
        val parts = sizeStr.split("|")
        return Size(parts[0].toFloat(), parts[1].toFloat())
    }

    open fun removeFromParentComponent() {
        BridgeManager.removeShadow(pagerId, viewRef)
    }

    fun callMethod(methodName: String, params: String): String {
        val result = BridgeManager.callShadowMethod(pagerId, viewRef, methodName, params)
        return if (result is String) result as String else result.toString()
    }
}
