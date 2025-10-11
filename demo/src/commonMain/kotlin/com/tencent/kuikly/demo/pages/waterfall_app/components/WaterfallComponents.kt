package com.tencent.kuikly.demo.pages.waterfall_app.components

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.waterfall_app.models.MessageItem

/**
 * 消息项组件
 */
internal class MessageItemView(private val messageItem: MessageItem) : ComposeView<MessageItemViewAttr, MessageItemViewEvent>() {
    
    override fun createEvent(): MessageItemViewEvent {
        return MessageItemViewEvent()
    }

    override fun createAttr(): MessageItemViewAttr {
        return MessageItemViewAttr()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    flexDirectionRow()
                    padding(12f, 16f, 12f, 24f)
                    alignItemsCenter()
                    backgroundColor(Color.WHITE)
                }

                // 头像容器
                View {
                    attr {
                        width(50f)
                        height(50f)
                        marginRight(12f)
                    }

                    // 使用Stack布局来叠加头像和在线状态指示器
                    View {
                        attr {
                            width(50f)
                            height(50f)
                        }

                        // 头像
                        Image {
                            attr {
                                width(50f)
                                height(50f)
                                borderRadius(25f)
                                src(ctx.messageItem.userAvatar)
                                backgroundColor(Color(0xFFF0F0F0))
                            }
                        }
                    }

                    // 在线状态指示器 - 使用绝对定位的替代方案
                    if (ctx.messageItem.isOnline) {
                        View {
                            attr {
                                width(12f)
                                height(12f)
                                borderRadius(6f)
                                backgroundColor(Color(0xFF00C851))
                                marginTop(-12f) // 向上偏移
                                marginLeft(38f) // 向右偏移到头像右下角
                            }
                        }
                    }
                }

                // 消息内容
                View {
                    attr {
                        flex(1f)
                        flexDirectionColumn()
                        justifyContentCenter()
                    }

                    // 用户名和时间
                    View {
                        attr {
                            flexDirectionRow()
                            alignItemsCenter()
                            justifyContentSpaceBetween()
                            marginBottom(4f)
                        }

                        Text {
                            attr {
                                text(ctx.messageItem.userName)
                                fontSize(16f)
                                color(Color(0xFF333333))
                                fontWeightBold()
                            }
                        }

                        Text {
                            attr {
                                text(ctx.messageItem.time)
                                fontSize(12f)
                                color(Color(0xFF999999))
                            }
                        }
                    }

                    // 最后一条消息
                    View {
                        attr {
                            flexDirectionRow()
                            alignItemsCenter()
                            justifyContentSpaceBetween()
                        }

                        Text {
                            attr {
                                text(ctx.messageItem.lastMessage)
                                fontSize(14f)
                                color(Color(0xFF666666))
                                flex(1f)
                            }
                        }

                        // 未读消息数量
                        if (ctx.messageItem.unreadCount > 0) {
                            View {
                                attr {
                                    // 根据数字长度动态调整宽度
                                    val count = ctx.messageItem.unreadCount
                                    val displayText = if (count > 99) "99+" else count.toString()
                                    
                                    // 单个数字使用圆形，多个数字使用椭圆形
                                    if (displayText.length == 1) {
                                        width(18f)
                                        height(18f)
                                    } else {
                                        minWidth(20f)
                                        height(18f)
                                        padding(left = 4f, right = 4f)
                                    }
                                    
                                    borderRadius(9f)
                                    backgroundColor(Color(0xFFFF2442))
                                    justifyContentCenter()
                                    alignItemsCenter()
                                    marginLeft(8f)
                                }

                                Text {
                                    attr {
                                        text(if (ctx.messageItem.unreadCount > 99) "99+" else ctx.messageItem.unreadCount.toString())
                                        fontSize(10f)
                                        color(Color.WHITE)
                                        fontWeightBold()
                                        textAlignCenter() // 确保文字居中对齐
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 分隔线
            View {
                attr {
                    height(0.5f)
                    backgroundColor(Color(0xFFEEEEEE))
                    marginLeft(78f) // 对齐消息内容
                }
            }
        }
    }
}

internal class MessageItemViewAttr : ComposeAttr()
internal class MessageItemViewEvent : ComposeEvent()

internal fun ViewContainer<*, *>.MessageItemView(messageItem: MessageItem, init: MessageItemView.() -> Unit) {
    addChild(MessageItemView(messageItem), init)
}

/**
 * 瀑布流空状态组件
 */
internal class WaterfallEmptyView(private val title: String): ComposeView<WaterfallEmptyViewAttr, WaterfallEmptyViewEvent>() {
    
    override fun createEvent(): WaterfallEmptyViewEvent {
        return WaterfallEmptyViewEvent()
    }

    override fun createAttr(): WaterfallEmptyViewAttr {
        return WaterfallEmptyViewAttr()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
                flex(1f)
                backgroundColor(Color(0xFFF5F5F5))
            }
            Text {
                attr {
                    text("${ctx.title}正在开发中...")
                    fontSize(18f)
                    color(Color(0xFF666666))
                }
            }
        }
    }
}

internal class WaterfallEmptyViewAttr : ComposeAttr()
internal class WaterfallEmptyViewEvent : ComposeEvent()

internal fun ViewContainer<*, *>.WaterfallEmptyView(title: String, init: WaterfallEmptyView.() -> Unit) {
    addChild(WaterfallEmptyView(title), init)
}