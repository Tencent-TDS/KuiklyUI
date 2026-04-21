package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.*
import com.tencent.kuikly.core.views.layout.Center
import com.tencent.kuikly.demo.pages.demo.base.NavBar

fun Attr.cssClass(value: String) {
    "cssClass" with value
}

@Page("cssClassTestPage")
internal class CssClassTestPage : Pager() {
    var dynamicClassName by observable("test-single-class")
    var dynamicClassNameForMultipleCase by observable("test-multi-class-1 test-multi-class-2")

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                }
                NavBar { attr { title = "CSS Class Test" } }

                // Test Case 1: Single CSS Class
                View {
                    attr {
                        size(pagerData.pageViewWidth, 100f)
                        backgroundColor(Color.WHITE)
                        cssClass(ctx.dynamicClassName)
                    }
                    Center {
                        Text {
                            attr {
                                text(ctx.dynamicClassName)
                                fontSize(16f)
                                cssClass("test-text-class")
                            }
                        }
                    }
                }

                // Test Case 2: Multiple CSS Classes
                View {
                    attr {
                        size(pagerData.pageViewWidth, 100f)
                        marginTop(20f)
                        backgroundColor(Color.WHITE)
                        cssClass(ctx.dynamicClassNameForMultipleCase)
                    }
                    Center {
                        Text {
                            attr {
                                text(ctx.dynamicClassNameForMultipleCase)
                                fontSize(16f)
                            }
                        }
                    }
                }

                // Test Case 3: Empty/Whitespace Handling
                View {
                    attr {
                        size(pagerData.pageViewWidth, 100f)
                        marginTop(20f)
                        backgroundColor(Color.WHITE)
                        cssClass("  test-padded-class  ")
                    }
                    Center {
                        Text {
                            attr {
                                text("Padded CSS Class: .test-padded-class")
                                fontSize(16f)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun created() {
        super.created()
        val ctx = this
        setTimeout(2000) {
            ctx.dynamicClassName = "test-single-class-updated"
            ctx.dynamicClassNameForMultipleCase = "test-multi-class-1-updated test-multi-class-2-updated"
        }
    }
}
