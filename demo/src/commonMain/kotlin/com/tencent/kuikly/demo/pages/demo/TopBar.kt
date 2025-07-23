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

package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.views.*

/**
 * Created by kam on 2022/6/22.
 */
internal class TopBar : ComposeView<ComposeAttr, ComposeEvent>() {

    lateinit var selfRef : ViewRef<ViewContainer<*, *>>
    override fun createAttr(): ComposeAttr {
        return ComposeAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            ref {
                ctx.selfRef = it
            }
            attr {
                height(88f)
                backgroundColor(Color.WHITE)
                alignItemsCenter()
                justifyContentCenter()
                paddingTop(ctx.pagerData.statusBarHeight)
               // backgroundLinearGradient(Direction.TO_BOTTOM, ColorStop(Color.BLACK, 0f),ColorStop(Color.WHITE, 1f))

            }

            event {
                click {
                    ctx.selfRef?.view?.attr {
                        height(400f)
                    }
                }
            }

            Image {
                attr {
                    absolutePosition(12f + getPager().pageData.statusBarHeight, 12f, 12f, 12f)
                    size(10f, 17f)
                    src("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAASBAMAAAB/WzlGAAAAElBMVEUAAAAAAAAAAAAAAAAAAAAAAADgKxmiAAAABXRSTlMAIN/PELVZAGcAAAAkSURBVAjXYwABQTDJqCQAooSCHUAcVROCHBiFECTMhVoEtRYA6UMHzQlOjQIAAAAASUVORK5CYII=")
                }
                event {
                    click {
                        getPager().acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                 //       Utils.bridgeModule(pagerId).closePage()
                    }
                }
            }

            RichText {
                Span {
                    fontSize(17f)
                    fontWeightBold()
                    color(Color.BLACK)
                    value("协商历史")
                }
            }
        }
    }
}

internal fun ViewContainer<*, *>.Topbar(init: TopBar.() -> Unit) {
    addChild(TopBar(), init)
}