package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.InterfaceStyle
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.utils.PlatformUtils
import com.tencent.kuikly.core.views.GlassEffectContainer
import com.tencent.kuikly.core.views.GlassEffectStyle
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.InputView
import com.tencent.kuikly.core.views.LiquidGlass
import com.tencent.kuikly.core.views.ios.TabbarIOS
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.ios.TabbarItem
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.core.views.ios.iOSSlider
import com.tencent.kuikly.core.views.ios.iOSSwitch
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.random.Random

@Page("LiquidGlassDemoPage")
internal class LiquidGlassDemoPage : BasePager() {

    private var shouldUseGlassEffect by observable(true)
    private var shouldAnimate by observable(false)
    private var randomTintColor by observable(0xFF90EE90.toString())
    private lateinit var inputRef: ViewRef<InputView>

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            ctx.testBackground()()
            NavBar {
                attr {
                    title = "iOS Liquid Glass Demo"
                }
            }

            Scroller {
                attr {
                    flex(1f)
                    margin(15f)
                }

                // 基础效果演示分组
                ctx.createDemoSection("基础效果演示") {
                    View {
                        attr {
                            size(300f, 60f)
                            borderRadius(30f)
                            margin(10f)
                            allCenter()
                            alignSelfCenter()

                            if (PlatformUtils.isLiquidGlassSupported() && ctx.shouldUseGlassEffect) {
                                glassEffectIOS() // iOS平台将自动添加液态玻璃效果
                                backgroundColor(Color.TRANSPARENT)
                            } else {
                                backgroundColor(Color.GRAY)
                                glassEffectIOS(enable = false)
                            }
                        }
                        event {
                            click {
                                ctx.shouldUseGlassEffect = !ctx.shouldUseGlassEffect
                            }
                        }
                        Text {
                            attr {
                                if (ctx.shouldUseGlassEffect) {
                                    text("液态玻璃-Regular Style")
                                } else {
                                    text("普通背景效果")
                                }
                                fontSize(21f)
                                alignSelfCenter()
                                fontWeight600()
                            }
                        }
                    }
                    // Clear效果演示分组
                    View {
                        attr {
                            size(300f, 60f)
                            borderRadius(30f)
                            margin(10f)
                            allCenter()
                            alignSelfCenter()
                            glassEffectIOS(style = GlassEffectStyle.CLEAR)
                        }
                        Text {
                            attr {
                                text("液态玻璃-Clear Style")
                                fontSize(21f)
                                alignSelfCenter()
                                fontWeight600()
                            }
                        }
                    }
                    Button {
                        attr {
                            flexDirectionRow()
                            size(300f, 60f)
                            margin(10f)
                            borderRadius(15f)
                            allCenter()
                            alignSelfCenter()
                            titleAttr {
                                text("Kuikly Liquid Glass Button")
                                fontSize(21f)
                                fontWeight600()
                            }
                            // 使用Liquid Glass时，不能同时设置背景色
                            glassEffectIOS(interactive = true, tintColor = Color(ctx.randomTintColor))
                            if (!getPager().pageData.isIOS) {
                                backgroundColor(Color(ctx.randomTintColor))
                            }
                        }
                        event {
                            click {
                                // 点击后切换随机颜色
                                ctx.randomTintColor = ctx.getRandomColor().toString()
                            }
                        }
                    }
                }()

                // 组合效果演示分组
                ctx.createDemoSection("组合效果演示") {
                    View {
                        attr {
                            flexDirectionRow()
                            margin(10f)
                            glassEffectContainerIOS(10f)
                        }
                        View {
                            attr {
                                height(60f)
                                borderRadius(30f)
                                flex(2f)
                                justifyContentCenter()
                                glassEffectIOS(interactive = false)
                            }
                            Text {
                                attr {
                                    margin(10f)
                                    text("融合效果：")
                                    fontSize(19f)
                                    alignSelfCenter()
                                    fontWeight500()
                                }
                            }
                        }
                        View {
                            attr {
                                height(60f)
                                borderRadius(30f)
                                flex(1f)
                                glassEffectIOS(tintColor = Color.GREEN)
                            }
                        }
                        View {
                            attr {
                                height(60f)
                                borderRadius(30f)
                                flex(1f)
                                glassEffectIOS()
                            }
                        }
                    }
                }()

                // 交互组件演示分组
                ctx.createDemoSection("交互组件演示") {
                    View {
                        attr {
                            flexDirectionRow()
                            margin(10f)
                            glassEffectContainerIOS(10f)
                        }
                        View {
                            attr {
                                height(60f)
                                borderRadius(30f)
                                marginRight(10f)
                                flex(1f)
                                glassEffectIOS()
                            }
                            Input {
                                ref {
                                    ctx.inputRef = it
                                }
                                attr {
                                    marginLeft(30f)
                                    flex(1f)
                                    placeholder("Type to Search")
                                    fontSize(16f)
                                }
                            }
                        }
                        View {
                            attr {
                                size(60f, 60f)
                                borderRadius(30f)
                                justifyContentCenter()
                                glassEffectIOS()
                            }
                            event {
                                click {
                                    ctx.blurInput()
                                }
                            }
                            Text {
                                attr {
                                    text("×")
                                    fontSize(28f)
                                    alignSelfCenter()
                                    fontWeight400()
                                }
                            }
                        }
                    }
                }()

                // 动画效果演示分组
                ctx.createDemoSection("动画效果演示") {
                    View {
                        attr {
                            justifyContentCenter()
                            margin(10f)
                            borderRadius(30f)
                            alignSelfCenter()
                            if (ctx.shouldAnimate) {
                                size(200f, 200f)
                                animate(Animation.easeInOut(3f), ctx.shouldAnimate)
                            } else {
                                size(80f, 80f)
                                animate(Animation.easeInOut(3f), ctx.shouldAnimate)
                            }
                            glassEffectIOS()
                        }
                        event {
                            click {
                                ctx.shouldAnimate = !ctx.shouldAnimate
                            }
                        }
                        Text {
                            attr {
                                if (ctx.shouldAnimate) {
                                    text("点击收起")
                                } else {
                                    text("点击展开")
                                }
                                fontSize(18f)
                                alignSelfCenter()
                                fontWeight500()
                            }
                        }
                    }
                }()

                vif({ getPager().pageData.isIOS }) {
                    // iOS 平台原生组件演示分组
                    ctx.createDemoSection("iOS 原生组件") {
                        ctx.iOSSwitchAndSliderDemo()()
                        ctx.iOSSystemTabbarDemo()()
                    }()

                    // 组件方式写法演示分组
                    ctx.createDemoSection("独立组件方式使用示例") {
                        ctx.liquidGlassComponentDemo()()
                    }()
                }
            }
        }
    }

    private fun blurInput() {
        inputRef.view?.blur()
    }

    private fun createDemoSection(title: String, content: ViewBuilder): ViewBuilder {
        return {
            View {
                attr {
                    margin(15f, 10f, 15f, 10f)
                    padding(20f)
                    borderRadius(20f)
                    border(border = Border(
                        color = Color(0xFFE5E5E5),
                        lineWidth = 1f,
                        lineStyle = BorderStyle.SOLID)
                    )
                }
                
                // 分组标题
                Text {
                    attr {
                        text(title)
                        fontSize(24f)
                        fontWeight600()
                        color(Color(0xFF333333))
                        backgroundColor(Color(0xFFF8F9FA))
                        marginBottom(15f)
                        alignSelfFlexStart()

                    }
                }
                
                // 分隔线
                View {
                    attr {
                        height(2f)
                        backgroundColor(Color(0xFFE8E8E8))
                        marginBottom(20f)
                        borderRadius(1f)
                    }
                }
                
                // 内容区域
                content()
            }
        }
    }

    private fun iOSSystemTabbarDemo(): ViewBuilder {
        return {
            View {
                attr {
                    margin(10f)
                    padding(15f)
                    borderRadius(15f)
                    backgroundColor(Color(0xFFF8F9FA))
                }

                Text {
                    attr {
                        text("系统风格 Tabbar 组件")
                        fontSize(18f)
                        fontWeight500()
                        color(Color(0xFF495057))
                        marginBottom(15f)
                        alignSelfCenter()
                    }
                }
                
                TabbarIOS {
                    attr {
                        height(80f)
                        items(
                            listOf(
                                TabbarItem("首页", "home_icon", "home_icon_selected"),
                                TabbarItem("我的", "me_icon", "me_icon_selected")
                            )
                        )
                        selectedIndex(0)
                    }
                    event {
                        onTabSelected {
                            // 处理 tab 切换
                        }
                    }
                }
            }
        }
    }

    private fun iOSSwitchAndSliderDemo(): ViewBuilder {
        return {
            View {
                attr {
                    margin(10f)
                }
                
                // Switch 演示
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        marginBottom(20f)
                        padding(15f)
                        borderRadius(15f)
                        backgroundColor(Color(0xFFF8F9FA))
                        border(border = Border(
                            color = Color(0xFFE9ECEF),
                            lineWidth = 1f,
                            lineStyle = BorderStyle.SOLID)
                        )
                    }
                    Text {
                        attr {
                            text("iOS Switch:")
                            fontSize(18f)
                            fontWeight500()
                            color(Color(0xFF495057))
                            flex(1f)
                        }
                    }
                    iOSSwitch {
                        attr {
                            size(100f, 30f)
                            value(true)
                        }
                    }
                }
                
                // Slider 演示
                View {
                    attr {
                        padding(15f)
                        borderRadius(15f)
                        backgroundColor(Color(0xFFF8F9FA))
                        border(border = Border(
                            color = Color(0xFFE9ECEF),
                            lineWidth = 1f,
                            lineStyle = BorderStyle.SOLID)
                        )
                    }
                    Text {
                        attr {
                            text("iOS Slider:")
                            fontSize(18f)
                            fontWeight500()
                            color(Color(0xFF495057))
                            marginBottom(15f)
                        }
                    }
                    iOSSlider {
                        attr {
                            width(280f)
                            height(30f)
                            value(0.5f)
                            alignSelfCenter()
                        }
                    }
                }
            }
        }
    }

    private fun liquidGlassComponentDemo(): ViewBuilder {
        return {
            View {
                attr {
                    margin(10f)
                }

                // Regular style
                LiquidGlass {
                    attr {
                        height(60f)
                        justifyContentCenter()
                        marginBottom(15f)
                        borderRadius(30f)
                        glassEffectInteractive(false)
                    }
                    Text {
                        attr {
                            text("Regular Style")
                            fontSize(20f)
                            alignSelfCenter()
                            fontWeight600()
                        }
                    }
                }
                
                // Clear style
                LiquidGlass {
                    attr {
                        height(60f)
                        justifyContentCenter()
                        marginBottom(15f)
                        borderRadius(30f)
                        glassEffectInteractive(false)
                        glassEffectStyle(GlassEffectStyle.CLEAR)
                    }
                    Text {
                        attr {
                            text("Clear Style")
                            fontSize(20f)
                            alignSelfCenter()
                            fontWeight600()
                        }
                    }
                }
                
                // 组合效果容器
                GlassEffectContainer {
                    attr {
                        spacing(15f)
                        flexDirectionRow()
                        interfaceStyle(InterfaceStyle.LIGHT)
                    }
                    LiquidGlass {
                        attr {
                            height(60f)
                            borderRadius(30f)
                            flex(1f)
                            justifyContentCenter()
                            glassEffectInteractive(true)
                        }
                        Text {
                            attr {
                                text("默认")
                                fontSize(16f)
                                alignSelfCenter()
                                fontWeight500()
                            }
                        }
                    }
                    LiquidGlass {
                        attr {
                            height(60f)
                            borderRadius(30f)
                            flex(1f)
                            justifyContentCenter()
                            glassEffectInteractive(true)
                            glassEffectTintColor(Color.YELLOW)
                        }
                        Text {
                            attr {
                                text("黄色")
                                fontSize(16f)
                                alignSelfCenter()
                                fontWeight500()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun testBackground(): ViewBuilder {
        return {
            Scroller {
                attr {
                    absolutePosition(0f, 0f, 0f, 0f)
                }
                View {
                    View {
                        attr {
                            height(100f)
                        }
                    }
                    Image {
                        attr {
                            resizeStretch()
                            size(pagerData.pageViewWidth, 360f)
                            src(ImageUri.commonAssets("penguin2.png"))
                        }
                    }
                    Image {
                        attr {
                            resizeCover()
                            size(pagerData.pageViewWidth, 800f)
                            src(ImageUri.commonAssets("views.png"))
                        }
                    }
                    Image {
                        attr {
                            resizeCover()
                            size(pagerData.pageViewWidth, 800f)
                            src(ImageUri.commonAssets("cat1.png"))
                        }
                    }
                    Image {
                        attr {
                            resizeCover()
                            size(pagerData.pageViewWidth, 800f)
                            src(ImageUri.commonAssets("cat2.png"))
                        }
                    }
                }
            }
        }
    }

    private fun getRandomColor(): Color {
        val red = Random.nextInt(0, 256)
        val green = Random.nextInt(0, 256)
        val blue = Random.nextInt(0, 256)
        return Color(red, green, blue, 1f)
    }

}