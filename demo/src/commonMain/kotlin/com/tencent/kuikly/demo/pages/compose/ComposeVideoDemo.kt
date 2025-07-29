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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.views.VideoPlayControl

@Page("ComposeVideoDemo")
internal class ComposeVideoDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()

        setContent {

            // 定义当前播放时间和总时间
            var currentTime by remember { mutableStateOf(0) }
            var totalTime by remember { mutableStateOf(1) } // 避免除以0错误

            // 定义播放控制状态
            var playControl by remember { mutableStateOf(VideoPlayControl.PLAY) }

            // 视频URL
            val videoUrl = "http://vjs.zencdn.net/v/oceans.mp4"

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black),
            ) {
                // 使用Video Composable
                Video(
                    src = videoUrl,
                    playControl = playControl,
                    playTimeDidChanged = { curTime, total ->
                        println("xxxx playTimeDidChanged $curTime, $total")
                        currentTime = curTime
                        if (total > 0) {
                            totalTime = total
                        }
                    },
                    modifier = Modifier.align(Alignment.Center).size(width = 400.dp, height = 300.dp).background(Color.Red),
                )

                // 底部控制栏
                Column(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Color(0x80000000))
                            .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 时间显示
                    Text(
                        text = "播放进度: ${formatTime(currentTime)} / ${formatTime(totalTime)}",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    // 控制按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 播放/暂停按钮
                        Button(
                            onClick = {
                                playControl =
                                    if (playControl != VideoPlayControl.PLAY) {
                                        VideoPlayControl.PLAY
                                    } else {
                                        VideoPlayControl.PAUSE
                                    }
                            },
                            modifier = Modifier.padding(end = 8.dp),
                        ) {
                            Text(
                                text = if (playControl != VideoPlayControl.PLAY) "播放" else "暂停",
                                color = Color.White,
                            )
                        }

                        // 停止按钮
                        Button(
                            onClick = {
                                playControl = VideoPlayControl.STOP
                            },
                        ) {
                            Text(
                                text = "停止",
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }

    // 格式化时间（毫秒转为分:秒格式）
    private fun formatTime(timeMs: Int): String {
        val totalSeconds = timeMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}
