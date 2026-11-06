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

package com.tencent.kuikly.demo.pages.demo.base

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseOhos

/** 返回箭头图标，自绘版与沉浸光感版共用 */
private const val NAV_BACK_ICON =
    "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAASBAMAAAB/WzlGAAAAElBMVEUAAAAAAAAAAAAAAAAAAAAAAADgKxmiAAAABXRSTlMAIN/PELVZAGcAAAAkSURBVAjXYwABQTDJqCQAooSCHUAcVROCHBiFECTMhVoEtRYA6UMHzQlOjQIAAAAASUVORK5CYII="

internal class NavigationBar: ComposeView<NavigationBarAttr, ComposeEvent>() {
    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }

    override fun createAttr(): NavigationBarAttr {
        return NavigationBarAttr()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            if (ctx.attr.immersiveLightSense) {
                // 沉浸光感版：把自绘标题栏的内容原样搬进沉浸光感容器。
                // 业务只需打开这个开关，不需要自己去容器里拼内容。
                ImmersiveLightSenseOhos {
                    attr {
                        height(ctx.pagerData.statusBarHeight + 56f)
                        padding(top = ctx.pagerData.statusBarHeight, left = 12f, right = 12f)
                        flexDirectionRow()
                        alignItemsCenter()
                        backgroundColor(Color.TRANSPARENT)
                        // 标题栏区域的"玻璃底"，档位由业务通过 immersiveMaterialStyle 指定
                        immersiveMaterial(style = ctx.attr.immersiveMaterialStyle, interactive = true)
                    }

                    // 返回：与自绘版同样的能力，只是外壳换成带材质的圆形按钮
                    vif({ !ctx.attr.backDisable }) {
                        View {
                            attr {
                                width(36f)
                                height(36f)
                                borderRadius(18f)
                                allCenter()
                                backgroundColor(Color.TRANSPARENT)
                                immersiveMaterial(style = ctx.attr.immersiveMaterialStyle, interactive = true)
                            }
                            Image {
                                attr {
                                    size(10f, 17f)
                                    testTag("nav_back")
                                    src(NAV_BACK_ICON)
                                }
                                event {
                                    click {
                                        getPager().acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                                    }
                                }
                            }
                        }
                    }

                    // 标题
                    View {
                        attr {
                            flex(1f)
                            allCenter()
                        }
                        Text {
                            attr {
                                text(ctx.attr.title)
                                fontSize(17f)
                                fontWeightSemiBold()
                                color(Color.WHITE)
                            }
                        }
                    }
                }
            } else {
            View {
                attr {
                    paddingTop(ctx.pagerData.statusBarHeight)
                    backgroundColor(Color.WHITE)
                }
                // nav bar
                View {
                    attr {
                        height(44f)
                        allCenter()
                    }

                    Text {
                        attr {
                            text(ctx.attr.title)
                            color(Color.BLACK)
                            fontSize(17f)
                            fontWeightSemiBold()
                        }
                    }

                }

                vif({!ctx.attr.backDisable}) {
                    Image {
                        attr {
                            absolutePosition(12f + getPager().pageData.statusBarHeight, 12f, 12f, 12f)
                            size(10f, 17f)
                            testTag("nav_back")
                            src("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAASBAMAAAB/WzlGAAAAElBMVEUAAAAAAAAAAAAAAAAAAAAAAADgKxmiAAAABXRSTlMAIN/PELVZAGcAAAAkSURBVAjXYwABQTDJqCQAooSCHUAcVROCHBiFECTMhVoEtRYA6UMHzQlOjQIAAAAASUVORK5CYII=")
                        }
                        event {
                            click {
                                getPager().acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                            }
                        }
                    }
                }

            }
            }
        }
    }
}

internal class NavigationBarAttr : ComposeAttr() {
    var title : String by observable("")
    var backDisable = false

    /**
     * 是否使用鸿蒙「沉浸光感」标题栏（默认 false，保持自绘白底）。
     *
     * 打开后：标题栏内容（返回 + 标题）会搬进 ImmersiveLightSenseOhos 容器，
     * 在支持沉浸光感的鸿蒙设备上呈现系统原生材质效果；
     * 设备不支持时容器自动降级为普通容器，内容照常显示。
     */
    var immersiveLightSense : Boolean by observable(false)

    /**
     * 沉浸光感材质档位，只在 [immersiveLightSense] 打开时生效。
     * 0=ULTRA_THIN 1=THIN 2=REGULAR 3=THICK 4=ULTRA_THICK，默认 REGULAR。
     */
    var immersiveMaterialStyle : Int by observable(2)
}

internal fun ViewContainer<*, *>.NavBar(init: NavigationBar.() -> Unit) {
    addChild(NavigationBar(), init)
}