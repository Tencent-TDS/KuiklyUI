package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.attr.AccessibilityRole
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.GlassEffectContainer
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Input
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

    private var shouldUseGlassEffeect by observable(true)
    private var shouldAnimate by observable(false)
    private var randomTintColor by observable("#FFFFFF")

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
                        margin(10f)
                        allCenter()
                        alignSelfCenter()
                        if (ctx.shouldUseGlassEffeect) {
                            glassEffectIOS() // iOS平台将自动添加液态玻璃效果
                            backgroundColor(Color.TRANSPARENT)
                        } else {
                            backgroundColor(Color.GRAY)
                            glassEffectIOS(enable = false)
                        }
                    }
                    Text {
                        attr {
                            text("液态玻璃效果")
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
                        // borderRadius(10f)
                        allCenter()
                        alignSelfCenter()
                        titleAttr {
                            text("Button with Liquid Glass")
                            fontSize(21f)
                            color(Color(0xFF00CED1))
                        }
                        glassEffectIOS(interactive = true, tintColor = Color(ctx.randomTintColor))
                    }
                    event {
                        click {
                            // 点击后切换随机颜色
                            ctx.randomTintColor = ctx.getRandomColor().toString()
                            ctx.shouldUseGlassEffeect = !ctx.shouldUseGlassEffeect
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
                            flex(1f)
                            glassEffectIOS(tintColor = Color.GREEN)
                        }
                    }
                    View {
                        attr {
                            height(60f)
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
                            marginRight(10f)
                            flex(1f)
                            glassEffectIOS()
                        }
                        Input {
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
                            justifyContentCenter()
                            glassEffectIOS()
                        }
                        Text {
                            attr {
                                text("+")
                                fontSize(21f)
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
                        event {
                            click {
                                ctx.shouldAnimate = !ctx.shouldAnimate
                            }
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
                            flex(1f)
                            interactive(true)
                        }
                    }
                    LiquidGlass {
                        attr {
                            height(60f)
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
                            tintColor(Color.RED)
                            accessibilityRole(AccessibilityRole.BUTTON)
                            size(pagerData.pageViewWidth * 0.7f, 120f)
                            src(ImageUri.commonAssets("panda2.png"))
                        }
                    }
                    Image {
                        attr {
                            resizeStretch()
                            tintColor(Color.GRAY)
                            size(pagerData.pageViewWidth * 0.7f, 120f)
                            src(ImageUri.commonAssets("panda2.png"))
                        }
                    }
                    Image {
                        attr {
                            resizeStretch()
                            size(pagerData.pageViewWidth * 0.7f, 120f)
                            src(ImageUri.commonAssets("penguin2.png"))
                        }
                    }
                }
                Text {
                    attr {
                        text("Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque sagittis urna fringilla diam facilisis rhoncus. Quisque vehicula elit sit amet nisi dictum, ac accumsan mauris sodales. Morbi rhoncus velit orci, vel ultricies lorem tincidunt sed. Nullam elementum odio ut nibh lobortis, eu dignissim urna consectetur. Morbi vestibulum tellus quis elit semper, ac aliquam mauris dictum. Maecenas maximus aliquet convallis. Nulla ut arcu sit amet mi suscipit aliquet auctor et erat. Duis in condimentum libero. In id odio vel leo dictum tempus. Integer odio tortor, dictum nec laoreet vel, volutpat id mi.\n" +
                                "\n" +
                                "Nullam vestibulum consectetur vulputate. Aenean vehicula dolor sit amet erat consectetur dapibus sed in augue. Praesent ante dui, vestibulum vel est ut, tempor bibendum purus. Ut facilisis mi vitae nunc eleifend, vitae vehicula nulla commodo. Suspendisse malesuada metus ut lorem aliquet, sed congue nulla mattis. Maecenas eu arcu diam. Aliquam tempus varius neque et condimentum. Fusce ac tortor vel metus gravida pretium.\n" +
                                "\n" +
                                "In vitae auctor metus, eu rutrum ex. Sed pulvinar facilisis justo a lobortis. Sed imperdiet lobortis mauris, ut sagittis augue mollis non. Cras hendrerit massa magna. Donec cursus tempus neque eget imperdiet. Aliquam iaculis sollicitudin orci id suscipit. Aenean pharetra finibus justo, in imperdiet leo convallis in. Duis mattis euismod mi, ut elementum quam pellentesque at.\n" +
                                "\n" +
                                "Maecenas eros nibh, aliquam nec est iaculis, tincidunt mattis ipsum. Mauris feugiat ut turpis ultricies rutrum. Maecenas ut leo lorem. Nulla vel euismod turpis. Integer rutrum malesuada elit id luctus. Morbi aliquet tempus neque non rutrum. Mauris elit purus, dapibus vel nunc at, tempus tempor quam. Suspendisse quis nunc convallis, egestas ipsum sed, lacinia eros. In pharetra pretium mauris at dapibus. In in felis gravida, blandit magna in, tempus quam. Donec vitae molestie justo. Donec mollis sodales ornare. Donec dictum eu lectus eu porta. Pellentesque fringilla nec sem sed vehicula. Cras accumsan laoreet dui, eu rhoncus urna fermentum in.\n" +
                                "\n" +
                                "Pellentesque sed tempus eros. Aenean vel ligula convallis, semper velit vitae, pellentesque tellus. Donec varius ut mi quis convallis. Donec eget elit in felis blandit tempus. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Suspendisse ultrices volutpat mi, sit amet hendrerit dui mattis vitae. Phasellus vel urna ultrices, congue ex vel, feugiat metus. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Integer imperdiet ornare nisl eget malesuada. Maecenas vel faucibus ligula, at rutrum tellus. Quisque viverra porta semper. Pellentesque scelerisque nunc a felis fringilla, eget placerat dui vulputate. Nunc fermentum lorem a pharetra commodo. Etiam leo ante, euismod id pellentesque nec, iaculis vitae est. Nunc quis volutpat elit.\n" +
                                "\n" +
                                "Pellentesque dignissim laoreet leo, sed tincidunt risus fermentum et. Proin faucibus consequat neque non feugiat. Aenean in laoreet justo. Suspendisse venenatis iaculis libero, ac gravida neque imperdiet eu. Aliquam ut lacinia magna, ac vulputate sapien. In hac habitasse platea dictumst. Cras vehicula justo nec nisi placerat rutrum. Sed felis tortor, molestie a egestas eu, commodo vel ante.\n" +
                                "\n" +
                                "In sed sapien lectus. Nulla id malesuada turpis, nec congue erat. Ut ultricies tincidunt maximus. Nam elementum facilisis suscipit. Vivamus quis egestas nunc. Quisque in libero eu nulla tempus consectetur. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Vestibulum porttitor ac quam eget gravida. Etiam nec dapibus arcu. Morbi eget lacus eu libero dapibus viverra ac id erat. Sed tincidunt erat eget orci rutrum feugiat. Curabitur dictum tellus eu convallis efficitur. Morbi at ornare ipsum. Mauris ut vehicula diam. Phasellus imperdiet aliquam molestie.\n" +
                                "\n" +
                                "Interdum et malesuada fames ac ante ipsum primis in faucibus. Suspendisse et aliquam ipsum. Aliquam vel velit eros. Donec in mollis lacus. Nam viverra risus nulla. Nunc eget nisl at nulla mattis ultrices. Nulla mattis, ligula sit amet accumsan aliquam, ex massa aliquam magna, a pellentesque sem est in massa.\n" +
                                "\n" +
                                "Duis lobortis, quam efficitur maximus pretium, neque lectus finibus tortor, sit amet facilisis odio arcu auctor quam. Cras bibendum porttitor ultrices. Donec tempor egestas lorem, ac laoreet odio commodo quis. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur quis nunc in elit semper posuere eget nec nisi. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean erat erat, sagittis sed mauris sit amet, mollis ullamcorper sem. Curabitur sit amet ex eget orci mollis vehicula. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Aenean tincidunt felis quis luctus consectetur. Quisque commodo vel odio non interdum.\n" +
                                "\n" +
                                "Mauris tincidunt tellus eget commodo sagittis. Etiam sodales odio ut orci molestie rhoncus eget sit amet est. Nunc in hendrerit justo. Nunc consectetur ornare vehicula. Proin nulla ex, pellentesque vel dolor vitae, lacinia mollis nulla. In nisi erat, cursus id elit non, tristique mollis orci. Suspendisse ultrices diam sit amet dignissim accumsan. Aliquam nec efficitur urna, vel feugiat diam. Sed sodales tincidunt augue, et tincidunt justo elementum non. Curabitur ut nibh mauris. Aliquam eleifend tortor sit amet nulla condimentum, vel vehicula est efficitur. Suspendisse posuere posuere lectus vel maximus. Nam risus eros, eleifend varius metus in, tincidunt tincidunt orci. Sed sit amet odio in mi vehicula congue. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; In hac habitasse platea dictumst.\n" +
                                "\n" +
                                "Generated 10 paragraphs, 845 words, 5687 bytes of Lorem Ipsum")
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