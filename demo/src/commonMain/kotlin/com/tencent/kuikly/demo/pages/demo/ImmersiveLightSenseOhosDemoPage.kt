package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.utils.PlatformUtils
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Switch
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseOhos
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * 鸿蒙沉浸光感（Immersive Light Sense）验证页。
 *
 * 顶部就是一个**标题栏**：容器本身只负责"这块区域要沉浸光感"，里面的内容（返回、标题、操作）
 * 全部由业务自定义，材质也由业务自己设在需要的那几个子视图上。
 *
 * 页面同时给出对照：顶部容器内的圆形/胶囊按钮有材质填充，下方容器外的同款胶囊只有描边、无材质。
 */
@Page("12013")
internal class ImmersiveLightSenseOhosDemoPage : BasePager() {

    /** 是否让顶部标题栏使用原生沉浸光感（关掉即演示降级路径） */
    private var useImmersive by observable(true)

    /** 容器级材质档位（0~4），与标题栏内子视图各自的档位独立 —— 演示「玻璃上的玻璃」 */
    private var containerStyle by observable(2)

    override fun body(): ViewBuilder {
        val ctx = this
        // 状态栏高度：沉浸光感容器占据标题栏的位置，故顶部要留出状态栏
        val statusBar = pagerData.statusBarHeight
        return {
            // 彩色背景：材质需要透过底层内容才看得出折射与模糊
            Image {
                attr {
                    absolutePosition(0f, 0f, 0f, 0f)
                    src("https://sqimg.qq.com/qq_product_operations/kan/images/viola/viola_bg.jpg")
                }
            }

            // 顶部：标题栏区域。
            // 容器本身不提供"要不要光感"的开关——这个选择由业务（或上层组件如 NavigationBar）决定：
            // 用 ImmersiveLightSenseOhos 就是沉浸光感版，用普通 View 就是自绘版，内容完全不用改。
            // 注意：条件渲染必须用 vif 指令。普通 Kotlin if 只在首次构建时求值一次，
            // observable 变化不会让它重新求值，表现为"开关拨了但界面不变"。
            vif({ ctx.useImmersive }) {
                ImmersiveLightSenseOhos {
                    attr {
                        height(statusBar + 56f)
                        padding(top = statusBar, left = 12f, right = 12f)
                        flexDirectionRow()
                        alignItemsCenter()
                        backgroundColor(Color.TRANSPARENT)
                        // 容器级材质：整个标题栏区域的"玻璃底"。
                        // 档位由下方按钮实时切换（0~4 全套）；
                        // 子视图（左右胶囊）各自设 REGULAR 档，形成"玻璃上的玻璃"对照
                        immersiveMaterial(style = ctx.containerStyle, interactive = true)
                    }
                    ctx.titleBarContent()()
                }
            }
            vif({ !ctx.useImmersive }) {
                View {
                    attr {
                        height(statusBar + 56f)
                        padding(top = statusBar, left = 12f, right = 12f)
                        flexDirectionRow()
                        alignItemsCenter()
                        backgroundColor(Color.TRANSPARENT)
                    }
                    ctx.titleBarContent()()
                }
            }

            Scroller {
                attr {
                    flex(1f)
                    margin(15f)
                }

                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        padding(12f)
                        borderRadius(16f)
                        backgroundColor(Color(0x66000000))
                        margin(10f)
                    }
                    Text {
                        attr {
                            text("使用沉浸光感标题栏")
                            fontSize(16f)
                            color(Color.WHITE)
                            flex(1f)
                        }
                    }
                    Switch {
                        attr {
                            isOn(ctx.useImmersive)
                        }
                        event {
                            switchOnChanged { isOn ->
                                ctx.useImmersive = isOn
                            }
                        }
                    }
                }

                // 能力探测结果，用于验证 PlatformUtils.isImmersiveMaterialSupported() 通路
                Text {
                    attr {
                        margin(10f)
                        text(
                            "设备支持沉浸光感: ${PlatformUtils.isImmersiveMaterialSupported()}" +
                                "    容器当前使用原生标题栏: ${ctx.useImmersive}"
                        )
                        fontSize(13f)
                        color(Color(0xFFEEEEEE))
                    }
                }

                // 容器材质档位矩阵：0~4 全套实时切换，标题栏玻璃底即时跟随
                ctx.panel("容器材质档位（0~4 实时切换，当前：${ctx.containerStyle}）") {
                    ctx.styleButton("0 · ULTRA_THIN", 0)()
                    ctx.styleButton("1 · THIN", 1)()
                    ctx.styleButton("2 · REGULAR", 2)()
                    ctx.styleButton("3 · THICK", 3)()
                    ctx.styleButton("4 · ULTRA_THICK", 4)()
                }()

                ctx.panel("「玻璃上的玻璃」：子视图覆盖") {
                    Text {
                        attr {
                            margin(top = 4f)
                            text("标题栏内左右两个胶囊各自设了 REGULAR 档材质 —— 容器换档时，" +
                                "胶囊档位不变，可观察子视图独立于容器级材质的效果叠加")
                            fontSize(12f)
                            color(Color(0x99EEEEEE))
                        }
                    }
                }()

                ctx.panel("定论：自绘按钮 = 官方 Button（无需原生组件）") {
                    Text {
                        attr {
                            margin(top = 4f)
                            text("官方 Button 仅在标题栏/TabBar 区域生效；本页标题栏内的胶囊是" +
                                "Kuikly 自绘节点 + immersiveMaterial()，落在生效区域内即获得材质" +
                                "（CAPI 节点 + NODE_SYSTEM_MATERIAL，日志已证 ret=0）。" +
                                "容器 + 自绘按钮即 Button 的等价用法，无需额外原生组件")
                            fontSize(12f)
                            color(Color(0x99EEEEEE))
                        }
                    }
                }()

                // 对照组：同样的胶囊 + 同样的材质，但不在沉浸光感容器的生效区域内
                ctx.panel("容器外（对照组，预期无材质）") {
                    View {
                        attr {
                            height(96f)
                            borderRadius(16f)
                            allCenter()
                            backgroundColor(Color.TRANSPARENT)
                        }
                        ctx.materialCapsule("容器外的子节点")()
                    }
                }()
            }
        }
    }

    private fun panel(title: String, content: ViewBuilder): ViewBuilder {
        return {
            View {
                attr {
                    margin(10f)
                    padding(12f)
                    borderRadius(20f)
                    backgroundColor(Color(0x4D000000))
                }
                Text {
                    attr {
                        text(title)
                        fontSize(14f)
                        color(Color(0xFFEEEEEE))
                    }
                }
                content.invoke(this)
            }
        }
    }

    /** 档位切换小按钮（平铺，自带间距） */
    private fun styleButton(label: String, style: Int): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    margin(left = 10f, right = 10f, top = 6f, bottom = 6f)
                    alignSelfFlexStart()
                    padding(left = 10f, right = 10f, top = 6f, bottom = 6f)
                    borderRadius(12f)
                    backgroundColor(
                        if (ctx.containerStyle == style) Color(0x66007DFF) else Color(0x66000000)
                    )
                    allCenter()
                }
                event {
                    click {
                        ctx.containerStyle = style
                    }
                }
                Text {
                    attr {
                        text(label)
                        fontSize(12f)
                        color(Color.WHITE)
                    }
                }
            }
        }
    }

    /**
     * 标题栏内容：返回 + 标题 + 操作。这就是"业务自定义内容"——
     * 无论外层是沉浸光感容器还是普通容器，这份内容都能原样复用，不用改一个字。
     */
    private fun titleBarContent(): ViewBuilder {
        val ctx = this
        return {
            // 左：返回
            ctx.glassPill(width = 36f, height = 36f) {
                Text {
                    attr {
                        text("←")
                        fontSize(18f)
                        color(Color.WHITE)
                    }
                }
                event {
                    click {
                        getPager().acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                    }
                }
            }()

            // 中：标题
            View {
                attr {
                    flex(1f)
                    allCenter()
                }
                Text {
                    attr {
                        text("沉浸光感标题栏")
                        fontSize(17f)
                        color(Color.WHITE)
                    }
                }
            }

            // 右：操作
            ctx.glassPill(width = 64f, height = 36f) {
                Text {
                    attr {
                        text("完成")
                        fontSize(14f)
                        color(Color.WHITE)
                    }
                }
            }()
        }
    }

    /**
     * 标题栏内的圆形/胶囊按钮：透明背景 + 沉浸光感材质。
     * 这是"业务自定义内容"的示例——容器本身不管内容长什么样，只提供生效区域。
     */
    private fun glassPill(width: Float, height: Float, content: ViewBuilder): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    width(width)
                    height(height)
                    borderRadius(height / 2f)
                    allCenter()
                    backgroundColor(Color.TRANSPARENT)
                    if (ctx.useImmersive) {
                        // style: 0=ULTRA_THIN 1=THIN 2=REGULAR 3=THICK 4=ULTRA_THICK
                        immersiveMaterial(style = 2, interactive = true)
                    }
                }
                content.invoke(this)
            }
        }
    }

    /**
     * 对照组胶囊：外观与标题栏内的一致，材质恒定开启。
     * 因为它不在沉浸光感容器的生效区域内，预期不会渲染材质。
     */
    private fun materialCapsule(label: String): ViewBuilder {
        return {
            View {
                attr {
                    width(260f)
                    height(56f)
                    borderRadius(28f)
                    allCenter()
                    alignSelfCenter()
                    backgroundColor(Color.TRANSPARENT)
                    border(
                        border = Border(
                            color = Color(0x33FFFFFF),
                            lineWidth = 2f,
                            lineStyle = BorderStyle.SOLID
                        )
                    )
                    // 与标题栏内的胶囊同档，保证对照组只差"是否在生效区域内"这一个变量
                    immersiveMaterial(style = 2, interactive = true)
                }
                Text {
                    attr {
                        text(label)
                        fontSize(14f)
                        color(Color.WHITE)
                    }
                }
            }
        }
    }
}
