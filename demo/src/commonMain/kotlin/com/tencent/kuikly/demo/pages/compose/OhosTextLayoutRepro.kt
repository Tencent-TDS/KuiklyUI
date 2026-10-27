/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 */

package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.IntrinsicSize
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

/** Check that text stays within its own background, including after intrinsic queries. */
@Page("OhosComposeTextLayoutRepro")
class OhosTextLayoutRepro : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            var narrow by remember { mutableStateOf(false) }
            val width = if (narrow) 180.dp else 280.dp
            val sample = "多行中文应完整显示，右侧对齐，反复切换宽度后不能漏字、错行或重叠。"
            ComposeNavigationBar {
                LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                    item {
                        Column(Modifier.fillMaxWidth()) {
                            Button(onClick = { narrow = !narrow }) { Text("切换宽度") }
                            Text("包裹内容：文字应在黄色背景内")
                            Box(Modifier.width(width)) {
                                Text(
                                    "较长的第一行\n短行",
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.background(Color.Yellow)
                                )
                            }
                            Box(Modifier.width(width)) {
                                Text(
                                    "较长的第一行\n短行",
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.background(Color.Yellow)
                                )
                            }
                            Text("固定宽度：多行中文")
                            Text(
                                sample,
                                modifier = Modifier.width(width).background(Color.LightGray),
                                textAlign = TextAlign.Right,
                                fontSize = 18.sp
                            )
                            Text("IntrinsicSize.Min 后的实际测量")
                            Row(Modifier.width(width).height(IntrinsicSize.Min)) {
                                Text(
                                    sample,
                                    modifier = Modifier.weight(1f).background(Color.Yellow),
                                    textAlign = TextAlign.Right
                                )
                                Text("旁列", modifier = Modifier.width(40.dp))
                            }
                            Text("截断：两行后省略")
                            Text(
                                sample + sample,
                                modifier = Modifier.width(width).background(Color.LightGray),
                                textAlign = TextAlign.Center,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}
