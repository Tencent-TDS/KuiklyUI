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
import com.tencent.kuikly.core.datetime.DateTime
import com.tencent.kuikly.core.directives.velse
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Switch
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("OverNativeClickDemo2")
internal class OverNativeClickDemo2 : BasePager() {

    private var superTouch by observable(false)
    private var result by observable("")
    private val events = mutableListOf<String>()

    private fun output(event: String) {
        events.add(0, "${DateTime.currentTimestamp()}:$event")
        if (events.size > 5) {
            events.removeAt(events.size - 1)
        }
        result = events.joinToString("\n")
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "OverNativeClickDemo"
                }
            }
            vif({ ctx.superTouch }) {
                View {
                    attr {
                        backgroundColor(0x3300FF00)
                        superTouch(true)
                        height(200f)
                    }
                    event {
                        touchDown {
                            ctx.output("green touchDown")
                        }
                        touchUp {
                            ctx.output("green touchUp")
                        }
                    }
                    View {
                        attr {
                            marginLeft(10f)
                            size(100f, 100f)
                            backgroundColor(Color.BLUE)
                            allCenter()
                        }
                        event {
                            touchDown {
                                ctx.output("blue touchDown")
                            }
                            touchUp {
                                ctx.output("blue touchUp")
                            }
                        }
                        View {
                            attr {
                                size(50f, 50f)
                                backgroundColor(Color.RED)
                            }
                            event {
                                touchDown {
                                    ctx.output("red touchDown")
                                }
                                touchUp {
                                    ctx.output("red touchUp")
                                }
                            }
                        }
                    }
                }
            }
            velse {
                View {
                    attr {
                        height(200f)
                    }
                    View {
                        attr {
                            marginLeft(10f)
                            size(100f, 100f)
                            backgroundColor(Color.BLUE)
                            allCenter()
                        }
                        event {
                            touchDown {
                                ctx.output("blue touchDown")
                            }
                            touchUp {
                                ctx.output("blue touchUp")
                            }
                        }
                        View {
                            attr {
                                size(50f, 50f)
                                backgroundColor(Color.RED)
                            }
                            event {
                                touchDown {
                                    ctx.output("red touchDown")
                                }
                                touchUp {
                                    ctx.output("red touchUp")
                                }
                            }
                        }
                    }
                }
            }
            Switch {
                attr {
                    isOn(ctx.superTouch)
                }
                event {
                    switchOnChanged {
                        ctx.superTouch = it
                    }
                }
            }
            Text {
                attr { text(ctx.result) }
            }
        }
    }
}