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


package com.tencent.kuikly.demo.pages.video.feed

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.base.event.EventHandlerFn
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.ListView
import com.tencent.kuikly.core.views.SpringAnimation
import com.tencent.kuikly.core.views.View

import com.tencent.kuikly.demo.pages.video.type.VideoItem
import kotlin.math.abs

internal class CommentView: ComposeView<CommentViewAttr, CommentViewEvent>() {

    private var animated: Boolean by observable(false)    // 构建vif条件
    private var listViewRef: ViewRef<ListView<*, *>>? = null
    private var exit: Boolean by observable(false)
    private var commentViewHeight =  450f    // 显示评论区域高度
    private var tabHeight = 40f              // 评论区顶部说明信息区域高度 ***改名 headerTab

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                absolutePosition(0f, 0f, 0f, 0f)

                // 页面背景透明度变化 -> 作为动画执行的导引
                if (ctx.animated) {
                    backgroundColor(Color(0, 0, 0, 0.5f))
                } else {
                    backgroundColor(Color(0, 0, 0, 0f))
                }
                animation(Animation.springEaseIn(1.0f, 0.92f, 1f), ctx.animated)
            }
            // 实现点击评论区外部即可关闭评论区
            event {
                click {
                    ctx.animated = false
                }
                animationCompletion {
                    if (!ctx.animated) {
                        this@CommentView.emit(CommentViewEvent.CLOSE, it);
                    }
                }
            }
            // 评论区View
            TransitionFromBottomView {
                attr {
                    flexDirectionColumn()
                    size(pagerData.pageViewWidth, ctx.commentViewHeight)
                    transitionAppear = ctx.animated
                    positionAbsolute()
                    bottom(0f)
                }

                // 评论内容部分
                List {
                    ref {
                        ctx.listViewRef = it
                    }
                    attr {
                        flex(1f)
                        flexDirectionColumn()
                        backgroundColor(Color.WHITE)        // 使得List自带的透明分隔条失效
                        size(pagerData.pageViewWidth, ctx.commentViewHeight)
                        bottom(0f)
                        justifyContentCenter()
                        showScrollerIndicator(false)
                        borderRadius(16f,16f,0f,0f)
                    }
                    event {
                        // 事件：支持可评论区可拖拽的能力，直接沿用了样例中模块的大幅度下拉的效果
                        willDragEndBySync {
                            // 大幅度下拉list时退出界面
                            if (it.offsetY < 0 && abs(it.offsetY) > ctx.commentViewHeight / 5) {
                                // 设置延迟动画
                                setTimeout(5) {
                                    ctx.listViewRef?.view?.setContentOffset(0f, -ctx.commentViewHeight, true, SpringAnimation(200,1.0f,1f))
                                }
                                ctx.exit = true

                                // 如果是从初始位置小幅度下拉或者从初始位置上方开始下滑，则恢复到初始位置。
                            } else if (it.offsetY < 0 && abs(it.offsetY) <= ctx.commentViewHeight / 5) {
                                setTimeout(5) {
                                    ctx.listViewRef?.view?.setContentOffset(0f, 0f, true, SpringAnimation(200,1.0f,1f))
                                }
                                ctx.exit = false
                            }
                        }
                        dragEnd {
                            if (ctx.exit) {
                                // 留时间给过度动画播放完
                                setTimeout(200) {
                                    this@CommentView.emit(CommentViewEvent.CLOSE, it);
                                }
                            }
                        }
                    }

                    CommentHeaderBar {
                        attr {
                            vitem = ctx.attr.vitem
                            if (ctx.attr.vitem.searchContent.isNotEmpty()){
                                size(pagerData.pageViewWidth, 100f)
                            }
                            else{
                                size(pagerData.pageViewWidth, 50f)
                            }
                            backgroundColor(Color.WHITE)
                        }
                    }

                    // 显示所有评论
                    // todo 这里的列表来源需要更换，考虑放在哪个类之中
                    for (itemdata in ctx.attr.commentList) {
                        CommentListCell {
                            attr {
                                item = itemdata
                            }
                        }
                    }

                    // list底部兜底填充View，用于弹力拖拽
                    View {
                        attr {
                            height(1f)
                            backgroundColor(Color.WHITE)
                        }
                        View {
                            attr {
                                height(ctx.commentViewHeight)
                                backgroundColor(Color.WHITE)
                            }
                        }
                    }
                }
            }


        }
    }


    override fun createAttr(): CommentViewAttr {
        return CommentViewAttr()
    }
    override fun createEvent(): CommentViewEvent {
        return CommentViewEvent()
    }
    override fun viewDidLayout() {
        super.viewDidLayout()
        animated = true
    }

    override fun created() {

        commentViewHeight = attr.commentViewHeight!!
        KLog.d("4ty32r63t2333333", attr.commentList.toString())

        attr.dataList.add(Item().apply {
            title = "取消售后申请"
            detialInfo = "撤销售后申请\n进入开始流程"
            avatarUrl = "https://pic2.zhimg.com/v2-2a0434dd4e4bb7a638b8df699a505ca1_b.jpg"
            index = attr.dataList.count()
        })
        attr.dataList.add(Item().apply {
            title = "同意售后申请"
            detialInfo = "商家已同意退货申请。\n" +
                    "退货地址：江苏省扬州市仪征新集镇迎宾路3号花藤印染院内亿合帽业二楼"

            index = attr.dataList.count()
            avatarUrl = "https://pic2.zhimg.com/v2-2a0434dd4e4bb7a638b8df699a505ca1_b.jpg"
        })

        attr.dataList.add(Item().apply {
            title = "发起售后申请"
            detialInfo = "发起了退货退款售后申请\n售后类型：退货退款\n货物状态：已收到货\n退货原因：7天无理由退款\n退款金额：¥59\n退货方式：线下寄件"
            avatarUrl =
                "https://p3.toutiaoimg.com/large/pgc-image/54b93ce31b2e47c3aa1224b8fbfe4ffa"
            index = attr.dataList.count()
            pictures.add("https://pic2.zhimg.com/v2-2a0434dd4e4bb7a638b8df699a505ca1_b.jpg")
            pictures.add("https://pic2.zhimg.com/v2-2a0434dd4e4bb7a638b8df699a505ca1_b.jpg")
            pictures.add("https://pic2.zhimg.com/v2-2a0434dd4e4bb7a638b8df699a505ca1_b.jpg")
            pictures.add("https://pic2.zhimg.com/v2-2a0434dd4e4bb7a638b8df699a505ca1_b.jpg")
        })


        attr.dataList.add(Item().apply {
            title = "取消售后申请"
            detialInfo = "撤销售后申请\n进入开始流程"
            avatarUrl = "https://pic2.zhimg.com/v2-2a0434dd4e4bb7a638b8df699a505ca1_b.jpg"
            index = attr.dataList.count()
        })
    }
}

internal class CommentViewAttr : ComposeAttr() {
    lateinit var vitem: VideoItem
    var commentList: MutableList<CommentItem> = mutableListOf() // todo 条数作为参数进行传递
    var dataList: MutableList<Item> = mutableListOf()
    var commentViewHeight by observable(0f)
}

internal class CommentViewEvent : ComposeEvent() {
    fun close(handler: EventHandlerFn) {
        registerEvent(CLOSE, handler)
    }
    companion object {
        const val CLOSE = "close"
    }
}
internal fun ViewContainer<*, *>.QBVideoComment(init: CommentView.() -> Unit) {
    addChild(CommentView(), init)
}

internal class Item {
    var title = ""
    var subTitle = ""
    var detialInfo = ""
    var avatarUrl = ""
    var pictures = arrayListOf<String>()
    var index: Int = 0
}