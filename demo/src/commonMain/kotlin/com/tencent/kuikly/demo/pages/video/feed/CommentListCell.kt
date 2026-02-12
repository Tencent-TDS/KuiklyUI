package com.tencent.kuikly.demo.pages.video.feed

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.RichText
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

/**
 * 评论区列表Cell，表示每一条评论
 */
internal class CommentListCellView : ComposeView<CommentListCellAttr, ComposeEvent>(){

    private var likeNum by observable(0)
    private var likeStatus by observable(false)
    override fun created() {
        this.likeNum = this.attr.item.likeNum
        this.likeStatus = this.attr.item.likeStatus
    }
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr{
                marginBottom(5f)        // 评论之间的间隔
            }

            // 最外层ContentView，内部是以头像和主要内容划分为左右两块
            View {
                attr {
                    // 水平分割布局，一共分为两块块：头像区、内容区
                    flexDirectionRow()
                    marginTop(10f)
                    marginLeft(10f)
                }
                // 最左区域：头像
                View {
                    Image {
                        attr {
                            alignSelfCenter()
                            resizeContain()
                            size(30f, 30f)
                            borderRadius(15f)
                            src(ctx.attr.item.avatar)
                        }
                    }
                }

                View {
                    attr {
                        marginLeft(10f)
                        flex(1f)
                        // 内容区再垂直分区，分为 “昵称点赞显示”、“评论内容显示”、“评论者信息显示”
                        flexDirectionColumn()
                        justifyContentCenter()
                    }
                    // 昵称 + 点赞数
                    View {
                        attr {
                            flexDirectionRow()
                            alignItemsCenter()
                            height(30f)
                        }
                        // 昵称
                        Text {
                            attr {
                                fontSize(13f)
                                fontWeightBold()
                                color(Color.BLACK)
                                text(ctx.attr.item.nick)
                            }
                        }
                        View {
                            attr {
                                // 位于页面的右侧
                                absolutePosition(right = 10f)
                                flexDirectionRow()
                                alignItemsCenter()
                                backgroundColor(Color.GRAY)
                            }
                            // 点赞数量
                            Text {
                                attr {
                                    if (ctx.likeNum != 0){
                                        text(ctx.likeNum.toString())
                                        marginRight(5f)
                                        fontSize(13f)
                                        color(Color.BLACK)
                                    }
                                    if(ctx.likeNum == 0){
                                        text("")
                                    }
                                }
                            }
                            Image {
                                attr{
                                    size(20f,20f)
                                    resizeCover()
                                    if(ctx.likeStatus){
                                        src(LIKED_ICON)
                                    }else{
                                        src(LIKE_ICON)
                                    }
                                }
                                event {
                                    click {
                                        ctx.likeStatus = !ctx.likeStatus
                                        if(ctx.likeStatus){
                                            ctx.likeNum++
                                        }else{
                                            ctx.likeNum--
                                        }
                                    }
                                }
                            }

                        } // end 点赞view
                    }
                    // 评论内容
                    View {
                        attr {
                            flexDirectionRow()
                            // 自适应布局，此view的宽和高全部都是由子视图来决定
                        }
                        Text {
                            attr {
                                textOverFlowWordWrapping()
                                width(pagerData.pageViewWidth - 50)
                                fontSize(14f)
                                fontWeightNormal()
                                color(Color.BLACK)
                                value(ctx.attr.item.commentContent)
                            }
                        }
                    }
                    // 评论位置、时间等信息
                    View {
                        attr {
                            flexDirectionRow()
                            justifyContentFlexStart()
                            alignItemsCenter()
                        }
                        // IP位置
                        RichText {
                            attr {
                                fontSize(13f)
                                fontWeightNormal()
                                color(0xFF999999)
                                value(ctx.attr.item.location)
                            }
                        }
                        // 发表时间
                        RichText {
                            attr {
                                marginLeft(10f)
                                fontSize(13f)
                                fontWeightNormal()
                                color(0xFF999999)
                                value(ctx.attr.item.time)
                            }
                        }
                        // 中间的点
                        RichText {
                            attr {
                                marginLeft(10f)
                                fontSize(20f)
                                fontWeightNormal()
                                color(0xFF999999)
                                value("·")
                            }
                        }
                        // 回复，带点击事件
                        RichText {
                            attr {
                                marginLeft(10f)
                                fontSize(13f)
                                fontWeightNormal()
                                color(0xFF999999)
                                value("回复")
                            }
                            event {
                                click {
                                    // 出现评论区
                                }
                            }
                        }
                    }
                }


            }
        }
    }

    override fun createAttr(): CommentListCellAttr {
        return CommentListCellAttr()
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }

}
internal class CommentListCellAttr : ComposeAttr() {
    // vflow中统一传递的是VideoData，所以设定接收的参数是对象是正确的
    lateinit var item: CommentItem
}

internal fun ViewContainer<*,*>.CommentListCell(init: CommentListCellView.() -> Unit){
    addChild(CommentListCellView(), init)
}
