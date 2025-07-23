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

package com.tencent.kuikly.core.directives

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.collection.fastArrayListOf
import com.tencent.kuikly.core.exception.throwRuntimeError
import com.tencent.kuikly.core.reactive.ReactiveObserver
import com.tencent.kuikly.core.utils.ConvertUtil

internal enum class ConditionType {
    V_IF, V_ELSE_IF, V_ELSE
}

/**
 * 条件指令标签节点, 作为模板条件指令，如vif, velseif, velse等模板指令
 */
class ConditionView internal constructor(
    private val conditionType: ConditionType,
    private val condition: () -> Any?,
    private val creator: ConditionView.() -> Unit
) : DirectivesView() {
    private var conditionResult: Boolean? = null
    private var didCreated = false
    var needSyncConditionResult = false
    private var rootConditionViewRef: Int = 0
    private var didInit = false

    override fun didInit() {
        super.didInit()
        didInit = true
    }

    override fun viewName(): String {
        return "ConditionView"
    }

    override fun didMoveToParentView() {
        super.didMoveToParentView()
        setupRootConditionViewRef()
        ReactiveObserver.bindValueChange(this) {
            val newValue = ConvertUtil.toBoolean(condition())
            if (newValue != conditionResult) {
                ReactiveObserver.addLazyTaskUtilEndCollectDependency {
                    updateConditionResult(newValue)
                }
            }
        }
    }

    override fun didRemoveFromParentView() {
        super.didRemoveFromParentView()
    }

    private fun setupRootConditionViewRef() {
        if (conditionType == ConditionType.V_IF) {
            rootConditionViewRef = nativeRef
        } else {
            val prevView = this.prevDirectivesView
            if (isIfConditionView(prevView) || isElseIfConditionView(prevView)) {
                rootConditionViewRef =
                    (prevView as? ConditionView)!!.rootConditionViewRef
            } else {
                throwRuntimeError("模板条件指令错误：if else 条件匹配错误")
            }
        }
    }

    private fun updateConditionResult(result: Boolean) {
        conditionResult = result
        needSyncConditionResult = true
        if (!didInit) {
            if (result) {
                syncConditionResult()
            }
        } else {
            syncConditionResult()
        }
    }

    private fun syncConditionResult() {
        val conditionViews = collectSameGroupConditionViews()
        val activeConditionView = getActiveConditionView(conditionViews)
        for (conditionView in conditionViews) {
            (conditionView as ConditionView).needSyncConditionResult = false
            if (conditionView != activeConditionView) {
                (conditionView as ConditionView).removeAllSubViewIfNeed()
            }
        }
        activeConditionView?.createSubViewIfNeed()
    }

    private fun createSubViewIfNeed() {
        if (!didCreated) {
            creator()
            didCreated = true
            if (didInit) { // didInit 之后代表非首次，属于动态更新，需要发送Dom更新指令
                syncCreateSubViewsToDom()
            }
        }
    }

    private fun removeAllSubViewIfNeed() {
        if (didCreated) {
            if (didInit) { // didInit 之后代表非首次，属于动态更新，需要发送Dom更新指令
                syncRemoveSubViewsToDom()
            }
            removeChildren()
            didCreated = false
        }
    }

    private fun getActiveConditionView(conditionViews: MutableList<DeclarativeBaseView<*, *>>): ConditionView? {
        for (view in conditionViews) {
            val conditionView = view as ConditionView
            if (conditionView.conditionResult!!) {
                return conditionView
            }
        }
        return null
    }

    private fun isContinuousConditionView(
        lastView: ConditionView?,
        nextView: ConditionView?
    ): Boolean {
        if (lastView != null
            && nextView != null
            && lastView.rootConditionViewRef == nextView.rootConditionViewRef
        ) {
            return true
        }
        return false
    }

    private fun isIfConditionView(view: DeclarativeBaseView<*, *>?): Boolean {
        if (view != null
            && view is ConditionView
            && view.conditionType == ConditionType.V_IF
        ) {
            return true
        }
        return false
    }

    private fun isElseIfConditionView(view: DeclarativeBaseView<*, *>?): Boolean {
        if (view != null
            && view is ConditionView
            && view.conditionType == ConditionType.V_ELSE_IF
        ) {
            return true
        }
        return false
    }

    // 收集同一组条件标签
    private fun collectSameGroupConditionViews(): MutableList<DeclarativeBaseView<*, *>> {
        val results = fastArrayListOf<DeclarativeBaseView<*, *>>()
        val ifView =
            getPager().getViewWithNativeRef(rootConditionViewRef) as? ConditionView
        if (ifView === null) {
            return results
        }
        var currentConditionView = ifView as? ConditionView;
        while (isContinuousConditionView(ifView, currentConditionView)) {
            results.add(currentConditionView!!)
            currentConditionView =
                currentConditionView.nextDirectivesView as? ConditionView
        }
        return results
    }

    // 同步创建标签信息到dom
    private fun syncCreateSubViewsToDom() {
        val rParent = realParent
        rParent?.also { parent ->
            val domChildren = parent.domChildren()
            this.domChildren().forEach { child ->
                val index = domChildren.indexOf(child)
                if (index < 0 || index >= domChildren.count()) {
                    return;
                }
                parent.insertDomSubView(child, index);
            }
        }
    }

    // 同步删除标签信息到dom
    private fun syncRemoveSubViewsToDom() {
        val rParent = realParent
        rParent?.also { parent ->
            domChildren().forEach { child ->
                parent.removeDomSubView(child)
            }
        }
    }
}

fun ViewContainer<*, *>.vif(
    condition: () -> Any?,
    creator: ConditionView.() -> Unit
) {
    val view = ConditionView(ConditionType.V_IF, condition, creator)
    addChild(view) { }
}

fun ViewContainer<*, *>.velseif(
    condition: () -> Any?,
    creator: ConditionView.() -> Unit
) {
    val view = ConditionView(ConditionType.V_ELSE_IF, condition, creator)
    addChild(view) { }
}

fun ViewContainer<*, *>.velse(creator: ConditionView.() -> Unit) {
    val view = ConditionView(ConditionType.V_ELSE, { true }, creator)
    addChild(view) { }
}
