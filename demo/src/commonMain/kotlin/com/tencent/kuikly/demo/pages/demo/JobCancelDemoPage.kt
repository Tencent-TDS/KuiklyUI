package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.coroutines.Job
import com.tencent.kuikly.core.coroutines.Deferred
import com.tencent.kuikly.core.coroutines.async
import com.tencent.kuikly.core.coroutines.delay
import com.tencent.kuikly.core.coroutines.suspendCancellableBridge
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
    private var bridgeJob: Job? = null
    private var awaitJob: Job? = null
    private var awaitDeferred: Deferred<Int>? = null
    private var remaining by observable(10)
    private var statusText by observable("就绪")
    private var bridgeStatusText by observable("桥接: 就绪")
    private var awaitStatusText by observable("Await: 就绪")

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
                            text(ctx.bridgeStatusText)
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
                                if (ctx.countDownJob?.isActive == true) return@click
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
                            text("桥接开始")
                        }
                        event {
                            click {
                                ctx.bridgeStatusText = "桥接: 运行中"
                                ctx.bridgeJob = getPager().lifecycleScope.launch {
                                    suspendCancellableBridge<Unit> { cont, onCancel ->
                                        val ref = setTimeout(timeout = 3000) {
                                            cont.resumeWith(Result.success(Unit))
                                        }
                                        onCancel { cause ->
                                            if (cause != null) {
                                                clearTimeout(ref)
                                            }
                                        }
                                    }
                                    ctx.bridgeStatusText = "桥接: 完成"
                                }
                            }
                        }
                    }

                    Text {
                        attr {
                            fontSize(18f)
                            color(Color.WHITE)
                            text("桥接取消")
                        }
                        event {
                            click {
                                ctx.bridgeStatusText = "桥接: 已取消"
                                ctx.bridgeJob?.cancel()
                            }
                        }
                    }

                    Text {
                        attr {
                            fontSize(18f)
                            color(Color.WHITE)
                            text("Await开始")
                        }
                        event {
                            click {
                                ctx.awaitStatusText = "Await: 运行中"
                                ctx.awaitDeferred = getPager().lifecycleScope.async {
                                    delay(3000)
                                    42
                                }
                                ctx.awaitJob = getPager().lifecycleScope.launch {
                                    val v = ctx.awaitDeferred?.await()
                                    ctx.awaitStatusText = "Await: 完成=$v"
                                }
                            }
                        }
                    }

                    Text {
                        attr {
                            fontSize(18f)
                            color(Color.WHITE)
                            text("Await取消")
                        }
                        event {
                            click {
                                ctx.awaitStatusText = "Await: 已取消"
                                ctx.awaitJob?.cancel()
                            }
                        }
                    }
                }
            }

        }

    }
}
