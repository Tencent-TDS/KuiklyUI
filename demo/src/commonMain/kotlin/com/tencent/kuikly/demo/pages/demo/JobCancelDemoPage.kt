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
package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.coroutines.Job
import com.tencent.kuikly.core.coroutines.Deferred
import com.tencent.kuikly.core.coroutines.async
import com.tencent.kuikly.core.coroutines.delay
import com.tencent.kuikly.core.coroutines.suspendCancellableCoroutine
import com.tencent.kuikly.core.coroutines.launch
import com.tencent.kuikly.core.timer.clearTimeout
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * Job Cancel Demo
 *
 * @author suzhanfeng
 * @date 2025/12/23
 */
@Page("JobCancelDemoPage")
internal class JobCancelDemoPage : BasePager() {
    private var countDownJob: Job? = null
    private var suspendJob: Job? = null
    private var awaitJob: Job? = null
    private var awaitDeferred: Deferred<Int>? = null
    private var remaining by observable(10)
    private var statusText by observable("倒计时任务就绪")
    private var suspendStatusText by observable("延时挂起任务：就绪")
    private var awaitStatusText by observable("等待任务: 就绪")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color(0xFF3c6cbdL))
            }
            // navBar
            NavBar {
                attr {
                    title = "Job Cancel Demo"
                }
            }

            List {
                attr { flex(1f) }

                View {
                    attr {
                        margin(16f)
                        height(80f)
                        backgroundColor(Color.BLACK)
                        allCenter()
                    }
                    Text {
                        attr {
                            fontSize(24f)
                            color(Color.WHITE)
                            text(ctx.remaining.toString())
                        }
                    }
                }

                View {
                    attr {
                        margin(16f)
                        height(40f)
                        backgroundColor(Color(0xFF1F1F1FL))
                        allCenter()
                    }
                    Text {
                        attr {
                            fontSize(16f)
                            color(Color.WHITE)
                            text(ctx.statusText)
                        }
                    }
                }

                View {
                    attr {
                        margin(16f)
                        height(40f)
                        backgroundColor(Color(0xFF1F1F1FL))
                        allCenter()
                    }
                    Text {
                        attr {
                            fontSize(16f)
                            color(Color.WHITE)
                            text(ctx.suspendStatusText)
                        }
                    }
                }

                View {
                    attr {
                        margin(16f)
                        height(40f)
                        backgroundColor(Color(0xFF1F1F1FL))
                        allCenter()
                    }
                    Text {
                        attr {
                            fontSize(16f)
                            color(Color.WHITE)
                            text(ctx.awaitStatusText)
                        }
                    }
                }

                View {
                    attr {
                        margin(16f)
                        height(60f)
                        flexDirectionRow()
                        justifyContentSpaceAround()
                        alignItemsCenter()
                        backgroundColor(Color(0xFF203A8CL))
                    }

                    Text {
                        attr {
                            fontSize(18f)
                            color(Color.WHITE)
                            text("开始")
                        }
                        event {
                            click {
                                if (ctx.countDownJob?.isActive == true) {
                                    return@click
                                }
                                ctx.remaining = 10
                                ctx.statusText = "运行中"
                                ctx.countDownJob = getPager().lifecycleScope.launch {
                                    while (ctx.remaining > 0) {
                                        delay(1000)
                                        ctx.remaining -= 1
                                    }
                                    ctx.statusText = "完成"
                                }
                            }
                        }
                    }

                    Text {
                        attr {
                            fontSize(18f)
                            color(Color.WHITE)
                            text("取消")
                        }
                        event {
                            click {
                                ctx.statusText = "已取消"
                                ctx.countDownJob?.cancel()
                            }
                        }
                    }

                    Text {
                        attr {
                            fontSize(18f)
                            color(Color.WHITE)
                            text("重置")
                        }
                        event {
                            click {
                                ctx.countDownJob?.cancel()
                                ctx.remaining = 10
                                ctx.statusText = "就绪"
                            }
                        }
                    }
                }

                View {
                    attr {
                        margin(16f)
                        height(60f)
                        flexDirectionRow()
                        justifyContentSpaceAround()
                        alignItemsCenter()
                        backgroundColor(Color(0xFF203A8CL))
                    }

                    Text {
                        attr {
                            fontSize(18f)
                            color(Color.WHITE)
                            text("开始挂起")
                        }
                        event {
                            click {
                                if (ctx.suspendJob?.isActive == true) {
                                    return@click
                                }
                                ctx.suspendStatusText = "延时挂起任务：运行中"
                                ctx.suspendJob = getPager().lifecycleScope.launch {
                                    suspendCancellableCoroutine<Unit> { continuation, onCompletion ->
                                        val ref = setTimeout(timeout = 3000) {
                                            continuation.resumeWith(Result.success(Unit))
                                        }
                                        onCompletion { cause ->
                                            if (cause != null) {
                                                clearTimeout(ref)
                                            }
                                        }
                                    }
                                    ctx.suspendStatusText = "延时挂起任务：完成"
                                }
                            }
                        }
                    }

                    Text {
                        attr {
                            fontSize(18f)
                            color(Color.WHITE)
                            text("取消挂起")
                        }
                        event {
                            click {
                                ctx.suspendStatusText = "延时挂起任务：已取消"
                                ctx.suspendJob?.cancel()
                            }
                        }
                    }

                    Text {
                        attr {
                            fontSize(18f)
                            color(Color.WHITE)
                            text("开始等待")
                        }
                        event {
                            click {
                                ctx.awaitStatusText = "等待任务: 运行中"
                                ctx.awaitDeferred = getPager().lifecycleScope.async {
                                    delay(3000)
                                    42
                                }
                                ctx.awaitJob = getPager().lifecycleScope.launch {
                                    val v = ctx.awaitDeferred?.await()
                                    ctx.awaitStatusText = "等待任务: 完成=$v"
                                }
                            }
                        }
                    }

                    Text {
                        attr {
                            fontSize(18f)
                            color(Color.WHITE)
                            text("取消等待")
                        }
                        event {
                            click {
                                ctx.awaitStatusText = "等待任务: 已取消"
                                ctx.awaitJob?.cancel()
                            }
                        }
                    }
                }
            }

        }

    }
}
