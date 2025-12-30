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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.animation.core.Spring
import com.tencent.kuikly.compose.animation.core.spring
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.animation.core.updateTransition
import com.tencent.kuikly.compose.animation.animateColor
import com.tencent.kuikly.compose.animation.core.animateDp
import com.tencent.kuikly.compose.animation.core.animateFloat
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Card
import com.tencent.kuikly.compose.material3.CardDefaults
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.rotate
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.graphicsLayer
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("TransitionDemo")
class TransitionDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            TransitionDemo()
        }
    }

    @Composable
    fun TransitionDemo() {
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .background(Color(0xFFF5F5F5)),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. 基础转场动画示例
            item {
                DemoCard(title = "基础转场动画") {
                    BasicTransitionExample()
                }
            }

            // 2. 多属性转场动画示例
            item {
                DemoCard(title = "多属性转场动画") {
                    MultiPropertyTransitionExample()
                }
            }

            // 3. 复杂状态转场示例
            item {
                DemoCard(title = "复杂状态转场") {
                    ComplexStateTransitionExample()
                }
            }
        }
    }

    @Composable
    fun DemoCard(
        title: String,
        content: @Composable () -> Unit,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                content()
            }
        }
    }

    // 1. 基础转场动画示例
    @Composable
    fun BasicTransitionExample() {
        var state by remember { mutableStateOf(BoxState.Collapsed) }
        val transition = updateTransition(targetState = state, label = "boxTransition")

        val size by transition.animateDp(
            transitionSpec = { spring() },
            label = "size"
        ) { if (it == BoxState.Expanded) 200.dp else 100.dp }

        val color by transition.animateColor(
            transitionSpec = { tween(500) },
            label = "color"
        ) { if (it == BoxState.Expanded) Color.Green else Color.Red }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = {
                state = if (state == BoxState.Expanded) BoxState.Collapsed else BoxState.Expanded
            }) {
                Text("切换状态")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(size)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text("转场动画", color = Color.White)
            }
        }
    }

    enum class BoxState { Collapsed, Expanded }

    // 2. 多属性转场动画示例
    @Composable
    fun MultiPropertyTransitionExample() {
        var state by remember { mutableStateOf(BoxState.Collapsed) }
        val transition = updateTransition(targetState = state, label = "multiPropertyTransition")

        val size by transition.animateDp(
            transitionSpec = { spring() },
            label = "size"
        ) { if (it == BoxState.Expanded) 200.dp else 100.dp }

        val color by transition.animateColor(
            transitionSpec = { tween(500) },
            label = "color"
        ) { if (it == BoxState.Expanded) Color.Green else Color.Red }

        val rotation by transition.animateFloat(
            transitionSpec = { tween(600) },
            label = "rotation"
        ) { if (it == BoxState.Expanded) 180f else 0f }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = {
                state = if (state == BoxState.Expanded) BoxState.Collapsed else BoxState.Expanded
            }) {
                Text("切换状态")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        rotationZ = rotation
                    }
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text("多属性动画", color = Color.White)
            }
        }
    }

    // 3. 复杂状态转场示例
    @Composable
    fun ComplexStateTransitionExample() {
        var cardState by remember { mutableStateOf(CardState.Normal) }
        val transition = updateTransition(targetState = cardState, label = "cardTransition")

        // 尺寸动画
        val width by transition.animateDp(
            transitionSpec = {
                when {
                    initialState isTransitioningTo CardState.Expanded ->
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    else -> tween(300)
                }
            },
            label = "width"
        ) {
            when (it) {
                CardState.Normal -> 150.dp
                CardState.Expanded -> 300.dp
                CardState.Highlighted -> 180.dp
            }
        }

        val height by transition.animateDp(
            transitionSpec = { spring() },
            label = "height"
        ) {
            when (it) {
                CardState.Normal -> 100.dp
                CardState.Expanded -> 200.dp
                CardState.Highlighted -> 120.dp
            }
        }

        // 颜色动画
        val backgroundColor by transition.animateColor(
            transitionSpec = { tween(400) },
            label = "backgroundColor"
        ) {
            when (it) {
                CardState.Normal -> Color(0xFF2196F3)
                CardState.Expanded -> Color(0xFF4CAF50)
                CardState.Highlighted -> Color(0xFFFF9800)
            }
        }

        // 位置动画
        val offsetX by transition.animateDp(
            transitionSpec = { spring() },
            label = "offsetX"
        ) {
            when (it) {
                CardState.Normal -> 0.dp
                CardState.Expanded -> 0.dp
                CardState.Highlighted -> 20.dp
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { cardState = CardState.Normal }) {
                    Text("普通")
                }
                Button(onClick = { cardState = CardState.Expanded }) {
                    Text("展开")
                }
                Button(onClick = { cardState = CardState.Highlighted }) {
                    Text("高亮")
                }
            }

            Box(
                modifier = Modifier
                    .offset(x = offsetX)
                    .size(width, height)
                    .background(backgroundColor, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when (cardState) {
                            CardState.Normal -> "普通状态"
                            CardState.Expanded -> "展开状态"
                            CardState.Highlighted -> "高亮状态"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${width.value.toInt()} x ${height.value.toInt()}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    enum class CardState { Normal, Expanded, Highlighted }
}

