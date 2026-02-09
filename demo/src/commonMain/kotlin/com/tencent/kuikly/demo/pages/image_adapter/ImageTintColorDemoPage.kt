package com.tencent.kuikly.demo.pages.image_adapter

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

@Page("ImageTintColorReusePage")
internal class ImageTintColorDemoPage : BasePager() {

    private var items by observableList<Int>()

    var enableCache: JSONObject = JSONObject().put("enableCache", true)

    override fun created() {
        super.created()
        items.addAll((0 until 30).toList())
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.Companion.WHITE)
            }

            List {
                attr {
                    flex(1f)
                }

                vfor({ ctx.items }) { index ->
                    View {
                        attr {
                            flexDirectionRow()
                            alignItemsCenter()
                            height(100f)
                            marginLeft(16f)
                            marginRight(16f)
                            marginTop(8f)
                            backgroundColor(Color(0xFFF5F5F5))
                            borderRadius(8f)
                        }

                        Image {
                            attr {
                                src("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAASBAMAAAB/WzlGAAAAElBMVEUAAAAAAAAAAAAAAAAAAAAAAADgKxmiAAAABXRSTlMAIN/PELVZAGcAAAAkSURBVAjXYwABQTDJqCQAooSCHUAcVROCHBiFECTMhVoEtRYA6UMHzQlOjQIAAAAASUVORK5CYII=", ctx.enableCache)
                                width(80f)
                                height(80f)
                                marginLeft(10f)
                                borderRadius(8f)
                                tintColor(
                                    when (index % 3) {
                                        0 -> Color.Companion.WHITE
                                        1 -> Color.Companion.RED
                                        else -> Color.Companion.GREEN
                                    }
                                )
                            }
                        }

                        View {
                            attr {
                                marginLeft(12f)
                                flex(1f)
                            }
                            Text {
                                attr {
                                    fontSize(16f)
                                    fontWeightBold()
                                    color(Color(0xFF333333))
                                    text("Item #$index")
                                }
                            }
                            Text {
                                attr {
                                    fontSize(13f)
                                    marginTop(4f)
                                    color(Color(0xFF999999))
                                    text(
                                        when (index % 3) {
                                            0 -> "tintColor: WHITE"
                                            1 -> "tintColor: RED"
                                            else -> "tintColor: GREEN"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}