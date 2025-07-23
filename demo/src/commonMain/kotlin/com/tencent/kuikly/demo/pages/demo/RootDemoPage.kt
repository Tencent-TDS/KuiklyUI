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

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import com.tencent.kuikly.core.reactive.handler.*
internal class ButtonDataItem {
    var title = ""
    var jumUrl = ""
}
@Page("root_demo")
internal class RootDemoPage: BasePager() {

    val itemList : ObservableList<ButtonDataItem> by observableList<ButtonDataItem>()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color(0xFF3c6cbdL))
            }
            // 背景图
            Image {
                attr {
                    absolutePosition(0f, 0f, 0f, 0f)
                    src("https://sqimg.qq.com/qq_product_operations/kan/images/viola/viola_bg.jpg")
                }
            }
            // navBar
            NavBar {
                attr {
                    title = "KTV相关"
                    backDisable = false
                }
            }
            // list
            List {
                attr {
                    flex(1f)
                }
                // margin
                View {
                    attr {
                        height(15f)
                    }
                }
                // button list
                vfor({ctx.itemList}) { item ->
                    Button {
                        attr {
                            margin(15f)
                            height(45f)
                            borderRadius(22.5f)
                            backgroundColor(Color(255, 255, 255,0.4f))
                            titleAttr {
                                text(item.title)
                                fontSize(17f)
                                color(Color.WHITE)
                            }
                        }
                        event {
                            click {
                                ctx.acquireModule<RouterModule>(RouterModule.MODULE_NAME).openPage(item.jumUrl)
                            }
                        }
                    }
                }
            }

        }
    }

    override fun created() {
        super.created()

//        // URLDemo
//        ButtonDataItem().apply {
//            title = "URLDemoo"
//            jumUrl = generateJumpUrl("${title}Page")
//            itemList.add(this)
//        }

        // (Rich)TextViewDemo
        ButtonDataItem().apply {
            title = "KTV页面路由"
            jumUrl = generateJumpUrl("router")
            itemList.add(this)
        }

        // View Demo
        ButtonDataItem().apply {
            title = "KTV View Demo（入门必看）"
            jumUrl = generateJumpUrl("ViewDemoIndexPage")
            itemList.add(this)
        }

        ButtonDataItem().apply {
            title = "命令式Demo"
            jumUrl = generateJumpUrl("root_demo_kit")
            itemList.add(this)
        }

        // (Rich)TextViewDemo
        ButtonDataItem().apply {
            title = "TextViewDemo"
            jumUrl = generateJumpUrl("${title}Page")
            itemList.add(this)
        }
        // ImageViewDemo
        ButtonDataItem().apply {
            title = "Image/GifViewDemo"
            jumUrl = generateJumpUrl("image_demo")
            itemList.add(this)
        }
        // ViewDemo
        ButtonDataItem().apply {
            title = "ViewDemo"
            jumUrl = generateJumpUrl("${title}Page")
            itemList.add(this)
        }
        // ListViewDemo
        ButtonDataItem().apply {
            title = "ListViewDemo"
            jumUrl = generateJumpUrl("${title}Page")
            itemList.add(this)
        }
        // PageListViewDemo
        ButtonDataItem().apply {
            title = "PageListViewDemo"
            jumUrl = generateJumpUrl("${title}Page")
            itemList.add(this)
        }

        // SliderPageViewDemo
        ButtonDataItem().apply {
            title = "SliderPageViewDemo"
            jumUrl = generateJumpUrl("${title}Page")
            itemList.add(this)
        }

        // SliderPageViewDemo
        ButtonDataItem().apply {
            title = "PAGViewDemo"
            jumUrl = generateJumpUrl("${title}Page")
            itemList.add(this)
        }

        // WaterfallListViewDemo
        ButtonDataItem().apply {
            title = "WaterfallListDemo"
            jumUrl = generateJumpUrl("${title}Page")
            itemList.add(this)
        }

        // InputDemo
        ButtonDataItem().apply {
            title = "InputViewDemo"
            jumUrl = generateJumpUrl("${title}Page")
            itemList.add(this)
        }

        // 开通带货Demo
        val peronalInfo = ButtonDataItem().apply {
            title = "InputPersonalInfo"
            jumUrl = generateJumpUrl("${title}Page")
        }
        itemList.add(peronalInfo)

        // ModalViewDemo
        ButtonDataItem().apply {
            title = "ModalViewDemo"
            jumUrl = generateJumpUrl("${title}Page")
            itemList.add(this)
        }

        // Transform Demo
        ButtonDataItem().apply {
            title = "TransformDemo"
            jumUrl = generateJumpUrl("${title}Page")
            itemList.add(this)
        }

        // Animation Demo
        ButtonDataItem().apply {
            title = "AnimationDemo"
            jumUrl = generateJumpUrl("${title}Page")
            itemList.add(this)
        }

        // Event Demo
        ButtonDataItem().apply {
            title = "EventDemo"
            jumUrl = generateJumpUrl("${title}Page")
            itemList.add(this)
        }

        // Event Demo
        ButtonDataItem().apply {
            title = "ReactiveDemo"
            jumUrl = generateJumpUrl("reactive")
            itemList.add(this)
        }

        // 协商历史Demo
        ButtonDataItem().apply {
            title = "协商历史Demo"
            jumUrl = generateJumpUrl("example")
            itemList.add(this)
        }

        // 经典HelloWorldDemo
        ButtonDataItem().apply {
            title = "经典HelloWorldDemo"
            jumUrl = generateJumpUrl("classic_demo")
            itemList.add(this)
        }

        ButtonDataItem().apply {
            title = "profileDemo"
            jumUrl = generateJumpUrl("profileDemoPage")
            itemList.add(this)
        }

    }

    private fun generateJumpUrl(pagerName: String, presentMarginTop: Int = 0) : String {
        return pagerName
    }

}