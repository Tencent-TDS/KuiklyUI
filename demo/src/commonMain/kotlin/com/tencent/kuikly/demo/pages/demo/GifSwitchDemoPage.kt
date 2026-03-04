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
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.timer.clearTimeout
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * Demo page to verify GIF image switching behavior.
 *
 * This page demonstrates that:
 * 1. GIF images don't get stuck when switching src
 * 2. GIF images always start playing from the first frame after src change
 *
 * Usage: Click the "Switch GIF" button to toggle between two different GIF images.
 * The GIF should switch smoothly and always start from the first frame.
 */
@Page("gif_switch_demo")
internal class GifSwitchDemoPage : BasePager() {

    private var showFirstGif: Boolean by observable(true)
    private var switchCount: Int by observable(0)
    private var autoSwitchTimeout: String = ""
    private var isAutoSwitching: Boolean by observable(false)

    companion object {
        private const val GIF_URL_1 =
            "https://upload.wikimedia.org/wikipedia/commons/2/2c/Rotating_earth_%28large%29.gif"
        private const val GIF_URL_2 =
            "https://upload.wikimedia.org/wikipedia/commons/d/d3/Newtons_cradle_animation_book_2.gif"
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { backgroundColor(Color.WHITE) }
            NavBar { attr { title = "GIF Switch Demo" } }

            View {
                attr {
                    flexDirectionColumn()
                    alignItemsCenter()
                    margin(20f)
                }

                // Status text
                Text {
                    attr {
                        fontSize(16f)
                        color(Color.BLACK)
                        text("Switch count: ${ctx.switchCount}, Showing: ${if (ctx.showFirstGif) "GIF 1" else "GIF 2"}")
                        margin(10f)
                    }
                }

                // GIF image - switches between two different images
                Image {
                    attr {
                        size(200f, 200f)
                        src(if (ctx.showFirstGif) GIF_URL_1 else GIF_URL_2)
                    }
                }

                // Manual switch button
                View {
                    attr {
                        backgroundColor(Color(0xFF007AFF.toInt()))
                        borderRadius(8f)
                        margin(15f)
                        padding(10f, 20f, 10f, 20f)
                    }
                    event {
                        click {
                            ctx.showFirstGif = !ctx.showFirstGif
                            ctx.switchCount++
                        }
                    }
                    Text {
                        attr {
                            fontSize(16f)
                            color(Color.WHITE)
                            text("Switch GIF")
                        }
                    }
                }

                // Auto switch button (simulates the scroll handler scenario from the issue)
                View {
                    attr {
                        backgroundColor(if (ctx.isAutoSwitching) Color(0xFFFF3B30.toInt()) else Color(0xFF34C759.toInt()))
                        borderRadius(8f)
                        margin(5f)
                        padding(10f, 20f, 10f, 20f)
                    }
                    event {
                        click {
                            if (ctx.isAutoSwitching) {
                                clearTimeout(ctx.autoSwitchTimeout)
                                ctx.isAutoSwitching = false
                            } else {
                                ctx.isAutoSwitching = true
                                ctx.startAutoSwitch()
                            }
                        }
                    }
                    Text {
                        attr {
                            fontSize(16f)
                            color(Color.WHITE)
                            text(if (ctx.isAutoSwitching) "Stop Auto Switch" else "Start Auto Switch (2s interval)")
                        }
                    }
                }

                // Description
                Text {
                    attr {
                        fontSize(13f)
                        color(Color(0xFF888888.toInt()))
                        margin(20f)
                        text(
                            "This demo verifies the GIF switching fix:\n" +
                                "1. Click 'Switch GIF' to manually toggle between images\n" +
                                "2. Click 'Start Auto Switch' to simulate the scroll-triggered scenario\n" +
                                "3. Verify: GIF should not get stuck and should play from the first frame"
                        )
                    }
                }
            }
        }
    }

    private fun startAutoSwitch() {
        autoSwitchTimeout = setTimeout(2000) {
            showFirstGif = !showFirstGif
            switchCount++
            if (isAutoSwitching) {
                startAutoSwitch()
            }
        }
    }
}
