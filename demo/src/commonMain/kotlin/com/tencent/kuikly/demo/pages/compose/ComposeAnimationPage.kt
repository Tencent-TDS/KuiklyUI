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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.animation.AnimatedVisibility
import com.tencent.kuikly.compose.animation.animateColorAsState
import com.tencent.kuikly.compose.animation.animateContentSize
import com.tencent.kuikly.compose.animation.core.Animatable
import com.tencent.kuikly.compose.animation.fadeIn
import com.tencent.kuikly.compose.animation.fadeOut
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.HorizontalDivider
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.demo.pages.base.BridgeModule
import kotlinx.coroutines.launch

@Page("ComposeAnimationPage")
class ComposeAnimationPage : ComposeContainer() {
    override fun createExternalModules(): Map<String, Module>? {
        val externalModules = hashMapOf<String, Module>()
        externalModules[BridgeModule.MODULE_NAME] = BridgeModule()
        return externalModules
    }

    override fun willInit() {
        super.willInit()

        setContent {
            ComposeNavigationBar {
                LazyColumn(modifier = Modifier.fillMaxSize().offset(y = 88.dp).background(Color.White)) {
                    item { VisibilityAnimation() }
                    item { BackColorAnimation() }
                    item { SizeAnimation1() }
                }
            }
        }
    }

    @Composable
    fun AnimationCell(
        text: String,
        onClick: () -> Unit,
        content: @Composable () -> Unit,
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 50.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            content()
            Button(onClick = onClick) {
                Text(text, fontSize = 20.sp)
            }
        }
        HorizontalDivider()
    }

    @Composable
    fun VisibilityAnimation() {
        var visible by remember {
            mutableStateOf(true)
        }

        AnimationCell("点击" + if (visible) "隐藏" else "显示", onClick = {
            visible = !visible
        }) {
            AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
                Box(modifier = Modifier.size(100.dp).background(Color.Green))
            }
        }
    }

    @Composable
    fun BackColorAnimation() {
        var animateBackgroundColor by remember { mutableStateOf(false) }
        val animatedColor by animateColorAsState(
            if (animateBackgroundColor) Color.Green else Color.Blue,
            label = "color",
        )

        AnimationCell("点击改变颜色", onClick = {
            animateBackgroundColor = !animateBackgroundColor
        }) {
            Box(modifier = Modifier.size(100.dp).background(animatedColor)) { }
        }
    }

    @Composable
    fun SizeAnimation() {
        var expanded by remember { mutableStateOf(false) }

        AnimationCell("点击改变大小", onClick = {
            expanded = !expanded
        }) {
            Box(
                modifier =
                    Modifier
                        .height(100.dp)
                        .background(Color.Yellow)
                        .animateContentSize()
                        .width(if (expanded) 200.dp else 100.dp),
            ) { }
        }
    }

    @Composable
    fun SizeAnimation1() {
        val width =
            remember {
                Animatable(100f)
            }

        val scope = rememberCoroutineScope()
        AnimationCell("点击改变大小", onClick = {
            scope.launch {
                if (width.value == 200f) {
                    width.animateTo(100f)
                } else {
                    width.animateTo(200f)
                }
            }
        }) {
            println("xxxx animation reSize, width: ${width.value}")
            Box(
                modifier =
                    Modifier
                        .height(100.dp)
                        .background(Color.Yellow)
                        .width(width.value.dp),
            ) { }
        }
    }
}
