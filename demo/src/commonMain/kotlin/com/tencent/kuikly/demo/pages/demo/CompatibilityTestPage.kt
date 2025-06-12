package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ColorStop
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.Direction
import com.tencent.kuikly.core.base.IPagerId
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.ClickParams
import com.tencent.kuikly.core.base.pagerId
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * This Page uses **deprecated** APIs for compatibility testing.
 * **DO NOT** use these APIs in production code.
 */
@Page("compat")
internal class CompatibilityTestPage : BasePager() {

    private var testObservable by com.tencent.kuikly.core.reactive.handler.observable(0)
    private val testObservableList by com.tencent.kuikly.core.reactive.handler.observableList<Int>()
    private val testObservableSet by com.tencent.kuikly.core.reactive.handler.observableSet<Int>()
    private var output by observable("output...")

    companion object {
        private val IPagerId.testPagerId: String by pagerId {
            "PagerId: $it"
        }
    }
    
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "CompatibilityTestPage"
                }
            }
            Text {
                attr {
                    text("BridgeManager.currentPageId = ${BridgeManager.currentPageId}")
                }
            }
            Text {
                attr {
                    text("observable = ${ctx.testObservable}")
                }
            }
            Text {
                attr {
                    text("observableList.size = ${ctx.testObservableList.size}")
                }
            }
            List {
                attr {
                    flexDirectionRow()
                    height(30f)
                }
                vfor({ ctx.testObservableList }) { item ->
                    Text {
                        attr {
                            text(item.toString())
                            margin(5f)
                        }
                    }
                }
            }
            Text {
                attr {
                    text("observableSet.size = ${ctx.testObservableSet.size}")
                }
            }
            vbind({ ctx.testObservableSet.size }) {
                List {
                    attr {
                        flexDirectionRow()
                        height(30f)
                    }
                    ctx.testObservableSet.forEach {
                        Text {
                            attr {
                                text(it.toString())
                                margin(5f)
                            }
                        }
                    }
                }
            }
            View {
                attr {
                    border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                    margin(5f)
                    padding(5f)
                }
                Text {
                    attr {
                        text(ctx.output)
                        height(100f)
                    }
                }
            }
            View {
                attr {
                    flexDirectionRow()
                    flexWrapWrap()
                }
                TestCase("pagerId") {
                    ctx.output = ctx.testPagerId
                }
                TestCase("observable") {
                    ctx.output = "click observable"
                    ctx.testObservable++
                }
                TestCase("observableList") {
                    ctx.output = "click observableList"
                    ctx.testObservableList.add(ctx.testObservableList.size)
                }
                TestCase("observableSet") {
                    ctx.output = "click observableSet"
                    ctx.testObservableSet.add(ctx.testObservableSet.size)
                }

                TestCase("postDelay") {
                    ctx.output = "click postDelay"
                    com.tencent.kuikly.core.timer.postDelayed(1000) {
                        ctx.output = "postDelay fired after 1s"
                    }
                }
                TestCase("cancelPostCallback") {
                    ctx.output = "click cancelPostCallback"
                    val ref = setTimeout(1000) {
                        ctx.output = "postDelay should not fired"
                    }
                    com.tencent.kuikly.core.timer.cancelPostCallback(ref)
                }
                TestCase("setTimeout(pagerId, callback, timeout)") {
                    ctx.output = "click setTimeout"
                    com.tencent.kuikly.core.timer.setTimeout(ctx.pagerId, {
                        ctx.output = "setTimeout fired after 1s"
                    }, 1000)
                }
                TestCase("setTimeout(callback, timeout)") {
                    ctx.output = "click setTimeout"
                    com.tencent.kuikly.core.timer.setTimeout({
                        ctx.output = "setTimeout fired after 1s"
                    }, 1000)
                }
                TestCase("setTimeout(timeout, callback)") {
                    ctx.output = "click setTimeout"
                    com.tencent.kuikly.core.timer.setTimeout(1000) {
                        ctx.output = "setTimeout fired after 1s"
                    }
                }
                TestCase("clearTimeout") {
                    ctx.output = "click clearTimeout"
                    val ref = setTimeout(1000) {
                        ctx.output = "postDelay should not fired"
                    }
                    com.tencent.kuikly.core.timer.clearTimeout(ref)
                }
            }
        }
    }
}

internal class TestCaseView(
    private val title: String,
    private val click: (ClickParams) -> Unit
): ComposeView<ComposeAttr, ComposeEvent>() {

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }

    override fun createAttr(): ComposeAttr {
        return ComposeAttr()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Button {
                attr {
                    height(30f)
                    borderRadius(20f)
                    margin(5f)
                    padding(left = 15f, right = 15f)
                    backgroundLinearGradient(
                        Direction.TO_BOTTOM,
                        ColorStop(Color(0xAA23D3FD), 0f),
                        ColorStop(Color(0xAAAD37FE), 1f)
                    )

                    titleAttr {
                        text(ctx.title)
                        fontSize(17f)
                        color(Color.WHITE)
                    }
                }
                event {
                    click(ctx.click)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.TestCase(title: String, click: (ClickParams) -> Unit) {
    addChild(TestCaseView(title, click)) {}
}