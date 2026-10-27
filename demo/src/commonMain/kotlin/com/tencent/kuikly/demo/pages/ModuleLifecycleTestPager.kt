package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * 观测渲染层销毁与页面 pageWillDestroy() 的先后顺序。
 *
 * 点击「关闭页面」按钮后对比 logcat 两个打点：
 * [ModuleLifecycle] pageWillDestroy / [KuiklyRenderCore] renderLayer onDestroy。
 *
 * 只观测顺序、不断言 Module 返回值：部分 Module 无状态或 onDestroy() 幂等，
 * 返回值无法区分「可用」与「已销毁」，用作断言会产生假阳性。
 */
@Page("ModuleLifecycleTestPager")
internal class ModuleLifecycleTestPager : BasePager() {

    private var resultText by observable("点击下方按钮关闭页面，然后从 logcat 观察销毁顺序")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { backgroundColor(Color.WHITE) }
            View {
                attr { margin(16f) }
                Text {
                    attr {
                        text(ctx.resultText)
                        fontSize(15f)
                        color(Color(0xFF333333))
                    }
                }
            }
            // 关闭页面 -> Activity.finish() -> Kuikly onDetach
            Button {
                attr {
                    margin(16f)
                    titleAttr {
                        text("关闭页面并观察 logcat")
                        color(Color.WHITE)
                    }
                    backgroundColor(Color(0xFF007AFF))
                    size(width = 280f, height = 48f)
                    borderRadius(8f)
                }
                event {
                    click {
                        ctx.finishSelf()
                    }
                }
            }
        }
    }

    private fun finishSelf() {
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
    }

    override fun pageWillDestroy() {
        // 与渲染层销毁打点对比时间戳
        KLog.i(TAG, "pageWillDestroy")
        // 异步调用（syncCall=false）会晚于渲染层销毁执行，用于观测该既有行为
        asyncProbeAfterDestroy()
        super.pageWillDestroy()
    }

    private fun asyncProbeAfterDestroy() {
        acquireModule<NotifyModule>(NotifyModule.MODULE_NAME)
            .postNotify(PROBE_EVENT, JSONObject(), false)
    }

    companion object {
        private const val TAG = "ModuleLifecycle"
        private const val PROBE_EVENT = "module_lifecycle_async_probe"
    }
}
