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
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.SliderPage
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import com.tencent.kuikly.core.reactive.handler.*
internal class Item {
    lateinit var bgColor : Color
    lateinit var title : String
}
@Page("SliderPageViewDemoPage")
internal class SliderPageViewDemoPage: BasePager() {

    var pageItemList = arrayListOf<Item>()
    var currentIndex : Int by observable(0)

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
                   title = "轮播图组件Demo"
               }
           }

           List {
               attr {
                   flex(1f)
               }
               View {
                   SliderPage {
                       attr {
                           isHorizontal = true
                           pageItemWidth = 375f
                           pageItemHeight = 375f

                           initSliderItems(ctx.pageItemList) { item ->
                               View {
                                   attr {
                                       backgroundColor(item.bgColor)
                                       allCenter()
                                   }
                                   Text {
                                       attr {
                                           text(item.title)
                                           fontSize(20f)
                                           color(Color.BLACK)
                                       }
                                   }
                               }
                           }
                       }
                   event {
                       pageIndexDidChanged {
                           ctx.currentIndex = (it as JSONObject).optInt("index")
                       }
                   }

                   }
               }

               View {
                   attr {
                       height(40f)
                       allCenter()
                       flexDirectionRow()
                   }
                   ctx.pageItemList.forEachIndexed { index, item ->
                       View {
                           attr {
                               size(5f, 5f)
                               borderRadius(2.5f)
                               margin(4f)
                               if (index == ctx.currentIndex) {
                                   backgroundColor(Color.WHITE)
                               } else {
                                   backgroundColor(Color(255, 255, 255, 0.5f))
                               }

                           }

                       }
                   }

               }

           }
       }
    }

    override fun created() {
        super.created()
        apply {
            val item = Item()
            item.bgColor = Color.YELLOW
            item.title = "第一页"
            pageItemList.add(item)
        }
        apply {
            val item = Item()
            item.bgColor = Color.RED
            item.title = "第二页"
            pageItemList.add(item)
        }

        apply {
            val item = Item()
            item.bgColor = Color.BLUE
            item.title = "第三页"
            pageItemList.add(item)
        }

    }

}