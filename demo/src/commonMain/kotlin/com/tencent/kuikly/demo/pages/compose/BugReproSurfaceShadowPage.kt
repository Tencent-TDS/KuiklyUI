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
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Surface
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.shadow
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

/**
 * Regression test for Surface shadowElevation double unit conversion bug (fixed).
 *
 * Compares Surface(shadowElevation=4.dp) vs Box(Modifier.shadow(4.dp)) side-by-side.
 * After fix, both should produce identical shadow sizes regardless of density.
 */
@Page("BugReproSurfaceShadowPage")
class BugReproSurfaceShadowPage : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            BugReproSurfaceShadowContent()
        }
    }
}

@Composable
fun BugReproSurfaceShadowContent() {
    val density = LocalDensity.current.density
    val testElevation = 4.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF303030))
            .padding(16.dp)
    ) {
        Text(
            text = "Density: $density  |  Test Elevation: ${testElevation.value}dp",
            color = Color.White
        )
        Text(
            text = "预期: Surface 阴影 = Box 阴影（修复后应相等）",
            color = Color(0xFF4CAF50)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Left: Box with Modifier.shadow (correct, single conversion)
        Text(text = "Box(Modifier.shadow(4.dp)) — 正确的单次换算", color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .shadow(
                    elevation = testElevation,
                    shape = RoundedCornerShape(12.dp)
                )
                .background(Color(0xFF424242), RoundedCornerShape(12.dp)),
        ) {
            Text(
                text = "Box.shadow(${testElevation.value}dp)",
                color = Color.White,
                modifier = Modifier.padding(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Right: Surface with shadowElevation (fixed: no double conversion)
        Text(text = "Surface(shadowElevation=4.dp)", color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = testElevation,
            color = Color(0xFF424242),
        ) {
            Text(
                text = "Surface.elevation=${testElevation.value}dp",
                color = Color.White,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
