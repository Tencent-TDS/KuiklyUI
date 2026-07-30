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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.animation.core.LinearEasing
import com.tencent.kuikly.compose.animation.core.animateFloatAsState
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.wrapContentHeight
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("MarqueeTextDemo")
internal class MarqueeTextDemo : ComposeContainer() {

    override fun willInit() {
        setContent {
            ComposeNavigationBar {
                MarqueeText(
                    text = "This is a long text marquee example1 This is a long text marquee example2 This is a long text marquee example3",
                    fontSize = 24,
                    textColor = Color.Black,
                    modifier = Modifier
                        .height(50.dp)
                        .background(color = Color.Red)
                )
            }
        }
    }
}

@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 16, // 字体大小
    textColor: Color = Color.Black, // 字体颜色
    backgroundColor: Color = Color.Transparent,
    speed: Int = 10000 // 控制跑马灯速度的参数，可以调整
) {
    var offsetX by remember { mutableStateOf(0f) }
    var targetX by remember { mutableStateOf(1f) }

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(
            durationMillis = speed, // 动画时长
            easing = LinearEasing
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.CenterStart
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        ) {
            item {
                val repeatedText = remember { text + " *** " } // 分隔符确保能看出滚动效果
                Text(
                    text = repeatedText,
                    color = textColor,
                    fontSize = fontSize.sp,
                    modifier = Modifier
                        .offset(x = -(animatedOffsetX * (repeatedText.length * 8)).dp) // 动画偏移效果
                        .fillMaxHeight()
                )
            }
        }
    }
    LaunchedEffect(Unit) {
        offsetX = targetX
    }
}
