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
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("FrameStaleDropdownPage")
internal class FrameStaleDropdownPage : BasePager() {

    private var expanded by observable(false)
    private var toggleSeq by observable(0)
    private var statusText by observable("Ready. Tap button to expand from 0px to 212px.")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            NavBar {
                attr {
                    title = "Frame Stale Repro"
                }
            }

            View {
                attr {
                    flex(1f)
                    backgroundColor(Color(0xFFF5F6FA))
                    padding(16f)
                }

                Text {
                    attr {
                        text("Repro target: dropdown height animation 0px -> 212px")
                        fontSize(14f)
                        color(Color(0xFF333333))
                    }
                }

                Text {
                    attr {
                        marginTop(8f)
                        text("In H5 with stale-check enabled, first expand may flash and collapse after transitionend.")
                        fontSize(12f)
                        color(Color(0xFF666666))
                    }
                }

                View {
                    attr {
                        marginTop(16f)
                        padding(left = 12f, top = 10f, right = 12f, bottom = 10f)
                        backgroundColor(Color(0xFF1677FF))
                        borderRadius(8f)
                    }
                    event {
                        click {
                            ctx.expanded = !ctx.expanded
                            ctx.toggleSeq++
                            ctx.statusText = if (ctx.expanded) {
                                "Expanding to 212px (toggle #${ctx.toggleSeq})"
                            } else {
                                "Collapsing to 0px (toggle #${ctx.toggleSeq})"
                            }
                        }
                    }

                    Text {
                        attr {
                            text(if (ctx.expanded) "Collapse" else "Expand")
                            color(Color.WHITE)
                            fontSize(14f)
                        }
                    }
                }

                View {
                    attr {
                        marginTop(12f)
                        width(320f)
                        height(if (ctx.expanded) 212f else 0f)
                        backgroundColor(Color.WHITE)
                        borderRadius(10f)
                        overflow(true)
                        animate(
                            animation = Animation.linear(durationS = 0.35f, key = "dropdown-height"),
                            value = ctx.expanded
                        )
                    }
                    event {
                        animationCompletion {
                            if (it.animationKey == "dropdown-height") {
                                ctx.statusText = "Animation finished. Expected inline height=212px when expanded."
                            }
                        }
                    }

                    Text {
                        attr {
                            margin(left = 12f, top = 12f)
                            text("Dropdown content")
                            color(Color(0xFF222222))
                            fontSize(15f)
                        }
                    }

                    Text {
                        attr {
                            margin(left = 12f, top = 40f)
                            text("- Item A")
                            color(Color(0xFF444444))
                            fontSize(13f)
                        }
                    }

                    Text {
                        attr {
                            margin(left = 12f, top = 70f)
                            text("- Item B")
                            color(Color(0xFF444444))
                            fontSize(13f)
                        }
                    }

                    Text {
                        attr {
                            margin(left = 12f, top = 100f)
                            text("- Item C")
                            color(Color(0xFF444444))
                            fontSize(13f)
                        }
                    }

                    Text {
                        attr {
                            margin(left = 12f, top = 130f)
                            text("- Item D")
                            color(Color(0xFF444444))
                            fontSize(13f)
                        }
                    }

                    Text {
                        attr {
                            margin(left = 12f, top = 160f)
                            text("- Item E")
                            color(Color(0xFF444444))
                            fontSize(13f)
                        }
                    }

                    Text {
                        attr {
                            margin(left = 12f, top = 190f)
                            text("- Item F")
                            color(Color(0xFF444444))
                            fontSize(13f)
                        }
                    }
                }

                Text {
                    attr {
                        marginTop(12f)
                        text(ctx.statusText)
                        color(Color(0xFF888888))
                        fontSize(12f)
                    }
                }
            }
        }
    }
}
