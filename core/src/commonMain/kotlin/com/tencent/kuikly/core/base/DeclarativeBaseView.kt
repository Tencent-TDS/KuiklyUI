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
import com.tencent.kuikly.core.collection.fastHashMapOf
import com.tencent.kuikly.core.exception.throwRuntimeError
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.layout.MutableFrame
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.module.CallbackFn
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.IPager
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.ReactiveObserver
import kotlin.math.max

abstract class DeclarativeBaseView<A : Attr, E : Event> : AbstractBaseView<A, E>() {
    enum class ImageType(val value: String) {
        // 返回缓存Key，可用于Image的src
        CACHE_KEY("cacheKey"),
        // 返回base64字符串
        DATA_URI("dataUri"),
        // 返回文件path
        FILE("file"),
    }

    /* DSL父节点（即模板父节点） */
    val parent: ViewContainer<*, *>?
        get() = getPager().getViewWithNativeRef(parentRef) as? ViewContainer<*, *>
    /* 真实父节点（一般使用该字段进行坐标换算） */
    override val domParent : ViewContainer<*, *>?
        get() {
            var domParent = parent
            while (domParent?.isVirtualView() == true) {
              domParent = domParent.parent
            }
            return domParent
       }

    internal var absoluteFlexNode : Boolean = false

    // 渲染属性对象,延迟初始化
    public var renderProperties: Any? = null

    override fun <T : DeclarativeBaseView<*, *>> T.ref(ref: (viewRef: ViewRef<T>) -> Unit) {
        ref(ViewRef<T>(pagerId, nativeRef))
    }

    override fun attr(init: A.() -> Unit) {
        if (pagerId.isEmpty()) {
            return
        }
        val observable = ReactiveObserver.bindValueChange(this) { isFirst ->
            attr.apply {
                if (isFirst) {
                    apply(init)
                } else {
                    beginApplyAttrProperty()
                    apply(init)
                    endApplyAttrProperty()
                }
            }
        }
        attr.isStaticAttr = !observable
    }

    open fun willMoveToParentComponent() {
        if (pagerId.isEmpty()) {
            throwRuntimeError("pager id is empty")
        }
    }

    open fun didMoveToParentView() {
        getPager().putNativeViewRef(nativeRef, this)
    }

    open fun willRemoveFromParentView() {
    }

    open fun didRemoveFromParentView() {
        getPager().removeNativeViewRef(nativeRef)
        ReactiveObserver.removeObserver(this)
        attr.viewDidRemove()
        event.onViewDidRemove()
        flexNode.layoutFrameDidChangedCallback = null
        renderView = null
    }

    open fun isPager(): Boolean {
        return this is IPager
    }

    override fun <T : Module> getModule(name: String): T? {
        return PagerManager.getPager(pagerId).getModule(name)
    }

    override fun <T : Module> acquireModule(name: String): T {
        return PagerManager.getPager(pagerId).acquireModule(name)
    }

    protected fun createComponentRenderViewIfNeed() {
        if (renderView !== null) {
            return;
        }
        renderView = RenderView(pagerId, nativeRef, viewName())
        attr.also {
            it.setPropsToRenderView()
        }
        event.onRenderViewDidCreated()
        flexNode.also {
            if (!it.layoutFrame.isDefaultValue()) {
                setFrameToRenderView(it.layoutFrame)
            }
        }
        performRenderViewLazyTasks()
    }

    open fun createRenderView() {
        createComponentRenderViewIfNeed()
    }

    open fun renderViewDidMoveToParentRenderView() {

    }

    open fun removeRenderView() {
        renderView?.also {
            it.removeFromParentRenderView()
        }
        renderView = null
        event.onRenderViewDidRemoved()
    }

    open fun frameInParentRenderComponentCoordinate(frame: Frame): MutableFrame {
        var parentRenderComponent: ViewContainer<*, *>? = parent
        val resFrame = MutableFrame(frame.x, frame.y, frame.width, frame.height)
        while (parentRenderComponent?.isRenderView() == false) {
            resFrame.x += parentRenderComponent.flexNode.layoutFrame.x
            resFrame.y += parentRenderComponent.flexNode.layoutFrame.y
            parentRenderComponent = parentRenderComponent.parent
        }
        return resFrame
    }

    override fun convertFrame(frame: Frame, toView: ViewContainer<*, *>?): Frame {
        var targetView : ViewContainer<*, *>? = toView ?: getPager() as ViewContainer<*, *>
        while (targetView?.isVirtualView() == true) {
            targetView = targetView.parent
        }
        var parentView: ViewContainer<*, *>? = domParent
        val resFrame = frame.toMutableFrame()
        val pager = getPager()
        while (parentView != targetView) {
            if (parentView == null) {
                break
            }
            resFrame.x += parentView.frame.x
            resFrame.y += parentView.frame.y
            parentView = parentView.domParent
            if (parentView === pager) {
                break
            }
        }
        return resFrame.toFrame()
    }

    override fun animateToAttr(animation: Animation, completion: ((Boolean)->Unit)?, attrBlock: Attr.() -> Unit) {
        val taskKey = "animationAttrTask_" + animation.hashCode()
        completion?.also {
            animation.key = taskKey
            val animateCompletionMap = getAnimateCompletionMap()
            animateCompletionMap[taskKey] = it
            getViewEvent().listenInternalAnimationCompletion { params ->
                animateCompletionMap[params.animationKey]?.also {
                    it.invoke(params.finish.toBoolean())
                }
            }
        }
        getViewAttr().apply {
            setPropByFrameTask(taskKey) {
                removePropFrameTask(taskKey)
                val resumeAnimation = (getProp(Attr.StyleConst.ANIMATION) as? String) ?: ""
                setProp(Attr.StyleConst.ANIMATION, animation.toString())
                // 设置签证更新，避免属性值不变的情况下，丢失动画回调
                forceUpdate = true
                attrBlock.invoke(this)
                forceUpdate = false
                if (flexNode?.isDirty == true) {
                    getPager().onLayoutView()
                }
                setProp(Attr.StyleConst.ANIMATION, resumeAnimation)
            }
        }
    }

    private fun getAnimateCompletionMap(): MutableMap<String,  (Boolean)->Unit> {
        val animateCompletionMapKey = "animateCompletionMapKey"
        var animateCompletionMap = extProps[animateCompletionMapKey] as? MutableMap<String,  (Boolean)->Unit>
        if (animateCompletionMap == null) {
            animateCompletionMap = fastHashMapOf<String,  ((Boolean)->Unit)>()
            extProps[animateCompletionMapKey] = animateCompletionMap
        }
        return animateCompletionMap
    }

    open fun isRenderView(): Boolean {
        return true
    }

    open fun createViewFromRegister(viewClassName: String): DeclarativeBaseView<*, *>? {
        return (getPager() as? Pager)?.createViewFromRegister(viewClassName)
    }

    override fun layoutFrameDidChanged(frame: Frame) {
        setFrameToRenderView(frame)
        attr.onViewLayoutFrameDidChanged(this)
        event.onViewLayoutFrameDidChanged(this)
    }

    open fun didSetFrameToRenderView() {

    }

    open fun setFrameToRenderView(frame: Frame) {
        // 换算相对到真实父亲的坐标系
        renderView?.also {
            // 设置view的frame之前，传递动画对象
            val animation = getPager().animationManager?.currentLayoutAnimation(nativeRef)
            animation?.run {
                attr.setProp(Attr.StyleConst.ANIMATION, toString())
            }

            // 设置frame到renderView
            val rFrame = frameInParentRenderComponentCoordinate(frame)
            it.setFrame(rFrame.x, rFrame.y, rFrame.width, rFrame.height)
            didSetFrameToRenderView()
            event.onRelativeCoordinatesDidChanged(this)

            // frame设置后，清楚animation对象
            animation?.run {
                attr.setProp(Attr.StyleConst.ANIMATION, "")
            }
        }
    }

    private fun internalCreateEvent(): E {
        val event = createEvent()
        event.init(pagerId,nativeRef)
        return event;
    }

    /**
     * 无障碍朗读文本
     *
     * @param message 朗读内容
     */
    fun accessibilityAnnounce(message: String) {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("accessibilityAnnounce", message, null)
        }
    }

    /**
     * 无障碍焦点触发，如果包含无障碍描述会触发朗读
     */
    fun accessibilityFocus() {
        performTaskWhenRenderViewDidLoad {
            renderView?.callMethod("accessibilityFocus", null, null)
        }
    }

    /**
     * 获取View截图
     * 注：支持鸿蒙平台（1.1.71版本）、iOS平台、Android平台
     *
     * @param type 截图类型
     * @param sampleSize 采样率，取值大于或等于1，默认1
     * @param callback 格式：{ code: Int, data: String?, message: String? }，
     * code：0成功，非0失败；
     * data：缓存key（可用于Image的src）或base64串或文件path，仅成功有该字段；
     * message：错误信息，仅失败有该字段。
     */
    fun toImage(type: ImageType, sampleSize: Int = 1, callback: CallbackFn) {
        performTaskWhenRenderViewDidLoad {
            val params = JSONObject()
                .put("type", type.value)
                .put("sampleSize", max(1, sampleSize))
                .toString()
            renderView?.callMethod("toImage", params, callback)
        }
    }
}

class ViewRef<T : DeclarativeBaseView<*, *>>(
    val pagerId: String,
    val nativeRef: Int
) {
    val view: T?
        get() = PagerManager.getPager(pagerId)
            .getViewWithNativeRef(nativeRef) as? T
}
