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

import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.domChildren
import com.tencent.kuikly.core.reactive.ReactiveObserver

/// 提供值绑定指令，用于值变化时对应的Component重建
class BindDirectivesView(
    private val bindValue: () -> Any?,
    private val creator: BindDirectivesView.() -> Unit
) : DirectivesView() {
    private var bindValueResult: Any? = null
    private var isDidInit = false
    override fun didInit() {
        super.didInit()
        ReactiveObserver.bindValueChange(this) {
            val newValue = bindValue()
            if (newValue != bindValueResult) {
                bindValueResult = newValue
                if (isDidInit) {
                    ReactiveObserver.addLazyTaskUtilEndCollectDependency {
                        if (newValue == bindValueResult) {
                            removeAllSubView()
                            bindValueResult?.also {
                                createSubView()
                            }
                        }
                    }
                }
            }
        }
        isDidInit = true
        bindValueResult?.also {
            creator()
        }
    }

    override fun didRemoveFromParentView() {
        super.didRemoveFromParentView()
    }

    private fun createSubView() {
        creator()
        syncCreateSubViewsToDom()
    }

    private fun removeAllSubView() {
        syncRemoveSubViewsToDom()
        removeChildren()
    }

    // 同步创建标签信息到dom
    private fun syncCreateSubViewsToDom() {
        val rParent = realParent
        rParent?.also { parent ->
            val domChildren = parent.domChildren()
            this.domChildren().forEach { child ->
                val index = domChildren.indexOf(child)
                if (index < 0 || index >= domChildren.count()) {
                    return
                }
                parent.insertDomSubView(child, index)
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

fun ViewContainer<*, *>.vbind(
    bindValue: () -> Any?,
    creator: BindDirectivesView.() -> Unit
) {
    val view = BindDirectivesView(bindValue, creator)
    addChild(view) { }
}