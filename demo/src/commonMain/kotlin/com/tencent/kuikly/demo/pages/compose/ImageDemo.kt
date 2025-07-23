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
import com.tencent.kuikly.compose.coil3.AsyncImagePainter
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
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
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.alpha
import com.tencent.kuikly.compose.ui.draw.rotate
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.layout.onSizeChanged
import com.tencent.kuikly.compose.ui.unit.LayoutDirection
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page

@Page("ComposeImageDemo")
internal class ImageDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        // 这里可以修改一些基本配置
        layoutDirection = LayoutDirection.Ltr

        setContent {
            ComposeNavigationBar() {
                Demo2()
            }
        }
    }

    @Composable
    fun Demo2() {
        Row {
            Column(
                modifier = Modifier.width(90.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Crop")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png"),
                    contentDescription = null,
                    alignment = Alignment.Center,
                    contentScale = ContentScale.Crop
                )
                Text("Fit")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png"),
                    contentDescription = null,
                    alignment = Alignment.Center,
                    contentScale = ContentScale.Fit
                )
                Text("FillHeight")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png"),
                    contentDescription = null,
                    alignment = Alignment.Center,
                    contentScale = ContentScale.FillHeight
                )
                Text("FillWidth")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png"),
                    contentDescription = null,
                    alignment = Alignment.Center,
                    contentScale = ContentScale.FillWidth
                )
                Text("Inside")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png"),
                    contentDescription = null,
                    alignment = Alignment.Center,
                    contentScale = ContentScale.Inside
                )
                Text("None")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png"),
                    contentDescription = null,
                    alignment = Alignment.Center,
                    contentScale = ContentScale.None
                )
                Text("FillBounds")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png"),
                    contentDescription = null,
                    alignment = Alignment.Center,
                    contentScale = ContentScale.FillBounds
                )
            }
            Column(
                modifier = Modifier.width(90.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Crop")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png"),
                    contentDescription = null,
                    alignment = Alignment.TopStart,
                    contentScale = ContentScale.Crop
                )
                Text("Fit")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png"),
                    contentDescription = null,
                    alignment = Alignment.TopStart,
                    contentScale = ContentScale.Fit
                )
                Text("FillHeight")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png"),
                    contentDescription = null,
                    alignment = Alignment.TopStart,
                    contentScale = ContentScale.FillHeight
                )
                Text("FillWidth")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png"),
                    contentDescription = null,
                    alignment = Alignment.TopStart,
                    contentScale = ContentScale.FillWidth
                )
                Text("Inside")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png"),
                    contentDescription = null,
                    alignment = Alignment.TopStart,
                    contentScale = ContentScale.Inside
                )
                Text("None")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png"),
                    contentDescription = null,
                    alignment = Alignment.TopStart,
                    contentScale = ContentScale.None
                )
                Text("FillBounds")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png"),
                    contentDescription = null,
                    alignment = Alignment.TopStart,
                    contentScale = ContentScale.FillBounds
                )
            }
            Column(
                modifier = Modifier.width(90.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Crop")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/be8ff284.png"),
                    contentDescription = null,
                    alignment = Alignment.Center,
                    contentScale = ContentScale.Crop
                )
                Text("Fit")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/be8ff284.png"),
                    contentDescription = null,
                    alignment = Alignment.Center,
                    contentScale = ContentScale.Fit
                )
                Text("FillHeight")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/be8ff284.png"),
                    contentDescription = null,
                    alignment = Alignment.Center,
                    contentScale = ContentScale.FillHeight
                )
                Text("FillWidth")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/be8ff284.png"),
                    contentDescription = null,
                    alignment = Alignment.Center,
                    contentScale = ContentScale.FillWidth
                )
                Text("Inside")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/be8ff284.png"),
                    contentDescription = null,
                    alignment = Alignment.Center,
                    contentScale = ContentScale.Inside
                )
                Text("None")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/be8ff284.png"),
                    contentDescription = null,
                    alignment = Alignment.Center,
                    contentScale = ContentScale.None
                )
                Text("FillBounds")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/be8ff284.png"),
                    contentDescription = null,
                    alignment = Alignment.Center,
                    contentScale = ContentScale.FillBounds
                )
            }
            Column(
                modifier = Modifier.width(90.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Crop")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/be8ff284.png"),
                    contentDescription = null,
                    alignment = Alignment.TopStart,
                    contentScale = ContentScale.Crop
                )
                Text("Fit")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/be8ff284.png"),
                    contentDescription = null,
                    alignment = Alignment.TopStart,
                    contentScale = ContentScale.Fit
                )
                Text("FillHeight")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/be8ff284.png"),
                    contentDescription = null,
                    alignment = Alignment.TopStart,
                    contentScale = ContentScale.FillHeight
                )
                Text("FillWidth")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/be8ff284.png"),
                    contentDescription = null,
                    alignment = Alignment.TopStart,
                    contentScale = ContentScale.FillWidth
                )
                Text("Inside")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/be8ff284.png"),
                    contentDescription = null,
                    alignment = Alignment.TopStart,
                    contentScale = ContentScale.Inside
                )
                Text("None")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/be8ff284.png"),
                    contentDescription = null,
                    alignment = Alignment.TopStart,
                    contentScale = ContentScale.None
                )
                Text("FillBounds")
                Image(
                    modifier = Modifier.size(50.dp).rotate(45f).alpha(0.5f).border(1.dp, Color.Red).background(Color.Green),
                    painter = rememberAsyncImagePainter("https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/be8ff284.png"),
                    contentDescription = null,
                    alignment = Alignment.TopStart,
                    contentScale = ContentScale.FillBounds
                )
            }
        }
    }
}

@Composable
private fun ImageLayout() {
    // 分割线与引导语保持一样的宽度
    var leadWidth by remember { mutableStateOf(0F) }

    Column(
        modifier = Modifier.offset(y = 50.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var t by remember { mutableStateOf(0) }
        Row(
            modifier = Modifier.background(Color.Gray)
                .onSizeChanged { leadWidth = it.width.toFloat() }) {
            var imageWidth by remember { mutableStateOf(0f) }
            val painter = rememberAsyncImagePainter(
                "https://wfiles.gtimg.cn/wuji_dashboard/xy/starter/baa91edc.png?t=$t",
                onState = { state ->
                    imageWidth = if (state is AsyncImagePainter.State.Success) {
                        state.painter.intrinsicSize.let { it.width / it.height * 100 }
                    } else {
                        0f
                    }
                }
            )
            Image(
                painter,
                contentDescription = null,
                modifier = Modifier.height(100.dp).width(imageWidth.dp).clickable {
                    ++t
                },
                contentScale = ContentScale.FillHeight
            )
        }
        Box(
            modifier = Modifier.size(100.dp).background(Color.Red).clickable {
                ++t
            }
        )
    }
}