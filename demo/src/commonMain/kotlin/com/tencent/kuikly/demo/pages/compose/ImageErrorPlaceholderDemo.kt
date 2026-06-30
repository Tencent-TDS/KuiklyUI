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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.resources.DrawableResource
import com.tencent.kuikly.compose.resources.InternalResourceApi
import com.tencent.kuikly.compose.resources.painterResource
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.attr.ImageUri

/**
 * Image Error Placeholder Demo
 * 横向列表展示 Image 组件，使用不可达 URL 触发 error 状态，显示本地资源占位图
 */
@Page("ImageErrorPlaceholderDemo")
class ImageErrorPlaceholderDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("Image Error Placeholder Demo") {
                ImageErrorPlaceholderContent()
            }
        }
    }

    @Composable
    fun ImageErrorPlaceholderContent() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "横向列表，Image 使用不可达 URL + 本地资源占位图",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 刷新按钮
            var refreshKey by remember { mutableStateOf(0) }
            Button(
                onClick = { refreshKey++ },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text("刷新列表 (key: $refreshKey)")
            }

            // 横向列表
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(10) { index ->
                    // 使用 key 触发重新加载
                    key(refreshKey) {
                        ImageListItem(index = index)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "说明：",
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "• 每个 Image 使用不可达的 URL (httpbin.org/status/404)\n" +
                        "• 加载中显示 placeholder (loading.png)\n" +
                        "• 加载失败后显示 error 占位图 (error.png)\n" +
                        "• 点击刷新按钮可重新触发加载",
                fontSize = 12.sp,
                color = Color.Gray,
                lineHeight = 18.sp
            )
        }
    }

    @Composable
    private fun ImageListItem(index: Int) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable {
                // 点击可添加交互
            }
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    // 不可达的 URL，加载中显示 placeholder，失败后显示 error
                    model = "https://httpbin.org/status/404?item=$index",
                    placeholder = painterResource(loadingResource),
                    error = painterResource(errorResource)
                ),
                contentDescription = "Item $index",
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Item $index",
                fontSize = 12.sp,
                color = Color.DarkGray
            )
        }
    }
}

@OptIn(InternalResourceApi::class)
private val loadingResource by lazy(LazyThreadSafetyMode.NONE) {
    DrawableResource(ImageUri.commonAssets("loading.png").toUrl(""))
}

@OptIn(InternalResourceApi::class)
private val errorResource by lazy(LazyThreadSafetyMode.NONE) {
    DrawableResource(ImageUri.commonAssets("error.png").toUrl(""))
}
