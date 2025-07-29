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

package com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vbind
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import kotlin.math.max
import kotlin.random.Random

internal class ListExampleModel(
    val title: String = "",
    val color: Color = Color.TRANSPARENT,
)

@Page("ListExamplePage")
internal class ListExamplePage: BasePager() {
    private var listedModels by observableList<ListExampleModel>()
    private val randomHelper = Random.Default

    private lateinit var footerRefreshView: FooterRefreshView
    private lateinit var headerRefreshView: RefreshView
    private var footerRefreshState by observable(FooterRefreshState.IDLE)
    private var headerRefreshText by observable("下拉刷新")

    override fun pageDidAppear() {
        requestModel(false)
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { backgroundColor(Color.WHITE) }
            NavBar { attr { title = "List Example" } }
            List {
                attr {
                    flex(1f)
                }
                Refresh {
                    ctx.headerRefreshView = this
                    attr {
                        flexDirectionRow()
                        justifyContentCenter()
                        alignItemsCenter()
                        height(56f)
                    }
                    event { refreshStateDidChange { state ->
                        when (state) {
                            RefreshViewState.REFRESHING -> {
                                ctx.headerRefreshText = "正在刷新"
                                ctx.requestModel(false) { success ->
                                    ctx.headerRefreshText = if (success) "刷新成功" else "刷新失败"
                                    setTimeout(600) {
                                        ctx.headerRefreshView.endRefresh()
                                    }
                                }
                            }
                            RefreshViewState.PULLING -> ctx.headerRefreshText = "松开刷新"
                            RefreshViewState.IDLE -> ctx.headerRefreshText =
                                if (ctx.headerRefreshText == "刷新成功" || ctx.headerRefreshText == "刷新失败") ctx.headerRefreshText
                                else "下拉刷新"
                        }
                    } }
                    Image {
                        attr {
                            src(
                                "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFAAAABQCAMAAAC5zwKfAAABQVBMVEUAAAD////v6u/////t5+3y7PL7+/vx7fLg19/07PTr6+vd1Nzh1uHp5Oj09PTu6e749Pjy8vPv7O/o5Oj4+Pns6ez19fXm3Ob4+Pjp4urp4ujz8PLc2N7Z1dvm5Ofh2uH7/Pzn4efz8/P6+vr09/fb0Nvl2uXr5+ri3+Laztr09fbe1t/o3Ofx8fHdz9729vbk2uHw6fDx8vL9/f3d0N38/Pzb1dvi2ODx8vLq6urv8PDt7e3t7+78/P3s6+zj4uTi4OLRxdHn5uf+//7z9PTZz9je297WztXRx9Dg3uHWydXTytLb19vZ0tjl5OX2+Pfo6OjZ19rOw8329ffu5+7d193d0Nzr5Ovn4ejY1djOzM/d1NzUxtTh0+Hh2+Dc2tzTzNLOxc3ay9rV09bS0NPSx9HW0Nb4+Pnp3+nMyMzl2eVVtzfcAAAAOHRSTlMAA0QHPjAP/uMWDfnYlIZgHOTW0LSgko94d1km9fG6rptsa2BI/O3n5NPKxcKsnFVVI/Ty8OqmhNTrSmMAAAL9SURBVFjD7dXXVuJQFIDh0FE60rGOjr1OTUglGtAEkFCCVAWU4vs/wOxzIjNzw5IVnKvJv7yKrs99SoAwMzMzMzMz+79z2i92A3tpy8doK4ljmqYp6CBq/YDhYl8yDI3FsiiW1pYlraEMjsEzirzAR1eX8T4xgJEkCSKDxBIvCGsrS3iYQyBE0xyMyN/ljIt7DHi42apFGDGX27QYPI8Qo3OzNXM6WCgkjYExTp/vBiIzyNOX/KiuB414QYrJIO4eApHhOHwod48FdegzAib+gDBiBg+IQFUdjjbe7o7DfxbdcS4Ihjk4EwziJf8eUG0+t54j27HE7l4M78KhdaETsVIIzOg7SMKK4VXhMTh8brXEKkTSDA0PhXXHO5t34Y+ESBKDICIOL7gsgpcrNMGbrp+uHTQ4+DUHD+sn8zHYlTBVKj1AFEdngMQah7wyPwOnLTf8qQ/+I03Bs3r7ah63ekY9NodF99bPlE/kaEYPOPD0AfGKp9MNuNubNHmje925u7jNihs/UkH82okUFtEbhzwReXBnmsOnljLKJbaPSdJVe4CRi5pmnwdGWPZ7xL9rTzttAQSiOJ0rlUqiKJYpeHvu2fsq6XLVJq9dRW2O8oNvzrk7GBqz1zj2K/tX1/AUx7JVsGqNyWT/NL7f7vfzeSXbOSfmZktFw67qGCOzMDke61Jj8vra7XbjK7CJl1Jevr3NZo/e+Yi0pa2BpN8XbjRqKEAacOiC0MvV25KUz8uDgabfPPuRnM1mz8FbpFRPEHiefxCgO8AKarMIC1TkTkf2WgicxXFpXfjT0XZYz/V66AewQrMI2tOT8gIjdQYBwkiBertQB0uFkxxhTUE7loUBbYZAy2YR18azAXbb71cqAH62G/1GdkuSVGy3NUlSlDcOPC1JGC2NxG5X0yTwKuBVkBcnjHflhWsiadoAgXg++XOSWKZVn0eSoRfYQcQNvHZiyRxbQCryCyRL7h0bsXzBnS2vx+Nxn8TtFuKjsthsFsLMzMzMzMzsH/cLPKnsav8gklIAAAAASUVORK5CYII="
                            )
                            width(40f)
                            height(40f)
                        }
                    }
                    Text {
                        attr {
                            marginLeft(5f)
                            fontSize(14f)
                            fontWeight500()
                            color(Color("kuikly_skin_color_text_light"))
                            text(ctx.headerRefreshText)
                        }
                    }
                }
                vfor({ ctx.listedModels }) { model ->
                    View {
                        attr {
                            height(80f)
                            backgroundColor(model.color)
                            allCenter()
                        }
                        Text { attr { text(model.title) } }
                    }
                }
                FooterRefresh {
                    ctx.footerRefreshView = this
                    attr {
                        height(40f)
                        allCenter()
                    }
                    event {
                        refreshStateDidChange {
                            ctx.footerRefreshState = it
                            if (it == FooterRefreshState.REFRESHING) {
                                ctx.requestModel(true)
                            }
                        }
                        click {
                            if (ctx.footerRefreshState == FooterRefreshState.FAILURE) {
                                ctx.footerRefreshView.beginRefresh()
                            }
                        }
                    }
                    vbind({ ctx.footerRefreshState }) {
                        Text {
                            attr {
                                fontSize(14f)
                                color(Color(0xFFC5C5C5))
                                text(
                                    when(ctx.footerRefreshState) {
                                        FooterRefreshState.IDLE -> ""
                                        FooterRefreshState.REFRESHING -> "正在加载"
                                        FooterRefreshState.NONE_MORE_DATA -> "没有更多了"
                                        FooterRefreshState.FAILURE -> "加载失败，请点击重试"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestModel(loadMore: Boolean, callback: ((Boolean) -> (Unit)) = {}) {
        setTimeout(500) {
            if (!loadMore) {
                listedModels.clear()
            }
            var models = mutableListOf<ListExampleModel>()
            for (i in 0 until 20) {
                models.add(ListExampleModel(
                    title = "${listedModels.count() + i}",
                    color = randomColor()
                ))
            }
            listedModels.addAll(models)
            if (listedModels.count() >= 100) {
                footerRefreshState = FooterRefreshState.NONE_MORE_DATA
                footerRefreshView.endRefresh(FooterRefreshEndState.NONE_MORE_DATA)
            } else {
                footerRefreshState = FooterRefreshState.IDLE
                footerRefreshView.endRefresh(FooterRefreshEndState.SUCCESS)
            }
            callback(true)
        }
    }

    private fun randomColor(): Color {
        var rgbValue : Long = 0xE9000000L
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