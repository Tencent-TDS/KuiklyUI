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

package com.tencent.kuikly.core.views

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.views.internal.GroupAttr
import com.tencent.kuikly.core.views.internal.GroupEvent
import com.tencent.kuikly.core.views.internal.GroupView
import com.tencent.kuikly.core.views.internal.TouchEventHandlerFn

open class DivView : GroupView<DivAttr, DivEvent>() {
    
    override fun createAttr(): DivAttr {
        return DivAttr()
    }

    override fun createEvent(): DivEvent {
        return DivEvent()
    }

    override fun viewName(): String {
        return ViewConst.TYPE_VIEW
    }

    override fun isRenderView(): Boolean {
        return isRenderViewForFlatLayer()
    }
    /**
     * 创建文本选区
     * @param x 选区起始点x坐标（相对于当前容器左上角）
     * @param y 选区起始点y坐标（相对于当前容器左上角）
     * @param type 选区类型（字符、单词、段落）
     */
    fun createSelection(x: Float, y: Float, type: SelectionType) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("createSelection", JSONObject().apply {
                put("x", x)
                put("y", y)
                put("type", type.value)
            }.toString())
        }
    }

    /**
     * 获取当前文本选区内容
     * @param callback 选区内容回调，List<String>表示选区内的文本内容数组
     */
    fun getSelection(callback: (List<String>) -> Unit) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("getSelection", null) { data ->
                val result = mutableListOf<String>()
                data?.optJSONArray("content")?.also {
                    for (i in 0 until it.length()) {
                        val text = it.optString(i, "")!!
                        result.add(text)
                    }
                }
                callback(result)
            }
        }
    }

    /**
     * 清除当前文本选区
     */
    fun clearSelection() {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("clearSelection")
        }
    }

    /**
     * 选中所有文本
     */
    fun createSelectionAll() {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("createSelectionAll")
        }
    }

}

enum class SelectionType(val value: Int) {
    CHARACTER(0),
    WORD(1),
    PARAGRAPH(2),
    SENTENCE(3),
}

open class DivAttr : GroupAttr() {


    /**
     * 设置子节点文本是否可选中
     * @param option 一个SelectableOption枚举值，表示容器的可选中状态，默认值为INHERIT
     */
    fun selectable(option: SelectableOption) {
        "selectable" with option.value
    }

    /**
     * 设置文本选区的颜色
     * @param color 一个Color对象，表示文本选中时的颜色
     */
    fun selectionColor(color: Color) {
        "selectionColor" with JSONObject().apply {
            put("background", 0x66000000L or (color.hexColor and 0x00FFFFFF))
            put("cursor", 0xFF000000L or (color.hexColor and 0x00FFFFFF))
        }.toString()
    }

}
enum class SelectableOption(val value: Int) {
    INHERIT(0),
    ENABLE(1),
    DISABLE(2)
}

open class DivEvent : GroupEvent() {
    companion object {
        private fun Any?.decodeToFrame(): Frame {
            val json = this as? JSONObject ?: return Frame.zero
            val x = json.optDouble("x").toFloat()
            val y = json.optDouble("y").toFloat()
            val width = json.optDouble("width").toFloat()
            val height = json.optDouble("height").toFloat()
            return Frame(x, y, width, height)
        }
    }



    /**
     * 激活文本选择模式事件回调。
     * @param handlerFn 事件回调，Frame表示选区的位置信息。
     */
    fun selectStart(handlerFn: (Frame) -> Unit) {
        register("selectStart") {
            handlerFn(it.decodeToFrame())
        }
    }

    /**
     * 调整文本选区位置或大小事件回调。
     * @param handlerFn 事件回调，Frame表示选区的位置信息。
     */
    fun selectChange(handlerFn: (Frame) -> Unit) {
        register("selectChange") {
            handlerFn(it.decodeToFrame())
        }
    }

    /**
     * 调整文本选区结束事件回调。
     * @param handlerFn 事件回调，Frame表示选区的位置信息。
     */
    fun selectEnd(handlerFn: (Frame) -> Unit) {
        register("selectEnd") {
            handlerFn(it.decodeToFrame())
        }
    }

    /**
     * 退出文本选择模式事件回调。
     * @param handlerFn 事件回调。
     */
    fun selectCancel(handlerFn: () -> Unit) {
        register("selectCancel") {
            handlerFn()
        }
    }
}
/**
 * 创建一个类似于 ViewGroup/UIView/Div 的视图容器。
 * @param init 一个 DivView.() -> Unit 函数，用于初始化视图容器的属性和子视图。
 */
fun ViewContainer<*, *>.View(init: DivView.() -> Unit) {
    val viewGroup = createViewFromRegister(ViewConst.TYPE_VIEW_CLASS_NAME) as? DivView
    if (viewGroup != null) { // 存在自定义扩展
        addChild(viewGroup, init)
    } else {
        addChild(DivView(), init)
    }
}
