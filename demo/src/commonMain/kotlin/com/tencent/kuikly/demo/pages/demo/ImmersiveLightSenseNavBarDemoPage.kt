package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.utils.PlatformUtils
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * 「无感使用」验证页：用的就是原来的 NavBar，只多开了一个开关。
 *
 * 标题栏的内容（返回箭头 + 居中标题）由 NavigationBar 自己搬进沉浸光感容器，
 * 业务不需要知道容器的存在、也不需要自己往容器里塞内容。
 */
@Page("ImmersiveLightSenseNavBarDemoPage")
internal class ImmersiveLightSenseNavBarDemoPage : BasePager() {

    override fun body(): ViewBuilder {
        return {
            // 彩色背景：材质需要透过底层内容才看得出折射与模糊
            Image {
                attr {
                    absolutePosition(0f, 0f, 0f, 0f)
                    src("https://sqimg.qq.com/qq_product_operations/kan/images/viola/viola_bg.jpg")
                }
            }

            // 业务唯一需要写的差别：immersiveLightSense = true
            NavBar {
                attr {
                    title = "沉浸光感 NavigationBar"
                    immersiveLightSense = true
                    // 本页用 ULTRA_THICK(4)，与裸容器版的 REGULAR(2) 形成档位对照
                    immersiveMaterialStyle = 4
                }
            }

            Scroller {
                attr {
                    flex(1f)
                    margin(15f)
                }
                Text {
                    attr {
                        margin(10f)
                        text("设备支持沉浸光感: ${PlatformUtils.isImmersiveMaterialSupported()}")
                        fontSize(13f)
                        color(Color(0xFFEEEEEE))
                    }
                }
                Text {
                    attr {
                        margin(10f)
                        text("这个标题栏就是原来的 NavBar，只是打开了 immersiveLightSense 开关；" +
                            "返回箭头与标题由组件自己搬进沉浸光感容器，业务无需关心。")
                        fontSize(13f)
                        color(Color(0xFFEEEEEE))
                    }
                }
            }
        }
    }
}
