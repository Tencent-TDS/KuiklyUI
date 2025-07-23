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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.ColumnScope
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Page("coroutines")
internal class CoroutinesDemoPage : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                Column(modifier = Modifier.fillMaxSize()) {
                    Case0()
                    Case1()
                    Case2()
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.Case0() {
    var count by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        try {
            while (true) {
                delay(1000)
                println("Case0: increment count $count")
                count++
            }
        } finally {
            println("Case0: finally")
        }
    }
    Box(
        Modifier.size(100.dp).border(1.dp, Color.Black)
            .align(Alignment.CenterHorizontally)
    ) {
        println("Case0: recompose")
        Text(
            text = "${count % 100}",
            fontSize = 64.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun ColumnScope.Case1() {
    var count by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        try {
            while (true) {
                withContext(Dispatchers.IO) {
                    delay(1000)
                }
                println("Case1: increment count $count")
                count++
            }
        } finally {
            println("Case1: finally")
        }
    }
    Box(
        Modifier.size(100.dp).border(1.dp, Color.Black)
            .align(Alignment.CenterHorizontally)
    ) {
        println("Case1: recompose")
        Text(
            text = "${count % 100}",
            fontSize = 64.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun ColumnScope.Case2() {
    var count by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        try {
            withContext(Dispatchers.IO) {
                while (true) {
                    delay(1000)
                    println("Case2: increment count $count")
                    count++
                }
            }
        } finally {
            println("Case2: finally")
        }
    }
    Box(
        Modifier.size(100.dp).border(1.dp, Color.Black)
            .align(Alignment.CenterHorizontally)
    ) {
        println("Case2: recompose")
        Text(
            text = "${count % 100}",
            fontSize = 64.sp,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

internal expect val Dispatchers.IO : CoroutineDispatcher