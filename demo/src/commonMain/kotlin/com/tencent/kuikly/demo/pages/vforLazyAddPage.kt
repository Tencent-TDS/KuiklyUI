package com.tencent.kuikly.demo.pages.demo.vforlazy

import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.PagerScope
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vforLazy
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.timer.setTimeout
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

@Page("vforlazyAdd")
internal class P3 : BasePager() {
    private var listedModels by observableList<MyText>()

    override fun created() {

        val temp = MyText(this)
        temp.text = "第一段文本"
        temp.color = Color.RED
        listedModels.add(temp)
        setTimeout(500) {
            val temp2 = MyText(this)
            temp2.color = Color.BLUE
            temp2.text = "第二段文本"
            listedModels.add(temp2)
        }

        super.created()
    }
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            List {
                attr {
                    height(300f)
                    backgroundColor(Color.GRAY)
                }
                vforLazy({ctx.listedModels}) {  item, index, count ->

                    View {
                        attr {
                            if (index == 0) {
                                height(100f)
                            }
                            allCenter()
                            backgroundColor(item.color)
                        }
                        Text {
                            attr {
                                fontSize(25f)
                                text(item.text)
                            }
                        }
                    }

                }
            }
            List {
                attr {
                    height(300f)
                    backgroundColor(Color.GREEN)
                }
                vforLazy({ctx.listedModels}) {  item, index, count ->

                    View {
                        attr {
                            if (index == 0) {
                                height(100f)
                            } else {
                                height(200f)
                            }
                            allCenter()
                            backgroundColor(item.color)
                        }
                        Text {
                            attr {
                                fontSize(25f)
                                text(item.text)
                            }
                        }
                    }

                }
            }

        }
    }
}

class MyText(pagerScope: PagerScope) {
    var text by pagerScope.observable("xxx")
    var color = Color.BLUE
}