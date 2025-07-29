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
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.extension.NestedScrollMode
import com.tencent.kuikly.compose.extension.nestedScroll
import com.tencent.kuikly.compose.material3.Card
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.graphicsLayer
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("LazyNest")
class LazyColumnNestDemoContainer : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                LazyColumnNestDemo()
            }
        }
    }
}

// 自定义选项枚举
enum class ScrollOption {
    OPTION1, OPTION2, OPTION3, OPTION4
}

// 自定义选择器组件
@Composable
fun CustomSelector(selectedOption: ScrollOption, onOptionSelected: (ScrollOption) -> Unit) {
    val options = listOf(
        "上父优下子优" to ScrollOption.OPTION1,
        "上下父优先" to ScrollOption.OPTION2,
        "上父优下只自" to ScrollOption.OPTION3,
        "子只自滑" to ScrollOption.OPTION4
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        options.forEach { (text, option) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onOptionSelected(option) }
            ) {
                // 自定义选择指示器
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(
                            width = 2.dp,
                            color = if (selectedOption == option) Color.Blue else Color.Gray,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .background(
                            if (selectedOption == option) {
                                Color.Blue
                            } else {
                                Color.Transparent
                            }
//                            ,RoundedCornerShape(4.dp)
                        )
                )
                Text(text = text, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun LazyColumnNestDemo() {
    var selectedOption by remember { mutableStateOf(ScrollOption.OPTION1) }
    Column(modifier = Modifier.fillMaxSize().padding(top = 60.dp)) {
        // 自定义选择器
        CustomSelector(
            selectedOption = selectedOption,
            onOptionSelected = { selectedOption = it }
        )
        // 根据选择显示内容
        when (selectedOption) {
            ScrollOption.OPTION1 -> ScrollContent(
                title = "子列表 向上先滑父亲，向下先滑动自己（新闻场景）",
                scrollUp = NestedScrollMode.PARENT_FIRST,
                scrollDown = NestedScrollMode.SELF_FIRST
            )
            ScrollOption.OPTION2 -> ScrollContent(
                title = "子列表 向上先滑父亲，向下先滑父亲",
                scrollUp = NestedScrollMode.PARENT_FIRST,
                scrollDown = NestedScrollMode.PARENT_FIRST
            )
            ScrollOption.OPTION3 -> ScrollContent(
                title = "子列表 向上先滑父亲，向下只滑动自己",
                scrollUp = NestedScrollMode.PARENT_FIRST,
                scrollDown = NestedScrollMode.SELF_ONLY
            )
            ScrollOption.OPTION4 -> ScrollContent(
                title = "子列表 向上只滑自己，向下直滑动自己",
                scrollUp = NestedScrollMode.SELF_ONLY,
                scrollDown = NestedScrollMode.SELF_ONLY
            )
        }
    }
}

@Composable
fun ScrollContent(title: String, scrollUp: NestedScrollMode, scrollDown: NestedScrollMode) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Box(modifier = Modifier.height(300.dp).background(Color.Green),
                contentAlignment = Alignment.TopCenter) {
                Text("我是父亲顶部")
            }
        }
        item {
            Column {
                Text(
                    text = title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Gray)
                        .padding(8.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(8.dp)
                        .graphicsLayer {  }
                        .nestedScroll(
                            scrollUp = scrollUp,
                            scrollDown = scrollDown
                        )
                ) {
                    items((1..10).toList()) { childIndex ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "子列表项 -$childIndex",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE1F5FE))
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
        item {
            Box(modifier = Modifier.height(200.dp).background(Color.Green),
                contentAlignment = Alignment.BottomCenter) {
                Text("我是父亲底部")
            }
        }
    }
}