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

package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.extension.MakeKuiklyComposeNode
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.core.views.VideoPlayControl
import com.tencent.kuikly.core.views.VideoView

@Composable
fun Video(
    src: String,
    playControl: VideoPlayControl,
    playTimeDidChanged: (curTime: Int, totalTime: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    MakeKuiklyComposeNode<VideoView>(
        factory = {
            VideoView()
        },
        modifier = modifier,
        viewInit = {
            getViewAttr().run {
                playControl(VideoPlayControl.PLAY)
                src(src)
            }
        },
        viewUpdate = {
            it.getViewAttr().run {
                src(src)
                playControl(playControl)
            }
            it.getViewEvent().run {
                playTimeDidChanged(handlerFn = playTimeDidChanged)
                playStateDidChanged(handlerFn = { state, b ->
                })
            }
        }
    )
}