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

import com.tencent.kuikly.core.pager.PageData
abstract class ComposeView<A : ComposeAttr, E : ComposeEvent> :
    ViewContainer<A, E>() {
    val pagerData : PageData
        get() = getPager().pageData
    /*
    * 组合组件生命周期
     */
    /// body创建前调用
    open fun created() {
    }

    open fun viewWillLoad() {
    }

    open fun viewDidLoad() {
    }

    open fun viewDidLayout() {
    }

    open fun viewWillUnload() {
    }

    open fun viewDidUnload() {
    }

    open fun viewDestroyed() {
    }

    override fun willInit() {
        super.willInit()
    }

    override fun didInit() {
        super.didInit()
        created()
        viewWillLoad()
        body()()
        viewDidLoad()
        getPager().addTaskWhenPagerUpdateLayoutFinish {
            viewDidLayout()
        }
    }

    override fun willRemoveFromParentView() {
        super.willRemoveFromParentView()
        viewWillUnload()
    }

    override fun didRemoveFromParentView() {
        super.didRemoveFromParentView()
        viewDidUnload()
        viewDestroyed()
    }

    override fun viewName(): String {
        return ViewConst.TYPE_VIEW
    }

    override fun isRenderView(): Boolean {
       return isRenderViewForFlatLayer()
    }

    open fun emit(eventName: String, param: Any?) {
        this.event.onFireEvent(eventName, param)
    }

    abstract fun body(): ViewBuilder

}

typealias ViewBuilder = ViewContainer<*, *>.() -> Unit