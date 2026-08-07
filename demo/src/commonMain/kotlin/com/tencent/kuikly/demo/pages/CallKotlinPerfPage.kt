/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.base.CallKotlinPerfTestModule
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * callKotlin interop performance page — layout + Fire/Update/Callback JSON bridge.
 *
 * Launch: open page `CallKotlinPerfPage` via demo router (Debug build).
 * Logs tag: CallKotlinPerf
 */
@Page("CallKotlinPerfPage")
internal class CallKotlinPerfPage : BasePager() {

    private var statusLine by observable("waiting...")
    private var resultLine by observable("")
    private var ran = false

    override fun createExternalModules(): Map<String, Module>? {
        val modules = super.createExternalModules()?.toMutableMap() ?: hashMapOf()
        modules[CallKotlinPerfTestModule.MODULE_NAME] = CallKotlinPerfTestModule()
        return modules
    }

    override fun pageDidAppear() {
        super.pageDidAppear()
        if (ran) {
            return
        }
        ran = true
        setTimeout(400) { runBenchmark() }
    }

    private fun runBenchmark() {
        val mod = acquireModule<CallKotlinPerfTestModule>(CallKotlinPerfTestModule.MODULE_NAME)
        try {
            statusLine = "nested lifecycle..."
            val nested = mod.testNestedLifecycle()
            KLog.i(TAG, "NESTED_LIFECYCLE $nested")
            if (nested.contains("\"ok\":false")) {
                KLog.i(TAG, "nested lifecycle failed (continue bench): $nested")
            }

            statusLine = "bench basic..."
            val basic = mod.bench(5000)
            KLog.i(TAG, "BASIC $basic")

            statusLine = "phases layout..."
            val layoutCpp = mod.benchPhases(iterations = 3000, jsonBytes = 64, mode = "layout")
            KLog.i(TAG, "PHASES_CPP $layoutCpp")

            // FireViewEvent + UpdateInstance (+ same JSON bridge as Create) + FireCallback
            val pathModes = listOf(
                "fire" to "map",
                "update" to "update_map",
                "callback" to "callback_map",
            )
            for (bytes in listOf(1024, 65536, 3 * 1024 * 1024)) {
                val iters = when {
                    bytes >= 3 * 1024 * 1024 -> 50
                    bytes >= 65536 -> 200
                    else -> 1000
                }
                for ((stringMode, lazyMode) in pathModes) {
                    statusLine = "$stringMode string $bytes..."
                    val cpp = mod.benchPhases(iterations = iters, jsonBytes = bytes, mode = stringMode)
                    KLog.i(TAG, "PHASES_CPP path=$stringMode jsonBytes=$bytes $cpp")

                    statusLine = "$lazyMode lazy $bytes..."
                    val cppMap = mod.benchPhases(iterations = iters, jsonBytes = bytes, mode = lazyMode)
                    KLog.i(TAG, "PHASES_CPP path=$lazyMode jsonBytes=$bytes $cppMap")
                }
            }

            resultLine = "done — see log tag CallKotlinPerf"
            statusLine = "done"
            KLog.i(TAG, "DONE")
        } catch (t: Throwable) {
            val msg = "bench failed: ${t.message}"
            KLog.e(TAG, msg)
            statusLine = msg
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "callKotlin Perf"
                }
            }
            View {
                attr {
                    padding(12f)
                    flex(1f)
                }
                Text {
                    attr {
                        fontSize(16f)
                        color(Color.BLACK)
                        text("status: ${ctx.statusLine}")
                    }
                }
                Text {
                    attr {
                        marginTop(12f)
                        fontSize(13f)
                        color(Color(0xFF333333))
                        text(ctx.resultLine)
                    }
                }
                View {
                    attr {
                        marginTop(20f)
                        height(44f)
                        backgroundColor(Color(0xFF1976D2))
                        allCenter()
                    }
                    event {
                        click {
                            ctx.ran = false
                            ctx.runBenchmark()
                        }
                    }
                    Text {
                        attr {
                            color(Color.WHITE)
                            fontSize(16f)
                            text("Re-run")
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "CallKotlinPerf"
    }
}
