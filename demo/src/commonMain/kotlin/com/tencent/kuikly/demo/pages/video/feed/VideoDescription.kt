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
import com.tencent.kuikly.demo.pages.video.type.VideoItem

internal class VideoDescriptionView: ComposeView<VideoDescriptionViewAttr, VideoDescriptionViewEvent>(){
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                absolutePosition(left = DescriptionMarginLeft, bottom = DescriptionMarginBottom)
                marginBottom(30f)
                flexDirectionColumn()
            }
            Text {
                attr {
                    color(Color.WHITE)
                    text("@" + ctx.attr.vitem.nick)
                    textAlignLeft()
                    fontSize(15f)
                    fontWeightBold()
                }
            }
            Text {
                attr {
                    marginTop(10f)
                    color(Color.WHITE)
                    fontSize(13f)
                    marginRight(60f)
                    text(ctx.attr.vitem.description)
                    textAlignLeft()
                    textOverFlowClip()
                }
            }
            Text {
                attr {
                    marginTop(10f)
                    marginBottom(10f)
                    color(Color.WHITE)
                    width(DescriptionWid)
                    fontSize(10f)
                    text("2022-12-12 12:12")
                    textAlignLeft()
                    textOverFlowClip()
                }
            }
        }
    }

    override fun createAttr(): VideoDescriptionViewAttr {
        return VideoDescriptionViewAttr()
    }

    override fun createEvent(): VideoDescriptionViewEvent {
        return VideoDescriptionViewEvent()
    }



}

internal class VideoDescriptionViewAttr: ComposeAttr(){
    lateinit var vitem: VideoItem
}

internal class VideoDescriptionViewEvent: ComposeEvent(){

}

internal fun ViewContainer<*,*>.VideoDescription(init: VideoDescriptionView.() -> Unit){
    addChild(VideoDescriptionView(), init)
}

