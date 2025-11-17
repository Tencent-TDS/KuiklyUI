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


package com.tencent.kuikly.demo.pages.video.feed

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.video.type.VideoItem

internal class CommentHeaderBarView: ComposeView<CommentHeaderBarAttr, CommentHeaderBarEvent>(){

    private var isShowSearch: Boolean = false

    override fun created() {
        isShowSearch = !this.attr.vitem.searchContent.isEmpty()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionColumn()
                justifyContentCenter()
                alignItemsCenter()
            }

            // 搜索提示区域
            View {
                attr {
                    if(ctx.isShowSearch){
                        size(pagerData.pageViewWidth, 50f)
                    } else{
                        size(0f,0f)
                    }
                    flexDirectionRow()
                }
                Text {
                    attr{
                        flex(1f)
                        alignSelfCenter()
                        marginLeft(10f)
                        fontSize(15f)
                        text(ctx.attr.vitem.searchContent)
                        color(Color.BLACK)
                    }
                }
            }
            // 分割线
            View {
                attr {
                    alignSelfCenter()
                    if(ctx.isShowSearch) {
//                        marginLeft(10f)
                        size(pagerData.pageViewWidth-20f, 1f)
                        backgroundColor(Color.GRAY)
                    } else{
                        size(0f,0f)
                    }
                }
            }
            View {
                attr {
                    absolutePosition(top = 0f, right = 0f)
                    size(width = 55f, height = 50f)
                    justifyContentCenter()
                    alignItemsCenter()
                }
                Button {
                    attr {
                        size(12f, 12f)
                        backgroundColor(Color.TRANSPARENT_WHITE)
                        imageAttr {
                            size(12f, 12f)
                            src(CLOSE_ICON)
                        }
                    }
                }
            }
            View {
                attr {
                    size(pagerData.pageViewWidth, 50f)
                    justifyContentCenter()
                }
                Text {
                    attr {
                        text("213 条评论")      // 这里后续需要替换为变量
                        fontSize(14f)
                        fontWeightBold()
                        alignSelfFlexStart()
                        marginLeft(10f)
                    }
                }
            }

        }
    }

    override fun createAttr(): CommentHeaderBarAttr {
        return CommentHeaderBarAttr()
    }

    override fun createEvent(): CommentHeaderBarEvent {
        return CommentHeaderBarEvent()
    }

}

internal class CommentHeaderBarAttr: ComposeAttr(){
    lateinit var vitem: VideoItem
}

internal class CommentHeaderBarEvent: ComposeEvent(){
}

internal fun ViewContainer<*, *>.CommentHeaderBar(init: CommentHeaderBarView.() -> Unit){
    addChild(CommentHeaderBarView(), init)
}