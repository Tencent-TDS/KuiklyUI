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

package com.tencent.kuikly.demo.pages.base.extension

import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image

internal class GCViewAttr : ComposeAttr() {
    internal var internalBackgroundImageSrc : String? by observable(null)
    fun backgroundImage(src: String): ComposeAttr {
        internalBackgroundImageSrc = src
        return this
    }

}

internal class GCViewEvent : ComposeEvent()

internal class GCView : ComposeView<GCViewAttr, GCViewEvent>() {

    override fun willInit() {
        super.willInit()
        val ctx = this
        vbind({attr.internalBackgroundImageSrc}) {
            Image {
                attr {
                    absolutePositionAllZero()
                    src(ctx.attr.internalBackgroundImageSrc ?: "")
                }
            }
        }
    }

    override fun body(): ViewBuilder {
        return {}
    }

    override fun createAttr() = GCViewAttr()

    override fun createEvent() = GCViewEvent()

}

internal fun ViewContainer<*, *>.GCView(init : GCView.() -> Unit) {
    addChild(GCView(), init)
}
