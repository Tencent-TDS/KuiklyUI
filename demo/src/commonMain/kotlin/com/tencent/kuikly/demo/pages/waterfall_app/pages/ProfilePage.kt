package com.tencent.kuikly.demo.pages.waterfall_app.pages

import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.log.KLog

/**
 * 个人资料页面
 */
internal class WaterfallProfilePage : ComposeView<WaterfallProfilePageAttr, WaterfallProfilePageEvent>() {
    
    // 用户数据
    private var userName by observable("用户名12345")
    private var userBio by observable("记录美好生活 ✨")
    private var userAvatar by observable("https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/8d0813ca.png")
    private var followCount by observable("128")
    private var fansCount by observable("1.2k")
    private var likeCount by observable("3.5k")
    
    override fun createEvent(): WaterfallProfilePageEvent {
        return WaterfallProfilePageEvent()
    }

    override fun createAttr(): WaterfallProfilePageAttr {
        return WaterfallProfilePageAttr()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flex(1f)
                backgroundColor(Color(0xFFF5F5F5))
                flexDirectionColumn()
            }
            
            // 顶部用户信息区域
            View {
                attr {
                    backgroundColor(Color.WHITE)
                    padding(20f)
                    marginBottom(10f)
                }
                
                // 用户头像和基本信息
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        marginBottom(20f)
                    }
                    
                    // 头像
                    Image {
                        attr {
                            width(80f)
                            height(80f)
                            borderRadius(40f)
                            src(ctx.userAvatar)
                        }
                    }
                    
                    // 用户信息
                    View {
                        attr {
                            marginLeft(16f)
                            flex(1f)
                        }
                        
                        // 用户名
                        Text {
                            attr {
                                text(ctx.userName)
                                fontSize(20f)
                                color(Color.BLACK)
                                fontWeightBold()
                                marginBottom(8f)
                            }
                        }
                        
                        // 个人简介
                        Text {
                            attr {
                                text(ctx.userBio)
                                fontSize(14f)
                                color(Color(0xFF666666))
                            }
                        }
                    }
                    
                    // 编辑资料按钮
                    View {
                        attr {
                            width(80f)
                            height(32f)
                            backgroundColor(Color(0xFFFF2442))
                            borderRadius(16f)
                            allCenter()
                        }
                        
                        Text {
                            attr {
                                text("编辑资料")
                                fontSize(12f)
                                color(Color.WHITE)
                            }
                        }
                        
                        event {
                            click {
                                KLog.i("WaterfallProfilePage", "点击编辑资料")
                            }
                        }
                    }
                }
                
                // 统计数据
                View {
                    attr {
                        flexDirectionRow()
                        justifyContentSpaceAround()
                    }
                    
                    // 关注
                    View {
                        attr {
                            allCenter()
                        }
                        
                        Text {
                            attr {
                                text(ctx.followCount)
                                fontSize(18f)
                                color(Color.BLACK)
                                fontWeightBold()
                            }
                        }
                        
                        Text {
                            attr {
                                text("关注")
                                fontSize(12f)
                                color(Color(0xFF666666))
                                marginTop(4f)
                            }
                        }
                        
                        event {
                            click {
                                KLog.i("WaterfallProfilePage", "查看关注列表")
                            }
                        }
                    }
                    
                    // 粉丝
                    View {
                        attr {
                            allCenter()
                        }
                        
                        Text {
                            attr {
                                text(ctx.fansCount)
                                fontSize(18f)
                                color(Color.BLACK)
                                fontWeightBold()
                            }
                        }
                        
                        Text {
                            attr {
                                text("粉丝")
                                fontSize(12f)
                                color(Color(0xFF666666))
                                marginTop(4f)
                            }
                        }
                        
                        event {
                            click {
                                KLog.i("WaterfallProfilePage", "查看粉丝列表")
                            }
                        }
                    }
                    
                    // 获赞
                    View {
                        attr {
                            allCenter()
                        }
                        
                        Text {
                            attr {
                                text(ctx.likeCount)
                                fontSize(18f)
                                color(Color.BLACK)
                                fontWeightBold()
                            }
                        }
                        
                        Text {
                            attr {
                                text("获赞")
                                fontSize(12f)
                                color(Color(0xFF666666))
                                marginTop(4f)
                            }
                        }
                    }
                }
            }
            
            // 功能菜单区域
            View {
                attr {
                    backgroundColor(Color.WHITE)
                    marginBottom(10f)
                }
            }
            
            // 底部提示
            View {
                attr {
                    backgroundColor(Color.WHITE)
                    padding(20f)
                    allCenter()
                }
                
                Text {
                    attr {
                        text("更多功能正在开发中...")
                        fontSize(14f)
                        color(Color(0xFF999999))
                    }
                }
            }
        }
    }
    
    /**
     * 创建菜单项的辅助函数
     */
    private fun createMenuItem(title: String, icon: String, onClick: () -> Unit): ViewBuilder {
        return {
            View {
                attr {
                    height(56f)
                    flexDirectionRow()
                    alignItemsCenter()
                    padding(20f)
                }
                
                // 图标
                Text {
                    attr {
                        text(icon)
                        fontSize(20f)
                        marginRight(12f)
                    }
                }
                
                // 标题
                Text {
                    attr {
                        text(title)
                        fontSize(16f)
                        color(Color.BLACK)
                        flex(1f)
                    }
                }
                
                // 箭头
                Text {
                    attr {
                        text(">")
                        fontSize(16f)
                        color(Color(0xFF999999))
                    }
                }
                
                event {
                    click {
                        onClick()
                    }
                }
            }
            
            // 分割线
            View {
                attr {
                    height(1f)
                    backgroundColor(Color(0xFFF0F0F0))
                    marginLeft(52f)
                }
            }
        }
    }
}

internal class WaterfallProfilePageAttr : ComposeAttr()

internal class WaterfallProfilePageEvent : ComposeEvent()

// 扩展函数，方便在其他地方使用
internal fun ViewContainer<*, *>.WaterfallProfilePage(init: WaterfallProfilePage.() -> Unit) {
    addChild(WaterfallProfilePage(), init)
}