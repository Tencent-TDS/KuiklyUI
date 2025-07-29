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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.layout.Layout
import com.tencent.kuikly.compose.ui.layout.onSizeChanged
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 创建一个防抖函数，延迟指定时间后执行操作，如果在延迟时间内再次调用则取消前一次操作
 * @param delayMillis 延迟执行的毫秒数
 * @param scope 协程作用域
 * @param action 要执行的操作
 */
class Debouncer(
    private val delayMillis: Long,
    private val scope: CoroutineScope,
) {
    private var job: Job? = null

    fun debounce(action: suspend () -> Unit) {
        job?.cancel()
        job =
            scope.launch {
                delay(delayMillis)
                action()
            }
    }
}

// 现在你可以在你的代码中使用这个防抖函数
// 例如：
/*
val debouncer = remember { Debouncer(500L, coroutineScope) }

// 在需要防抖的地方
debouncer.debounce {
    state.kuiklyInfo.currentContentSize = state.calculateContentSize()
    println("xabs 高度有变化 非滑动中 key: ${itemResult.key} 从新计算contentSize")
}
*/

@Page("ScrollMeasureTest")
internal class ScrollMeasurePage : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ScrollMeasureTest()
        }
    }
}

@Composable
internal fun DemoItem(
    text: String,
    modifier: Modifier,
) {
    SideEffect {
        println("xabs DemoItem 重组 $text")
    }
    Layout(
        content = {
            // 你的内容
            Text(text)
        },
        modifier = modifier,
    ) { measurables, constraints ->
        println("xabs DemoItem 测量 $text")

        val placeables =
            measurables.map { measurable ->
                measurable.measure(constraints)
            }
        val height = placeables.sumOf { it.height }
        val width =
            placeables
                .maxOf { it.width }
                .coerceIn(constraints.minWidth, constraints.maxWidth)

        // 布局逻辑
        layout(width, height) {
            // 放置逻辑
            var y = 0
            placeables.forEachIndexed { index, placeable ->
                println("xabs DemoItem 放置 $text")
                placeable.placeRelative(0, y)
                y += placeable.height
            }
        }
    }
}

@Composable
internal fun ScrollMeasureTest() {
    // 使用mutableStateListOf以确保数据变化时UI会重组
    val dataList = remember { mutableStateListOf<String>() }

    // 只在首次组合时添加数据
    LaunchedEffect(Unit) {
        // 清空列表以避免重复添加
        dataList.clear()
        // 添加数据
        for (i in 0..100) {
            dataList.add("item $i")
        }
    }

    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Gray)
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
                        .padding(2.dp)
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Red),
            )
        }
    }
}
