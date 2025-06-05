/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.shadow
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.graphicsLayer
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("shadowdemo")
class ShadowDemoPage : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            DemoScaffold("Shadow 示例", true) {
                ShadowDemo()
            }
        }
    }
}

@Composable
private fun ShadowDemo() {
    Text("Modifier.shadow()")
    Box(
        Modifier
            .size(100.dp)
            .shadow(
                elevation = 10.dp,
                ambientColor = Color.Red,
                spotColor = Color.Blue
            )
            .background(Color.White)
    )
    val density = LocalDensity.current
    Text("Modifier.graphicsLayer()")
    Box(
        Modifier
            .size(100.dp)
            .graphicsLayer(
                shadowElevation = with(density) { 10.dp.toPx() },
                ambientShadowColor = Color.Red,
                spotShadowColor = Color.Blue
            )
            .background(Color.White)
    )
    Text("Modifier.graphicsLayer { }")
    Box(
        Modifier
            .size(100.dp)
            .graphicsLayer {
                shadowElevation = with(density) { 10.dp.toPx() }
                ambientShadowColor = Color.Red
                spotShadowColor = Color.Blue
            }
            .background(Color.White)
    )
}