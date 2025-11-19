package com.tencent.kuikly.demo.pages.video.feed

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.layout.FlexDirection
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

internal class BottomBarView: ComposeView<BottomBarViewAttr, BottomBarViewEvent>(){
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                size(pagerData.pageViewWidth, 50f)
                absolutePosition(bottom = 10f)
                marginBottom(30f)
                flexDirectionRow()
                alignItemsCenter()
            }
            Image {
                attr {
                    marginLeft(10f)
                    size(18f, 18f)
                    backgroundColor(Color.TRANSPARENT_WHITE)
                    src(SEARCH_ICON)
                }
            }
            // 搜索文字
            Text {
                attr {
                    marginLeft(5f)
                    textAlignLeft()
                    color(Color.WHITE)
                    textOverFlowClip()
                    text("相关搜索:  ")
                    fontSize(14f)
                    fontWeightBold()
                }
                event {
                    click {
                        // todo 弹出包含搜索内容的浮层
                    }
                }
            }
            View {
                attr {
                    flex(1f)
                    flexDirection(FlexDirection.ROW_REVERSE)
                }
                Image {
                    attr {
                        marginRight(35f)
                        size(12f,12f)
                        backgroundColor(Color.TRANSPARENT_WHITE)
                        src(BOTTOM_SEARCH_BAR_ARROW)
                    }
                }
            }


        }
    }

    override fun createAttr(): BottomBarViewAttr {
        return BottomBarViewAttr()
    }

    override fun createEvent(): BottomBarViewEvent {
        return BottomBarViewEvent()
    }

}

internal class BottomBarViewAttr: ComposeAttr(){
}

internal class BottomBarViewEvent: ComposeEvent(){
}

internal fun ViewContainer<*,*>.BottomBar(init: BottomBarView.() -> Unit){
    addChild(BottomBarView(), init)
}