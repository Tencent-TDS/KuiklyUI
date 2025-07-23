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
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Card
import com.tencent.kuikly.compose.material3.CardDefaults
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.ui.window.Dialog
import com.tencent.kuikly.core.annotations.Page

@Page("PopMenuDialogDemo")
class PopMenuDialogDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            PopMenuDialogDemoContent()
        }
    }
}

@Composable
fun PopMenuDialogDemoContent() {
    var showMenu by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf("") }
    var offsetX by remember { mutableStateOf(100.dp) }
    var offsetY by remember { mutableStateOf(200.dp) }
    val menuItems = listOf("复制", "粘贴", "删除", "更多操作")

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.TopStart).padding(32.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { showMenu = true }) {
                Text("指定位置弹出菜单")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("X:")
                Button(onClick = { offsetX -= 20.dp }) { Text("-") }
                Text("${offsetX.value.toInt()}dp", modifier = Modifier.padding(horizontal = 4.dp))
                Button(onClick = { offsetX += 20.dp }) { Text("+") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Y:")
                Button(onClick = { offsetY -= 20.dp }) { Text("-") }
                Text("${offsetY.value.toInt()}dp", modifier = Modifier.padding(horizontal = 4.dp))
                Button(onClick = { offsetY += 20.dp }) { Text("+") }
            }
            if (selected.isNotEmpty()) {
                Text("已选择: ${'$'}selected", fontSize = 18.sp, color = Color.Blue)
            }
        }

        if (showMenu) {
            Dialog(onDismissRequest = { showMenu = false }) {
                Box(Modifier.fillMaxSize().clickable {
                    showMenu = false
                }) {
                    Card(
                        modifier = Modifier
                            .offset(x = offsetX, y = offsetY)
                            .width(220.dp),
                        elevation = CardDefaults.cardElevation(8.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier.background(Color.White),
                            verticalArrangement = Arrangement.Center
                        ) {
                            menuItems.forEachIndexed { idx, item ->
                                item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selected = item
                                            showMenu = false
                                        }
                                        .background(if (selected == item) Color(0xFFE3F2FD) else Color.Transparent)
                                        .padding(vertical = 16.dp, horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item, fontSize = 16.sp, color = Color.Black)
                                }
                                if (idx != menuItems.lastIndex) {
                                    Spacer(modifier = Modifier.background(Color(0xFFE0E0E0)).fillMaxWidth().padding(vertical = 0.5.dp))
                                }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 