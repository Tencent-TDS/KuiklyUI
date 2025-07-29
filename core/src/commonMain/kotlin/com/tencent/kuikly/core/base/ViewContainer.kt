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

import com.tencent.kuikly.core.base.attr.CaptureRule
import com.tencent.kuikly.core.base.attr.IContainerLayoutAttr
import com.tencent.kuikly.core.base.attr.IEventCaptureAttr
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.collection.fastArrayListOf
import com.tencent.kuikly.core.collection.toFastList
import com.tencent.kuikly.core.exception.throwRuntimeError
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.layout.FlexWrap
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.layout.LayoutImpl.isMeasureDefined
import com.tencent.kuikly.core.layout.StyleSpace
import com.tencent.kuikly.core.layout.undefined
import com.tencent.kuikly.core.layout.valueEquals
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.views.RichTextView
import com.tencent.kuikly.core.views.ScrollerContentView
import com.tencent.kuikly.core.views.TextAreaView
import com.tencent.kuikly.core.views.TextView

interface IChildInit<T> {
    fun childInit(child: T)
}

abstract class ViewContainer<A : ContainerAttr, E : Event> : DeclarativeBaseView<A, E>() {

    protected val children = fastArrayListOf<DeclarativeBaseView<*, *>>()
    private var lastFrame = Frame(0f, 0f, 0f, 0f)
    private var didCreateFlexNode = false
    private var createRenderViewing = false

    open fun <T : DeclarativeBaseView<*, *>> addChild(child: T, init: T.() -> Unit) {
        addChild(child, init, -1)
    }

    open fun realContainerView(): ViewContainer<*, *> {
        return this
    }

    open fun <T : DeclarativeBaseView<*, *>> addChild(child: T, init: T.() -> Unit, index: Int) {
        internalAddChild(child, index)
        child.willInit()
        child.init()
        child.didInit()
    }

    open fun removeChild(child: DeclarativeBaseView<*, *>) {
        internalRemoveChild(child)
    }

    open fun removeChild(index: Int) {
        internalRemoveChild(children[index])
    }

    open fun templateChildren(): List<DeclarativeBaseView<*, *>> {
        return children.toFastList()
    }

    open fun removeChildren() {
        forEachChild { child ->
            removeChild(child)
        }
    }

    open fun didInsertDomChild(child: DeclarativeBaseView<*, *>, index: Int) {
    }

    fun forEachChild(action: (index: Int, child: DeclarativeBaseView<*, *>) -> Unit) {
        templateChildren().forEachIndexed(action)
    }

    fun forEachChild(action: (child: DeclarativeBaseView<*, *>) -> Unit) {
        templateChildren().forEach(action)
    }

    open fun getChild(index: Int): DeclarativeBaseView<*, *> {
        return children[index]
    }

    /*
     * 获取该容器视图下所有真实子孩子们（供外部使用）
     */
    fun getSubviews(): List<DeclarativeBaseView<*, *>> {
        return domChildren()
    }

    fun childrenSize(): Int {
        return children.size
    }

    /*
     * 获取该容器视图下对应index子孩子视图（供外部使用）
     */
    fun getSubview(index: Int): DeclarativeBaseView<*, *>? {
        val subviews = getSubviews()
        if (index < subviews.size) {
            return subviews[index]
        }
        return null
    }

    private fun renderViews(subView: DeclarativeBaseView<*, *>): List<DeclarativeBaseView<*, *>> {
        val list = fastArrayListOf<DeclarativeBaseView<*, *>>()
        if (subView.isRenderView()) {
            list.add(subView)
        } else if (subView is ViewContainer) {
            list.addAll((subView as ViewContainer).renderChildren())
        }
        return list
    }

    open fun insertSubRenderView(subView: DeclarativeBaseView<*, *>) {
        val currentRenderComponent = currentRenderView()
        currentRenderComponent?.also { curRenderComp ->
            if (curRenderComp.createRenderViewing) {
                return
            }
            curRenderComp.renderView?.also { renderView ->
                val subRenderComponents = renderViews(subView)
                val renderChildren = curRenderComp.renderChildren()
                subRenderComponents.forEach {
                    val insertIndex = renderChildren.indexOf(it)
                    if (insertIndex >= 0) {
                        it.createRenderView()
                        renderView.insertSubRenderView(it.nativeRef, insertIndex)
                        it.renderViewDidMoveToParentRenderView()
                    } else {
                        throw RuntimeException("renderChildren.indexOf(it) not found")
                    }
                }
            }
        }
    }

    // dom相关操作
    open fun insertDomSubView(subView: DeclarativeBaseView<*, *>, index: Int) {
        if (!didCreateFlexNode) {
            return
        }
        insertSubRenderView(subView)
        subView.createFlexNode()
        flexNode.addChildAt(subView.flexNode, index)
        didInsertDomChild(subView, index)
        flexNode.markDirty()
    }

    open fun removeDomSubView(subView: DeclarativeBaseView<*, *>) {
        if (!didCreateFlexNode) {
            return
        }
        val subRenderComponents = renderViews(subView)
        subRenderComponents.forEach {
            it.removeRenderView()
        }
        subView.removeFlexNode()
        flexNode.markDirty()
    }

    override fun createRenderView() {
        createRenderViewing = true
        super.createRenderView()
        createRenderViewing = false
        var index = 0
        renderChildren().forEach { child ->
            child.createRenderView()
            renderView?.insertSubRenderView(child.nativeRef, index++)
            child.renderViewDidMoveToParentRenderView()
        }
    }

    override fun removeRenderView() {
        super.removeRenderView()
        renderChildren().forEach { child ->
            child.removeRenderView()
        }
    }

    override fun createFlexNode() {
        super.createFlexNode()
        if (didCreateFlexNode) {
            throw RuntimeException("error: duplicate createFlexNode")
        }
        didCreateFlexNode = true
        var index = 0
        domChildren().forEach { child ->
            child.createFlexNode()
            flexNode.addChildAt(child.flexNode, index)
            didInsertDomChild(child, index)
            index++
        }
    }

    override fun removeFlexNode() {
        super.removeFlexNode()
        didCreateFlexNode = false
        domChildren().forEach { child ->
            child.removeFlexNode()
        }
    }

    override fun layoutFrameDidChanged(frame: Frame) {
        super.layoutFrameDidChanged(frame)
        if (!isRenderView() && !lastFrame.isDefaultValue()) {
            renderChildren().forEach {
                it.setFrameToRenderView(it.flexNode.layoutFrame)
            }
        }
        lastFrame = frame
    }

    override fun didRemoveFromParentView() {
        super.didRemoveFromParentView()
        removeChildren()
    }

    fun move(from: Int, to: Int, count: Int) {
        if (from == to) {
            return // nothing to do
        }

        for (i in 0 until count) {
            // if "from" is after "to," the from index moves because we're inserting before it
            val fromIndex = if (from > to) from + i else from
            val toIndex = if (from > to) to + i else to + count - 2
            val child = children.removeAt(fromIndex)

            children.add(toIndex, child)
        }
    }

    private fun internalAddChild(child: DeclarativeBaseView<*, *>, index: Int) {
        child.pagerId = pagerId
        child.willMoveToParentComponent()
        if (index < 0) { // append when index -1
            children.add(child)
        } else {
            children.add(index, child)
        }
        child.parentRef = nativeRef
        child.didMoveToParentView()
    }

    private fun internalRemoveChild(child: DeclarativeBaseView<*, *>) {
        child.willRemoveFromParentView()
        children.remove(child)
        child.didRemoveFromParentView()
        child.parentRef = 0
    }

    private fun currentRenderView(): ViewContainer<*, *>? {
        var currentRenderView: ViewContainer<*, *>? = this
        while (currentRenderView?.isRenderView() == false) {
            currentRenderView = currentRenderView.parent
        }
        return currentRenderView
    }

    fun renderChildren(): List<DeclarativeBaseView<*, *>> {
        val renderChildren = fastArrayListOf<DeclarativeBaseView<*, *>>()
        forEachChild { child ->
            if (child.isRenderView()) {
                renderChildren.add(child)
            } else if (child is ViewContainer) {
                renderChildren.addAll((child as ViewContainer).renderChildren())
            }
        }
        return renderChildren
    }

    // 层级扁平下，判断当前容器是否可以扁平
    internal fun isRenderViewForFlatLayer(): Boolean {
        if (CrossPlatFeature.isIgnoreRenderViewForFlatLayer) {
            return true
        }
        if (getPager().isDebugUIInspector) {
            return true
        }
        var domParent = parent
        while (domParent?.isVirtualView() == true) {
            domParent = domParent.parent
        }
        if (domParent is ScrollerContentView) {
            return true
        }
        if ((getViewAttr().isEmpty())
            && getViewAttr().isStaticAttr
            && (getViewEvent().isEmpty())
        ) {
            return false
        }
        return super.isRenderView()
    }

    internal fun markChildTextViewsDirty(){
        forEachChild { child ->
            if (child.isRenderView() && child.flexNode.isMeasureDefined()) {
                if(child is TextView){
                    (child as TextView).markDirty()
                }
                if(child is RichTextView){
                    (child as RichTextView).markDirty()
                }
                if (child is TextAreaView) {
                    (child as TextAreaView).markDirty()
                }
            } else if (child is ViewContainer) {
                (child as ViewContainer).markChildTextViewsDirty()
            }
        }
    }
}

open class ContainerAttr : Attr(), IContainerLayoutAttr, IEventCaptureAttr {

    internal val isHorizontalDirection: Boolean
        get() = flexNode?.flexDirection == FlexDirection.ROW ||  flexNode?.flexDirection == FlexDirection.ROW_REVERSE

    // region: layout
    override fun flexDirection(flexDirection: FlexDirection): ContainerAttr {
        flexNode?.flexDirection = flexDirection
        return this
    }

    fun flexDirectionColumn(): ContainerAttr {
        flexDirection(FlexDirection.COLUMN)
        return this
    }

    open fun flexDirectionRow(): ContainerAttr {
        flexDirection(FlexDirection.ROW)
        return this
    }

    override fun flexWrap(flexWrap: FlexWrap): ContainerAttr {
        flexNode?.flexWrap = flexWrap
        return this
    }

    fun flexWrapNoWrap(): ContainerAttr {
        flexWrap(FlexWrap.NOWRAP)
        return this
    }

    fun flexWrapWrap(): ContainerAttr {
        flexWrap(FlexWrap.WRAP)
        return this
    }

    override fun justifyContent(justifyContent: FlexJustifyContent): ContainerAttr {
        flexNode?.justifyContent = justifyContent
        return this
    }

    fun justifyContentCenter(): ContainerAttr {
        justifyContent(FlexJustifyContent.CENTER)
        return this
    }

    fun justifyContentFlexStart(): ContainerAttr {
        justifyContent(FlexJustifyContent.FLEX_START)
        return this
    }

    fun justifyContentFlexEnd(): ContainerAttr {
        justifyContent(FlexJustifyContent.FLEX_END)
        return this
    }

    fun justifyContentSpaceAround(): ContainerAttr {
        justifyContent(FlexJustifyContent.SPACE_AROUND)
        return this
    }

    fun justifyContentSpaceEvenly(): ContainerAttr {
        justifyContent(FlexJustifyContent.SPACE_EVENLY)
        return this
    }

    fun justifyContentSpaceBetween(): ContainerAttr {
        justifyContent(FlexJustifyContent.SPACE_BETWEEN)
        return this
    }

    override fun alignItems(alignItems: FlexAlign): ContainerAttr {
        flexNode?.alignItems = alignItems
        return this
    }

    fun alignItemsCenter(): ContainerAttr {
        alignItems(FlexAlign.CENTER)
        return this
    }

    fun alignItemsFlexStart(): ContainerAttr {
        alignItems(FlexAlign.FLEX_START)
        return this
    }

    fun alignItemsFlexEnd(): ContainerAttr {
        alignItems(FlexAlign.FLEX_END)
        return this
    }

    fun alignItemsStretch(): ContainerAttr {
        alignItems(FlexAlign.STRETCH)
        return this
    }
    fun allCenter(): ContainerAttr {
        alignItems(FlexAlign.CENTER)
        justifyContent(FlexJustifyContent.CENTER)
        return this
    }

    override fun padding(
        top: Float, /* = Float.undefined */
        left: Float, /* = Float.undefined */
        bottom: Float, /* = Float.undefined */
        right: Float /* = Float.undefined */
    ): ContainerAttr {
        if (!top.valueEquals(Float.undefined)) flexNode?.setPadding(StyleSpace.Type.TOP, top)
        if (!bottom.valueEquals(Float.undefined)) flexNode?.setPadding(
            StyleSpace.Type.BOTTOM,
            bottom
        )
        if (!right.valueEquals(Float.undefined)) flexNode?.setPadding(StyleSpace.Type.RIGHT, right)
        if (!left.valueEquals(Float.undefined)) flexNode?.setPadding(StyleSpace.Type.LEFT, left)
        return this
    }

    fun padding(all: Float): ContainerAttr {
        padding(top = all, left = all, bottom = all, right = all)
        return this
    }

    fun paddingTop(top: Float): ContainerAttr {
        padding(top = top)
        return this
    }

    fun paddingBottom(bottom: Float): ContainerAttr {
        padding(bottom = bottom)
        return this
    }

    fun paddingLeft(left: Float): ContainerAttr {
        padding(left = left)
        return this
    }

    fun paddingRight(right: Float): ContainerAttr {
        padding(right = right)
        return this
    }

    override fun capture(vararg rule: CaptureRule?) {
        IEventCaptureAttr.CAPTURE with if (rule.isEmpty()) "[]" else JSONArray().apply {
            rule.forEach {
                if (it != null) {
                    put(it.encode())
                }
            }
        }.toString()
    }
}
