package com.tencent.kuikly.demo.pages.video.feed

import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.View

internal class HeaderBarView: ComposeView<HeaderBarViewAttr, ComposeEvent>() {

    private var isContinuePlay by observable(false)
    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }
    override fun createAttr(): HeaderBarViewAttr {
        return HeaderBarViewAttr()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                absolutePosition(left = 0f, top = 20f)
                width(pagerData.pageViewWidth)
                flexDirectionRow()
                justifyContentFlexStart()
            }

            // 左边返回按钮
            vif({ !ctx.attr.backDisable }) {
                View {
                    attr {
                        size(60f,60f)
                        flexDirectionRow()
                        justifyContentFlexStart()
                        alignItemsCenter()
                    }
                    View {
                        attr {
                            size(45f, 45f)
                            justifyContentCenter()
                            alignItemsCenter()
                        }
                        Image {
                            attr {
                                size(12.5f, 23f)
                                src(BACK_ICON)
                                backgroundColor(0X00000000)
                            }
                            event {
                                click {
                                    getPager().acquireModule<RouterModule>(RouterModule.MODULE_NAME)
                                        .closePage()
                                }
                            }
                        }
                    }
                }

            }

            // 右边搜索按钮
            View {
                attr {
                    flex(1f)
                    height(60f)
                    flexDirectionRow()
                    justifyContentFlexEnd()
                    alignItemsCenter()
                    marginRight(10f)
                }
                // 搜索按钮
                Image {
                    attr {
                        size(24f, 24f)
                        src(SEARCH_ICON)
                    }
                    event {
                    }
                }
                Image {
                    attr {
                        size(24f, 24f)
                        marginLeft(15f)
//                        if (!ctx.isContinuePlay) {
//                            src(AUTO_PLAY_ICON_OFF)
//                        } else {
//                            src(AUTO_PLAY_ICON_ON)
//                        }
                        src(GIFT)
                    }
                }

                Image {
                    attr {
                        size(17f, 3f)
                        marginLeft(15f)
                        marginRight(10f)
                        src(MENU_ICON)
                    }
                }
            }


        }
    }
}


internal class HeaderBarViewAttr : ComposeAttr() {
    // todo 暂时没有需要额外的数据传入
    var title : String by observable("")
    var backDisable = false
}

internal fun ViewContainer<*, *>.HeaderBar(init: HeaderBarView.() -> Unit) {
    addChild(HeaderBarView(), init)
}