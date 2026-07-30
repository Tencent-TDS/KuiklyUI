package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

@Page("PageDataTestPage")
internal class PageDataTestPage : BasePager() {

    private var allPageDataInfo: String by observable("")

    override fun created() {
        super.created()
        val sb = StringBuilder()
        sb.appendLine("===== PageData 解析值 =====")
        sb.appendLine("pageViewWidth: ${pageData.pageViewWidth}")
        sb.appendLine("pageViewHeight: ${pageData.pageViewHeight}")
        sb.appendLine("activityWidth: ${pageData.activityWidth}")
        sb.appendLine("activityHeight: ${pageData.activityHeight}")
        sb.appendLine("deviceWidth: ${pageData.deviceWidth}")
        sb.appendLine("deviceHeight: ${pageData.deviceHeight}")
        sb.appendLine("statusBarHeight: ${pageData.statusBarHeight}")
        sb.appendLine("navigationBarHeight: ${pageData.navigationBarHeight}")
        sb.appendLine("platform: ${pageData.platform}")
        sb.appendLine("appVersion: ${pageData.appVersion}")
        sb.appendLine("nativeBuild: ${pageData.nativeBuild}")
        sb.appendLine("osVersion: ${pageData.osVersion}")
        sb.appendLine("density: ${pageData.density}")
        sb.appendLine("isIOS: ${pageData.isIOS}")
        sb.appendLine("isMacOS: ${pageData.isMacOS}")
        sb.appendLine("isAndroid: ${pageData.isAndroid}")
        sb.appendLine("isOhOs: ${pageData.isOhOs}")
        sb.appendLine("isWeb: ${pageData.isWeb}")
        sb.appendLine("isMiniApp: ${pageData.isMiniApp}")
        sb.appendLine("isIphoneX: ${pageData.isIphoneX}")
        sb.appendLine("isAccessibilityRunning: ${pageData.isAccessibilityRunning}")
        sb.appendLine("safeAreaInsets: ${pageData.safeAreaInsets}")
        sb.appendLine("androidBottomBavBarHeight: ${pageData.androidBottomBavBarHeight}")
        allPageDataInfo = sb.toString()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }

            View {
                attr {
                    backgroundColor(Color(0xFFF5F5F5.toInt()))
                    borderRadius(12f)
                    padding(16f)
                    margin(16f)
                    marginTop(60f)
                }

                Text {
                    attr {
                        fontSize(13f)
                        color(Color(0xFF333333.toInt()))
                        text(ctx.allPageDataInfo)
                    }
                }
            }
        }
    }
}
