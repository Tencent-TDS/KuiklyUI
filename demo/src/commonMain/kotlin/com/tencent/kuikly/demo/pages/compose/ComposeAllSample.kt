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
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Card
import com.tencent.kuikly.compose.material3.CardDefaults
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.utils.urlParams
import kotlin.math.absoluteValue

// 表示每个Demo条目的数据类
internal data class DemoItem(
    val title: String,
    val description: String,
    val pageName: String,
)

@Page("ComposeAllSample")
internal class ComposeAllSample : ComposeContainer() {
    // 预定义一组美观的Material Design颜色
    private val demoColors =
        listOf(
            Color(0xFF2196F3), // 蓝色
            Color(0xFF4CAF50), // 绿色
            Color(0xFFFF9800), // 橙色
            Color(0xFFE91E63), // 粉色
            Color(0xFF9C27B0), // 紫色
            Color(0xFF3F51B5), // 靛蓝色
            Color(0xFF009688), // 青色
            Color(0xFFFF5722), // 深橙色
            Color(0xFF795548), // 棕色
            Color(0xFF607D8B), // 蓝灰色
        )

    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("Demo案例-Compose语法") {
                DemoListScreen()
            }
        }
    }

    // 获取所有演示项的列表
    private fun getDemoItems(): List<DemoItem> =
        listOf(
            // 基础组件
            DemoItem("Image", "Image 组件示例", "ComposeImageDemo"),
            DemoItem("Text", "Text 组件示例", "TextDemo"),
            DemoItem("Canvas", "Canvas 组件示例", "CanvasDemo"),
            DemoItem("Animation", "Compose常见动画API示例", "ComposeAnimateDemo1"),
            DemoItem("Gesture", "pointerInput内各种手势示例", "GestureTestDemo"),
            DemoItem("LazyColumn", "LazyColumn各种状态信息示例", "LazyColumnDemo3"),
            DemoItem("LazyRow", "LazyRow基本用法示例", "LazyRowDemo1"),
            DemoItem("Dialog", "Dialog 组件示例", "DialogDemo"),
            DemoItem("BottomSheet", "底部弹窗示例", "BottomSheetDemo1"),
            DemoItem("Shape", "shape示例", "shapedemo"),
            DemoItem("HorizontalPager", "HorizontalPager基本用法示例", "HorizontalPagerDemo1"),
            DemoItem("LazyHorizontalGrid", "LazyHorizontalGrid基本用法示例", "LazyHorizontalGridDemo1"),
            DemoItem("焦点处理", "Focus焦点处理示例", "focusDemo"),
            DemoItem("封装KuiklyView", "封装Kuikly的VideoView为一个Composeable组件示例", "ComposeVideoDemo"),
            DemoItem("TextField", "TextField 组件示例", "TextFieldDemo"),
            DemoItem("PullToRefresh", "PullToRefresh 组件示例", "PullToRefreshDemo"),

            // 动画
            DemoItem("AnimatedVisibility", "AnimatedVisibility动画API示例", "AnimatedVisibilityDemo"),
            DemoItem("AnimatedContent", "AnimatedContent内容过渡动画示例", "AnimatedContentDemo"),
            DemoItem("Tab/TabRow", "Tab/TabRow 组件示例", "TabRowDemo"),
            // LazyRow/LazyColumn
            DemoItem("LazyRow2", "LazyRow的key,type参数示例", "LazyRowDemo2"),
            DemoItem("LazyRow3", "LazyRow各种状态信息示例", "LazyRowDemo3"),
            DemoItem("LazyRowMarquee", "LazyRow实现跑马灯", "MarqueeTextDemo"),
            DemoItem("LazyColumn2", "LazyColumn滑动api示例", "LazyColumnDemo4"),
            DemoItem("LazyColumn嵌套", "LazyColumn嵌套滚动", "LazyNest"),
            // Pager
//            DemoItem("HorizontalPager2", "HorizontalPager的key，SnapPosition参数示例", "HorizontalPagerDemo2"),
            DemoItem("HorizontalPager3", "HorizontalPager各种状态信息示例", "HorizontalPagerDemo3"),
            // FlowRow/FlowColumn
            DemoItem("FlowRow", "FlowRow 流式行布局示例,无懒加载", "FlowRowDemo1"),
            DemoItem("FlowColumn", "FlowColumn 流式列布局示例，无懒加载", "FlowColumnDemo1"),
            // HorizontalGrid 格子布局
            DemoItem("LazyHorizontalGrid2", "LazyHorizontalGrid的key，span跨行参数示例", "LazyHorizontalGridDemo2"),
            DemoItem("LazyHorizontalGrid3", "LazyHorizontalGrid各种状态信息示例", "LazyHorizontalGridDemo3"),
            // HorizontalGrid 错位格子布局
            DemoItem("StaggeredHorizontalGrid1", "StaggeredHorizontalGrid基本用法示例", "StaggeredHorizontalGridDemo1"),
            DemoItem("StaggeredHorizontalGrid2", "StaggeredHorizontalGrid各种状态信息示例", "StaggeredHorizontalGridDemo3"),
            // GraphicsLayer
            DemoItem("SimpleGraphicsLayer", "SimpleGraphicsLayer示例", "graphicslayer1"),
            DemoItem("BlockingGraphicsLayer", "BlockingGraphicsLayer示例", "graphicslayer2"),
            DemoItem("GraphicsLayerSettings", "GraphicsLayerSettings示例", "graphicslayer3"),
            DemoItem("Brush", "Brush示例", "BrushDemo"),
            // Coroutines
            DemoItem("Coroutines", "协程示例", "coroutines"),
            // 在这里添加更多的demo
            DemoItem("OverNativeClickDemo", "遮住原生View可点击", "OverNativeClickDemo"),
            DemoItem("AppearPercentDemo", "可见度百度比", "AppearDemo"),
            DemoItem("内外边距", "Compose内外边距，边框写法示例", "MarginPaddingTest"),
        )

    @Composable
    fun DemoListScreen() {
        // 使用抽离出的函数获取演示列表
        val demoList = remember { getDemoItems() }

        Column(
            modifier =
                Modifier
                    .background(Color(0xFFF5F5F5))
                    .fillMaxSize(),
        ) {

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp), // 减小间距
                contentPadding = PaddingValues(all = 8.dp),
            ) {
                items(demoList) { demo ->
                    DemoItemCard(demo) {
                        navigateToPage(demo)
                    }
                }
            }
        }
    }

    @Composable
    fun DemoItemCard(
        demo: DemoItem,
        onClick: () -> Unit,
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClick),
            shape = RoundedCornerShape(8.dp),
            colors =
                CardDefaults.cardColors(
                    containerColor = Color.White,
                ),
            elevation =
                CardDefaults.cardElevation(
                    defaultElevation = 2.dp,
                ),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 左侧图标指示器
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(getColorForDemo(demo.pageName)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = demo.title.first().toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 右侧文本内容
                Column {
                    Text(
                        demo.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        demo.description,
                        fontSize = 12.sp,
                        color = Color(0xFF666666),
                    )
                }
            }
        }
    }

    // 根据页面名称自动分配颜色
    private fun getColorForDemo(pageName: String): Color {
        // 根据页面名称的哈希值选择颜色索引，确保同一页面总是获得相同的颜色
        val colorIndex = pageName.hashCode().absoluteValue % demoColors.size
        return demoColors[colorIndex]
    }

    // 页面跳转方法
    private fun navigateToPage(demoItem: DemoItem) {
        val params = urlParams("pageName=${demoItem.pageName}&pageTitle=${demoItem.title}")
        val pageData = JSONObject()
        params.forEach {
            pageData.put(it.key, it.value)
        }
        val pageName = pageData.optString("pageName")

        var hotReloadIp: String? = null
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).openPage(pageName, pageData)
    }
} 

@Composable
fun NavBar(title: String) {

}