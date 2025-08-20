package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.GlassEffectContainer
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.InputView
import com.tencent.kuikly.core.views.InterfaceStyle
import com.tencent.kuikly.core.views.LiquidGlass
import com.tencent.kuikly.core.views.TabbarIOS
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.TabbarItem
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.core.views.iOSSlider
import com.tencent.kuikly.core.views.iOSSwitch
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
            ctx.testBackground().invoke(this)
            NavBar {
                attr {
                    title = "iOS Liquid Glass Demo"
                }
            }

            View {
                attr {
                    flex(1f)
                    margin(10f)
                }

                View {
                    attr {
                        flexDirectionRow()
                        size(300f, 60f)
                        borderRadius(30f)
                        margin(10f)
                        allCenter()
                        alignSelfCenter()

                        if (ctx.shouldUseGlassEffect) {
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
                                text("液态玻璃效果")
                            } else {
                                text("普通背景效果")
                            }
                            fontSize(21f)
                            alignSelfCenter()
                        }
                    }
                }
                Button {
                    attr {
                        flexDirectionRow()
                        size(300f, 60f)
                        margin(10f)
                        borderRadius(10f)
                        allCenter()
                        alignSelfCenter()
                        titleAttr {
                            text("Kuikly Liquid Glass Button")
                            fontSize(21f)
                        }
                        // 使用Liquid Glass时，不能同时设置背景色，因此要判断平台
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

                // 基础组合效果示例
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

                // 输入框示例
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
                                fontSize(31f)
                                alignSelfCenter()
                            }
                        }
                    }
                }

                // 动画示例
                View {
                    attr {
                        justifyContentCenter()
                        margin(10f)
                        borderRadius(30f)
                        if (ctx.shouldAnimate) {
                            size(200f, 200f)
                            animate(Animation.easeInOut(3f), ctx.shouldAnimate)
                        } else {
                            size(60f,60f)
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
                            fontSize(21f)
                            alignSelfCenter()
                        }
                    }
                }

                vif({ getPager().pageData.isIOS }) {
                    // iOS 平台原生组件示例
                    ctx.iOSSwitchAndSliderDemo().invoke(this)
                    ctx.iOSSystemTabbarDemo().invoke(this)

                    // 使用方式二：组件方式写法示例，注意需判断平台
                    ctx.liquidGlassComponentDemo().invoke(this)
                }
            }

        }
    }

    private fun blurInput() {
        inputRef.view?.blur()
    }

    private fun iOSSystemTabbarDemo(): ViewBuilder {
        return {
            View {
                attr {
                    flexDirectionRow()
                    margin(10f)
                }
                TabbarIOS {
                    attr {
                        height(80f)
                        flex(1f)
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
                    allCenter()
                }
                iOSSwitch {
                    attr {
                        size(100f, 30f)
                        margin(10f)
                        value(true)
                        // alignSelfCenter()
                    }
                }
                iOSSlider {
                    attr {
                        size(200f, 30f)
                        margin(10f)
                        value(0.5f)
                    }
                }
            }
        }
    }

    private fun liquidGlassComponentDemo(): ViewBuilder {
        return {
            View {
                attr {
                    flexDirectionRow()
                    margin(10f)
                }
                LiquidGlass {
                    attr {
                        height(60f)
                        interactive(false)
                        justifyContentCenter()
                    }
                    Text {
                        attr {
                            margin(10f)
                            text("组件写法示例:")
                            fontSize(19f)
                            alignSelfCenter()
                        }
                    }
                }
                GlassEffectContainer {
                    attr {
                        spacing(15f)
                        flex(1f)
                        flexDirectionRow()
                        interfaceStyle(InterfaceStyle.LIGHT)
                    }
                    LiquidGlass {
                        attr {
                            height(60f)
                            borderRadius(30f)
                            flex(1f)
                            interactive(true)
                        }
                        View {
                            attr {
                                height(60f)
                                borderRadius(30f)
                                flex(1f)
                            }
                        }
                    }
                    LiquidGlass {
                        attr {
                            height(60f)
                            borderRadius(30f)
                            flex(1f)
                            interactive(true)
                            tintColor(Color.YELLOW)
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
                    attr {
                        allCenter()
                        margin(20f)
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
                            resizeStretch()
                            tintColor(Color.GRAY)
                            size(pagerData.pageViewWidth, 360f)
                            src(ImageUri.commonAssets("panda2.png"))
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