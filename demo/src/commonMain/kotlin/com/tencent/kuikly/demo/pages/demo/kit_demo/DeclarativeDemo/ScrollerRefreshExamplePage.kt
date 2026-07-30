package com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo

import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.Refresh
import com.tencent.kuikly.core.views.RefreshView
import com.tencent.kuikly.core.views.RefreshViewState
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import kotlin.math.max
import kotlin.random.Random


internal data class ScrollerItem(
    var title: String
)

@Page("AnyRefreshExamplePage")
internal class AnyRefreshExamplePage : BasePager() {

    private val randomHelper = Random.Default

    private val items by observableList<ScrollerItem>()

    private var refreshText by observable("")

    /**
     * 刷新次数
     */
    private var refreshCount: Int = 0


    private var refreshRef: ViewRef<RefreshView>? = null

    override fun pageDidAppear() {
        super.pageDidAppear()
        val items = mutableListOf<ScrollerItem>()
        for (i in 0..50) {
            items.add(ScrollerItem("AnyRefreshExamplePage,refreshCount:$refreshCount index:$i"))
        }
        this.items.addAll(items)
    }
    
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            Scroller {
                attr {
                    flex(1f)
                    margin(left = 16f, top = ctx.pageData.statusBarHeight, right = 16f, bottom = 4f)
                    showScrollerIndicator(true)
                    flexDirectionColumn()
                    alignItemsCenter()
                    backgroundColor(Color(0xFFF9F9F9))
                }
                Refresh {
                    ref {
                        ctx.refreshRef = it
                    }
                    attr {
                        height(50f)
                        allCenter()
                    }
                    event {
                        refreshStateDidChange {
                            when(it) {
                                RefreshViewState.REFRESHING -> {
                                    ctx.refreshText = "正在刷新"
                                    ctx.refreshData {
                                        ctx.refreshRef?.view?.endRefresh()
                                        ctx.refreshText = "刷新成功"
                                    }
                                }
                                RefreshViewState.IDLE -> ctx.refreshText = "下拉刷新"
                                RefreshViewState.PULLING -> ctx.refreshText = "松手即可刷新"
                            }
                        }
                    }
                    Text {
                        attr {
                            color(Color.BLACK)
                            text(ctx.refreshText)
                        }
                    }
                }

                vfor({ctx.items}){
                    Text {
                        attr {
                            height(50f)
                            text(it.title)
                            fontSize(16f)
                            backgroundColor(ctx.randomColor())
                        }
                    }
                }
            }
        }
    }

    private fun refreshData(block: () -> Unit){
        setTimeout(1500){
            this.items.clear()
            refreshCount++
            val items = mutableListOf<ScrollerItem>()
            for (i in 0..50) {
                items.add(ScrollerItem("AnyRefreshExamplePage,refreshCount:$refreshCount index:$i"))
            }
            this.items.addAll(items)
            block.invoke()

        }
    }

    private fun randomColor(): Color {
        var rgbValue: Long = 0xE9000000L
        var totalRGBValue = randomHelper.nextInt() % 64 + 192
        var r = randomHelper.nextInt(499999, 999999)
        var g = randomHelper.nextInt(499999, 999999)
        var b = randomHelper.nextInt(499999, 999999)
        var totalRGB = max(max(r, g), b)
        r = r * totalRGBValue / totalRGB
        g = g * totalRGBValue / totalRGB
        b = b * totalRGBValue / totalRGB
        rgbValue += (r * 0x00010000L)
        rgbValue += (g * 0x00000100L)
        rgbValue += (b * 0x00000001L)
        return Color(rgbValue)
    }
}