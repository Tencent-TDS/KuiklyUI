package com.example.kuiklyworkwithminiapp

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.core.reactive.handler.*
import com.example.kuiklyworkwithminiapp.base.BasePager
import com.example.kuiklyworkwithminiapp.base.bridgeModule

/**
 * 设置页面示例
 * 展示常见的设置页面UI组件使用方式
 */
@Page("settings", supportInLocal = true)
internal class SettingsPage : BasePager() {

    // 设置项的开关状态
    var notificationEnabled by observable(true)
    var darkModeEnabled by observable(false)
    var autoPlayEnabled by observable(true)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color(0xFFF5F5F5))
            }

            // 导航栏
            SettingsNavBar {
                attr {
                    title = "设置"
                    backDisable = false
                }
            }

            List {
                attr {
                    flex(1f)
                }

                // 账号信息区域
                View {
                    attr {
                        backgroundColor(Color.WHITE)
                        margin(top = 10f, left = 15f, right = 15f)
                        borderRadius(10f)
                        padding(15f)
                    }
                    View {
                        attr {
                            flexDirectionRow()
                            alignItemsCenter()
                        }
                        // 头像
                        View {
                            attr {
                                size(60f, 60f)
                                borderRadius(30f)
                                backgroundColor(Color(0xFFAD37FE))
                                allCenter()
                            }
                            Text {
                                attr {
                                    text("K")
                                    fontSize(24f)
                                    color(Color.WHITE)
                                    fontWeightBold()
                                }
                            }
                        }
                        // 用户信息
                        View {
                            attr {
                                marginLeft(15f)
                                flex(1f)
                            }
                            Text {
                                attr {
                                    text("Kuikly用户")
                                    fontSize(18f)
                                    fontWeightSemisolid()
                                    color(Color(0xFF333333))
                                }
                            }
                            Text {
                                attr {
                                    text("kuikly@example.com")
                                    fontSize(14f)
                                    color(Color(0xFF999999))
                                    marginTop(5f)
                                }
                            }
                        }
                        // 箭头
                        Text {
                            attr {
                                text(">")
                                fontSize(20f)
                                color(Color(0xFFCCCCCC))
                            }
                        }
                    }
                    event {
                        click {
                            ctx.bridgeModule.toast("点击了账号信息")
                        }
                    }
                }

                // 通用设置区域
                View {
                    attr {
                        backgroundColor(Color.WHITE)
                        margin(top = 10f, left = 15f, right = 15f)
                        borderRadius(10f)
                    }

                    // 通知设置
                    SettingItem {
                        attr {
                            title = "消息通知"
                            showSwitch = true
                            switchOn = ctx.notificationEnabled
                        }
                        event {
                            onSwitchChanged {
                                ctx.notificationEnabled = it
                                ctx.bridgeModule.toast("消息通知已${if (it) "开启" else "关闭"}")
                            }
                        }
                    }

                    // 分割线
                    View {
                        attr {
                            height(0.5f)
                            backgroundColor(Color(0xFFEEEEEE))
                            marginLeft(15f)
                            marginRight(15f)
                        }
                    }

                    // 深色模式
                    SettingItem {
                        attr {
                            title = "深色模式"
                            showSwitch = true
                            switchOn = ctx.darkModeEnabled
                        }
                        event {
                            onSwitchChanged {
                                ctx.darkModeEnabled = it
                                ctx.bridgeModule.toast("深色模式已${if (it) "开启" else "关闭"}")
                            }
                        }
                    }

                    // 分割线
                    View {
                        attr {
                            height(0.5f)
                            backgroundColor(Color(0xFFEEEEEE))
                            marginLeft(15f)
                            marginRight(15f)
                        }
                    }

                    // 自动播放
                    SettingItem {
                        attr {
                            title = "自动播放视频"
                            showSwitch = true
                            switchOn = ctx.autoPlayEnabled
                        }
                        event {
                            onSwitchChanged {
                                ctx.autoPlayEnabled = it
                                ctx.bridgeModule.toast("自动播放已${if (it) "开启" else "关闭"}")
                            }
                        }
                    }
                }

                // 其他设置区域
                View {
                    attr {
                        backgroundColor(Color.WHITE)
                        margin(top = 10f, left = 15f, right = 15f)
                        borderRadius(10f)
                    }

                    // 清除缓存
                    SettingItem {
                        attr {
                            title = "清除缓存"
                            subtitle = "128.5 MB"
                            showArrow = true
                        }
                        event {
                            onClick {
                                ctx.bridgeModule.toast("清除缓存功能")
                            }
                        }
                    }

                    // 分割线
                    View {
                        attr {
                            height(0.5f)
                            backgroundColor(Color(0xFFEEEEEE))
                            marginLeft(15f)
                            marginRight(15f)
                        }
                    }

                    // 隐私政策
                    SettingItem {
                        attr {
                            title = "隐私政策"
                            showArrow = true
                        }
                        event {
                            onClick {
                                ctx.bridgeModule.toast("查看隐私政策")
                            }
                        }
                    }

                    // 分割线
                    View {
                        attr {
                            height(0.5f)
                            backgroundColor(Color(0xFFEEEEEE))
                            marginLeft(15f)
                            marginRight(15f)
                        }
                    }

                    // 关于我们
                    SettingItem {
                        attr {
                            title = "关于我们"
                            subtitle = "v1.0.0"
                            showArrow = true
                        }
                        event {
                            onClick {
                                ctx.bridgeModule.toast("关于我们")
                            }
                        }
                    }
                }

                // 退出登录按钮
                View {
                    attr {
                        margin(top = 30f, left = 15f, right = 15f, bottom = 30f)
                        height(50f)
                        borderRadius(25f)
                        backgroundColor(Color.WHITE)
                        allCenter()
                        border(Border(1f, BorderStyle.SOLID, Color(0xFFEEEEEE)))
                    }
                    Text {
                        attr {
                            text("退出登录")
                            fontSize(16f)
                            color(Color(0xFFFF3B30))
                        }
                    }
                    event {
                        click {
                            ctx.bridgeModule.toast("退出登录")
                        }
                    }
                }
            }
        }
    }
}

/**
 * 设置页面导航栏组件
 */
internal class SettingsNavBar : ComposeView<SettingsNavBarAttr, ComposeEvent>() {
    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }

    override fun createAttr(): SettingsNavBarAttr {
        return SettingsNavBarAttr()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
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
                            fontSize(17f)
                            fontWeightSemisolid()
                            backgroundLinearGradient(
                                Direction.TO_BOTTOM,
                                ColorStop(Color(0xFF23D3FD), 0f),
                                ColorStop(Color(0xFFAD37FE), 1f)
                            )
                        }
                    }
                }

                // 返回按钮
                if (!ctx.attr.backDisable) {
                    Image {
                        attr {
                            absolutePosition(
                                top = 12f + getPager().pageData.statusBarHeight,
                                left = 12f,
                                bottom = 12f,
                                right = 12f
                            )
                            size(10f, 17f)
                            src("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAASBAMAAAB/WzlGAAAAElBMVEUAAAAAAAAAAAAAAAAAAAAAAADgKxmiAAAABXRSTlMAIN/PELVZAGcAAAAkSURBVAjXYwABQTDJqCQAooSCHUAcVROCHBiFECTMhVoEtRYA6UMHzQlOjQIAAAAASUVORK5CYII=")
                        }
                        event {
                            click {
                                getPager().acquireModule<RouterModule>(RouterModule.MODULE_NAME)
                                    .closePage()
                            }
                        }
                    }
                }
            }
        }
    }
}

internal class SettingsNavBarAttr : ComposeAttr() {
    var title: String by observable("")
    var backDisable = false
}

internal fun ViewContainer<*, *>.SettingsNavBar(init: SettingsNavBar.() -> Unit) {
    addChild(SettingsNavBar(), init)
}

/**
 * 设置项组件
 * 可复用的设置列表项组件
 */
internal class SettingItem : ComposeView<SettingItemAttr, SettingItemEvent>() {
    override fun createEvent(): SettingItemEvent {
        return SettingItemEvent()
    }

    override fun createAttr(): SettingItemAttr {
        return SettingItemAttr()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    height(56f)
                    flexDirectionRow()
                    alignItemsCenter()
                    paddingLeft(15f)
                    paddingRight(15f)
                }
                event {
                    click {
                        ctx.event.onClickHandler?.invoke()
                    }
                }

                // 标题和副标题
                View {
                    attr {
                        flex(1f)
                        justifyContentCenter()
                    }
                    Text {
                        attr {
                            text(ctx.attr.title)
                            fontSize(16f)
                            color(Color(0xFF333333))
                        }
                    }
                    if (ctx.attr.subtitle.isNotEmpty()) {
                        Text {
                            attr {
                                text(ctx.attr.subtitle)
                                fontSize(13f)
                                color(Color(0xFF999999))
                                marginTop(3f)
                            }
                        }
                    }
                }

                // 开关
                if (ctx.attr.showSwitch) {
                    Switch {
                        attr {
                            isOn(ctx.attr.switchOn)
                            onColor(Color(0xFF23D3FD))
                        }
                        event {
                            switchOnChanged {
                                ctx.attr.switchOn = it
                                ctx.event.onSwitchChangedHandler?.invoke(it)
                            }
                        }
                    }
                }

                // 箭头
                if (ctx.attr.showArrow) {
                    Text {
                        attr {
                            text(">")
                            fontSize(20f)
                            color(Color(0xFFCCCCCC))
                        }
                    }
                }
            }
        }
    }
}

/**
 * 设置项属性
 */
internal class SettingItemAttr : ComposeAttr() {
    var title: String by observable("")
    var subtitle: String by observable("")
    var showSwitch: Boolean by observable(false)
    var switchOn: Boolean by observable(false)
    var showArrow: Boolean by observable(false)
}

/**
 * 设置项事件
 */
internal class SettingItemEvent : ComposeEvent() {
    internal var onClickHandler: (() -> Unit)? = null
    internal var onSwitchChangedHandler: ((Boolean) -> Unit)? = null

    fun onClick(handler: () -> Unit) {
        onClickHandler = handler
    }

    fun onSwitchChanged(handler: (Boolean) -> Unit) {
        onSwitchChangedHandler = handler
    }
}

/**
 * 设置项容器扩展函数
 */
internal fun ViewContainer<*, *>.SettingItem(init: SettingItem.() -> Unit) {
    addChild(SettingItem(), init)
}
