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

package com.tencent.kuikly.demo.pages.demo.catalog

import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("ExampleIndexPage")
internal class ExampleIndexPage : BasePager() {
    private val itemList by observableList<ExampleItemData>()

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "Kuikly Example"
                }
            }
            List {
                attr {
                    flex(1f)
                }
                vfor({ ctx.itemList }) { item ->
                    ExampleItem {
                        attr {
                            itemData = item
                        }
                    }
                }
            }
        }
    }

    override fun created() {
        itemList.add(ExampleItemData().apply {
            avatarText = "App"
            titleText = "App Demo"
            subtitleText = "现提供信息Feed流，功能不断完善中"
            declarativeExampleUrl = generateJumpUrl("AppTabPage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Vi"
            titleText = "View Attr"
            subtitleText = "使用View的属性，为你的app中添加不同的样式"
            declarativeExampleUrl = generateJumpUrl("ViewExamplePage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Tr"
            titleText = "View Transform"
            subtitleText = "对View进行Transform，支持平移、旋转和缩放"
            declarativeExampleUrl = generateJumpUrl("TransformExamplePage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Fl"
            titleText = "View FlexBox"
            subtitleText = "使用Flex布局来简化布局工作，此Demo大部分页面都使用Flex布局"
            declarativeExampleUrl = generateJumpUrl("FlexExamplePage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Zi"
            titleText = "View ZIndex"
            subtitleText = "支持View设置ZIndex属性，以调整View之间的遮挡"
            declarativeExampleUrl = generateJumpUrl("ZIndexExamplePage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "An"
            titleText = "KTView Animation"
            subtitleText = "简单好用的动画，也支持动画状态的监听"
            declarativeExampleUrl = generateJumpUrl("AnimationExamplePage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Te"
            titleText = "Text/RichTextView"
            subtitleText = "支持字体、颜色和字重等常用属性，也支持富文本"
            declarativeExampleUrl = generateJumpUrl("TextExamplePage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Im"
            titleText = "ImageView"
            subtitleText = "使用ImageView为你的app添加图片"
            declarativeExampleUrl = generateJumpUrl("ImageExamplePage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "In"
            titleText = "InputView"
            subtitleText = "单行/多行(TextArea)输入框组件，用法一致"
            declarativeExampleUrl = generateJumpUrl("InputViewDemoPage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Ca"
            titleText = "CanvasView"
            subtitleText = "和h5一致的canvas标签组件，实现自绘不规则图形"
            declarativeExampleUrl = generateJumpUrl("CanvasExamplePage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Bu"
            titleText = "ButtonView"
            subtitleText = "Kuikly中的Button实际上是一个带有文字和背景颜色的View"
            declarativeExampleUrl = generateJumpUrl("ButtonExamplePage")
        })

        // 缺少一个scroller

        itemList.add(ExampleItemData().apply {
            avatarText = "Li"
            titleText = "ListView"
            subtitleText = "Scroller基础上实现无Diff的View粒度复用、原生性能的无限流列表"
            declarativeExampleUrl = generateJumpUrl("ListExamplePage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Wa"
            titleText = "WaterfallView"
            subtitleText = "List基础上实现瀑布流布局的列表组件"
            declarativeExampleUrl = generateJumpUrl("WaterfallListDemoPage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Re"
            titleText = "SliderPageView"
            subtitleText = "基于PageList实现的轮播图组件"
            declarativeExampleUrl = generateJumpUrl("SliderPageViewDemoPage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Pa"
            titleText = "PageListView"
            subtitleText = "基于List实现的分页组件"
            declarativeExampleUrl = generateJumpUrl("PageListExamplePage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Mo"
            titleText = "ModalView"
            subtitleText = "全屏模态组件，用来做alert弹窗或者浮层"
            declarativeExampleUrl = generateJumpUrl("ModalViewDemoPage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Re"
            titleText = "Refresh&FooterRefresh"
            subtitleText = "列表头部刷新和尾部刷新组件（适用于任何列表，包括瀑布流和List以及PageList）"
            declarativeExampleUrl = generateJumpUrl("ListViewDemoPage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Da"
            titleText = "DatePickerView/ScrollPicker"
            subtitleText = "日期选择器/滚动选择组件，可用作日期选择或者地区选择等自定义场景"
            declarativeExampleUrl = generateJumpUrl("ScrollPickerExamplePage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Sl"
            titleText = "SliderView"
            subtitleText = "滑动选择器组件，可用于拖动进度条场景"
            declarativeExampleUrl = generateJumpUrl("SliderExamplePage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Sw"
            titleText = "SwitchView"
            subtitleText = "开关组件，支持属性定制自定义开关样式"
            declarativeExampleUrl = generateJumpUrl("SwitchExamplePage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Bl"
            titleText = "BlurView"
            subtitleText = "高斯模糊组件，盖住其他view可进行动态高斯模糊布局位置下方的视图"
            declarativeExampleUrl = generateJumpUrl("BlurExamplePage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Ac"
            titleText = "ActivityIndicatorView"
            subtitleText = "活动指示器，又名loading菊花，安卓风格和iOS一致"
            declarativeExampleUrl = generateJumpUrl("ActivityIndicatorExamplePage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Ho"
            titleText = "HoverView"
            subtitleText = "悬停置顶，用于列表下的自动悬停（列表滚动可自动悬浮置顶）视图组件"
            declarativeExampleUrl = generateJumpUrl("HoverExamplePage")
        })

        // 缺mask
        itemList.add(ExampleItemData().apply {
            avatarText = "Ma"
            titleText = "MaskView"
            subtitleText = "遮罩视图，可用于屏蔽视图内容"
            declarativeExampleUrl = generateJumpUrl("mask_demo")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Ch"
            titleText = "CheckBoxView"
            subtitleText = "复选框组件，可用作单击选中态/非选中态的切换展示"
            declarativeExampleUrl = generateJumpUrl("CheckBoxExamplePage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Pa"
            titleText = "PAGView"
            subtitleText = "PAG，类Lottie播放动画的组件"
            declarativeExampleUrl = generateJumpUrl("PAGViewDemoPage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Ap"
            titleText = "APNGView"
            subtitleText = "APNG，播放APNG图片动画的组件"
            declarativeExampleUrl = generateJumpUrl("APNGExamplePage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Ta"
            titleText = "TabsView"
            subtitleText = "选项卡切换组件（与PageList分页列表组件配套使用）"
            declarativeExampleUrl = generateJumpUrl("TabsExamplePage")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Al"
            titleText = "AlertDialog"
            subtitleText = "提示对话框组件，UI风格对齐iOS UIAlertController风格, 并支持自定义弹窗UI）"
            declarativeExampleUrl = generateJumpUrl("AlertDialogDemo")
        })

        itemList.add(ExampleItemData().apply {
            avatarText = "Vi"
            titleText = "VideoView"
            subtitleText = "使用VideoView为你的app添加视频"
            declarativeExampleUrl = generateJumpUrl("VideoExamplePage")
        })

    }

    private fun generateJumpUrl(pagerName: String) : String {
        return pagerName
    }

}

internal class ExampleItemData {
    var avatarText by observable("")
    var titleText by observable("")
    var subtitleText by observable("")
    var declarativeExampleUrl by observable("")
}
