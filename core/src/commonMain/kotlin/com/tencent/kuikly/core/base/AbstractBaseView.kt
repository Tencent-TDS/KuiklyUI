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

import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.collection.fastArrayListOf
import com.tencent.kuikly.core.layout.FlexNode
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.module.CallbackFn
import com.tencent.kuikly.core.module.IModuleAccessor
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/*
 * View公共基础Api供外部使用
 */
interface IViewPublicApi<A : Attr, E : Event> {
    /**
     * 父节点（参与dom排版）
     */
    val domParent: ViewContainer<*, *>?
    /**
     * 当前布局结果坐标（相对于父亲坐标系，即domParent）, 可通过监听layoutFrameDidChange事件获取该属性更新监听
     */
    val frame: Frame

    /**
     * 获得View对象用于调用View方法
     */
    fun <T : DeclarativeBaseView<*, *>> T.ref(ref: (viewRef: ViewRef<T>) -> Unit)

    /**
     * 设置View属性（响应式自动更新)
     */
    fun attr(init: A.() -> Unit)

    /**
     * 设置View事件
     */
    fun event(init: E.() -> Unit)

    /**
     * @return 获得ViewAttr对象，用于手动更新View属性
     */
    fun getViewAttr(): A

    /**
     * @return 获得ViewEvent对象
     */
    fun getViewEvent(): E

    /**
     * 转换当前坐标到目标节点坐标系
     * @param frame 当前view坐标系的坐标位置
     * @param toView 相对目标节点View坐标系( 如toView为null，则默认为当前Pager)
     * @return 转换到toView坐标系的坐标位置
     * 注：转换过程忽略transform设值
     */
    fun convertFrame(frame: Frame, toView: ViewContainer<*, *>?): Frame

    /**
     * 命令式做属性动画方法，实现以动画方式更新属性(属性差值动画)
     * 目的：最大化提升性能和自主可控性，实现命令式O(1)性能做属性动画
     * @param animation 动画参数 如 Animation.linear(0.3f)
     * @param completion 动画结束回调该闭包，其中Bool参数为是否动画执行成功
     * @param attrBlock 需要以动画方式更新的设置属性闭包，如 { transform(rotate = 45f) }
     */
    fun animateToAttr(animation: Animation, completion: ((Boolean)->Unit)? = null, attrBlock: Attr.() -> Unit)
}
/**
 * 抽象基类，只保留FlexNode,Event,Attr,renderView相关逻辑
 */
abstract class AbstractBaseView<A : Attr, E : Event> : BaseObject(), IViewPublicApi<A, E>, IModuleAccessor, IPagerId {
    val nativeRef: Int = ++nativeRefProducer
    var parentRef: Int = 0
    override var pagerId: String = ""
    protected var layoutFrame = Frame.zero
    override val frame: Frame
        get() = flexNode.layoutFrame
    protected val attr: A by lazy(LazyThreadSafetyMode.NONE) {
        internalCreateAttr()
    }
    protected val event: E by lazy(LazyThreadSafetyMode.NONE) {
        internalCreateEvent()
    }
    val flexNode = FlexNode()
    var renderView: RenderView? = null
    private val renderViewLazyTasks: MutableList<() -> Unit> by lazy(LazyThreadSafetyMode.NONE) {
        fastArrayListOf<() -> Unit>()
    }

    override fun getViewAttr(): A {
        return attr
    }

    override fun getViewEvent(): E {
        return event
    }

    override fun event(init: E.() -> Unit) {
        event.apply(init)
    }

    override fun convertFrame(frame: Frame, toView: ViewContainer<*, *>?): Frame {
        return Frame.zero
    }

    open fun willInit() {
    }

    open fun didInit() {
        if (getPager().debugUIInspector()) {
            injectDebugName()
        }
    }

    // 将类名注入Native端
    private fun injectDebugName() {
        attr.apply {
            val className = this@AbstractBaseView::class.simpleName
            debugName(className ?: "")
        }
    }

    abstract fun createAttr(): A
    abstract fun createEvent(): E

    abstract fun viewName(): String

    open fun onFireEvent(event: String, data: JSONObject?) {
        this.event.onFireEvent(event, data)
    }

    open fun createFlexNode() {
        flexNode.layoutFrameDidChangedCallback = {
            getPager().addTaskWhenPagerDidCalculateLayout {
                if (layoutFrame.isDefaultValue() || !(flexNode.layoutFrame.equals(layoutFrame))) {
                    layoutFrame = flexNode.layoutFrame
                    layoutFrameDidChanged(flexNode.layoutFrame)
                }
            }
        }
    }

    open fun removeFlexNode() {
        flexNode.parent?.also { parentNode ->
            val index = parentNode.indexOf(flexNode)
            if (index >= 0) {
                parentNode.removeChildAt(index)
            }
        }
    }

    open fun layoutFrameDidChanged(frame: Frame) {
    }

    private fun internalCreateEvent(): E {
        val event = createEvent()
        event.init(pagerId,nativeRef)
        return event
    }

    protected fun internalCreateAttr(): A {
        val attr = createAttr()
        attr.pagerId = pagerId
        attr.nativeRef = nativeRef
        attr.flexNode = flexNode
        return attr
    }

    open fun didSetProp(propKey: String, propValue: Any) {
        renderView?.setProp(propKey, propValue)
    }

    open fun syncProp(propKey: String, propValue: Any) {
        renderView?.setProp(propKey, propValue)
    }

    fun performTaskWhenRenderViewDidLoad(task: () -> Unit) {
        if (renderView != null) {
            task()
        } else {
            renderViewLazyTasks.add(task)
        }
    }

    fun performRenderViewLazyTasks() {
        if (renderViewLazyTasks.isNotEmpty()) {
            renderViewLazyTasks.forEach {
                it()
            }
            renderViewLazyTasks.clear()
        }
    }

    protected fun callRenderViewMethod(methodName: String, params: String? = null, callback: CallbackFn? = null) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod(methodName, params, callback)
        }
    }

    companion object {
        var nativeRefProducer = 0
    }
}