package com.tencent.kuikly.demo.pages.waterfall_app.components

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.log.KLog

/**
 * 底部导航栏组件
 */
internal class BottomNavigationBar(
    private val tabTitles: List<String> = listOf("首页", "热门", "消息", "我")
) : ComposeView<BottomNavigationBarAttr, BottomNavigationBarEvent>() {
    
    override fun createEvent(): BottomNavigationBarEvent {
        return BottomNavigationBarEvent()
    }

    override fun createAttr(): BottomNavigationBarAttr {
        return BottomNavigationBarAttr()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            // 底部导航栏
            View {
                attr {
                    height(45f)
                    backgroundColor(Color.WHITE)
                    padding(5f, 0f)
                }

                // 导航项容器
                View {
                    attr {
                        height(35f) // 减小容器高度
                        flexDirectionRow()
                        alignItemsCenter()
                        justifyContentSpaceEvenly() // 均匀分布
                        padding(0f, 0f) // 移除左右内边距，让导航项更分散
                    }

                    // 首页
                    ctx.createNavItem("首页", 0).invoke(this)

                    // 热门
                    ctx.createNavItem("热门", 1).invoke(this)

                    // 创作按钮（红色加号）
                    View {
                        attr {
                            width(32f)
                            height(32f)
                            backgroundColor(Color(0xFFFF2442))
                            borderRadius(16f) // 调整圆角
                            allCenter()
                        }

                        Text {
                            attr {
                                text("+")
                                fontSize(20f)
                                color(Color.WHITE)
                                fontWeightBold()
                            }
                        }

                        event {
                            click {
                                // 创作功能点击事件
                                KLog.i("BottomNavigationBar", "点击创作按钮")
                                ctx.event.onCreateClick?.invoke()
                            }
                        }
                    }

                    // 消息
                    ctx.createNavItem("消息", 2).invoke(this)

                    // 我
                    ctx.createNavItem("我", 3).invoke(this)
                }
            }
        }
    }
    
    /**
     * 创建导航项
     */
    private fun createNavItem(title: String, index: Int): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    width(40f) // 进一步减小宽度
                    height(35f) // 匹配容器高度
                    allCenter()
                }

                // 标题
                Text {
                    attr {
                        text(title)
                        fontSize(16f)
                        color(if (ctx.attr.currentTabIndex == index) Color(0xFFFF2442) else Color(0xFF999999))
                        if (ctx.attr.currentTabIndex == index) fontWeightBold()
                    }
                }

                event {
                    click {
                        KLog.i("BottomNavigationBar", "点击导航项: $title")
                        ctx.event.onTabClick?.invoke(index, title)
                    }
                }
            }
        }
    }
}

internal class BottomNavigationBarAttr : ComposeAttr() {
    var currentTabIndex by observable(0)
}

internal class BottomNavigationBarEvent : ComposeEvent() {
    var onTabClick: ((Int, String) -> Unit)? = null
    var onCreateClick: (() -> Unit)? = null
}

internal fun ViewContainer<*, *>.BottomNavigationBar(
    tabTitles: List<String> = listOf("首页", "热门", "消息", "我"),
    init: BottomNavigationBar.() -> Unit
) {
    addChild(BottomNavigationBar(tabTitles), init)
}