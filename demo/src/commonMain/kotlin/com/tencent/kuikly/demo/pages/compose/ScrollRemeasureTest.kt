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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.wrapContentHeight
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.extension.bouncesEnable
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.layout.onSizeChanged
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("ScrollReMeasurePage")
internal class ScrollReMeasurePage : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ScrollReMeasureTest()
        }
    }
}

@Composable
internal fun ScrollReMeasureTest() {
    // 使用mutableStateListOf以确保数据变化时UI会重组
    val dataList = remember { mutableStateListOf<String>() }

    // 只在首次组合时添加数据
    LaunchedEffect(Unit) {
        // 清空列表以避免重复添加
        dataList.clear()
        // 添加数据
        for (i in 0..1) {
            dataList.add("item $i")
        }
    }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(Color.Gray)
                .border(2.dp, Color.Yellow)
                .bouncesEnable(false)
                .onSizeChanged {
                    println("xabs lazyColumn size change: $it")
                },
    ) {
        items(dataList.size) { index ->
            val item = dataList[index]
            DemoItem(
                item,
                modifier =
                    Modifier
                        .clickable {
                            dataList[index] = item + "超额过去玩恶搞\n超额过去玩恶搞" +
                                "\n超额过去玩恶搞\n超额过去玩恶搞\n超额过去玩恶搞" +
                                "\n超额过去玩恶搞\n超额过去玩恶搞\n超额过去玩恶搞" +
                                "\n超额过去玩恶搞\n超额过去玩恶搞\n超额过去玩恶搞"
                        }.padding(100.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(Color.Red),
            )
        }
    }
}
