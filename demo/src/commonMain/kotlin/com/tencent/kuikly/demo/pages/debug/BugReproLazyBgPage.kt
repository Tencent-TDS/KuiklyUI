/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

package com.tencent.kuikly.demo.pages.debug

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

/**
 * LazyColumn 复用背景色复现页。
 *
 * 200 行，相邻行色相相差 8°。复用节点若留下上一行的背景，新行颜色会和相邻行断开。
 */
@Page("BugReproLazyBgPage")
internal class BugReproLazyBgPage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            BugReproContent()
        }
    }
}

private const val ROW_COUNT = 200

private val rowIds = List(ROW_COUNT) { it }

private fun rowColor(id: Int): Color =
    Color.hsv(hue = (id * 8f) % 360f, saturation = 0.85f, value = 0.95f)

@Composable
private fun BugReproContent() {
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F5F5))) {
        Text(
            "LazyColumn 背景色",
            fontSize = 13.sp,
            modifier = Modifier.padding(8.dp),
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            items(rowIds, key = { it }) { id ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(rowColor(id)),
                ) {
                    Text("Row #$id", fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }
}
