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

package com.tencent.kuikly.demo.pages.app.feed

import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer

internal class AppFeedVideoView: ComposeView<AppFeedVideoViewAttr, AppFeedVideoViewEvent>() {
    
    override fun createEvent(): AppFeedVideoViewEvent {
        return AppFeedVideoViewEvent()
    }

    override fun createAttr(): AppFeedVideoViewAttr {
        return AppFeedVideoViewAttr()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            // todo: 视频
        }
    }
}

internal class AppFeedVideoViewAttr : ComposeAttr() {
    lateinit var videoUrl: String
}

internal class AppFeedVideoViewEvent : ComposeEvent() {
    
}

internal fun ViewContainer<*, *>.AppFeedVideo(init: AppFeedVideoView.() -> Unit) {
    addChild(AppFeedVideoView(), init)
}