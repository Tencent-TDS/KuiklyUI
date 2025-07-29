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
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("MarginPaddingTest")
class MarginPaddingTestDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                var paddingValue by remember { mutableStateOf(8) }
                var marginValue by remember { mutableStateOf(16) }
                var cornerRadius by remember { mutableStateOf(4) }

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.LightGray.copy(alpha = 0.3f))
                            .padding(16.dp),
                ) {
                    // 标题
                    item {
                        Text("内外边距测试示例", fontSize = 20.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        Text("Compose中使用Modifier实现各种内外边距效果", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // 1. 基础内边距 padding 示例
                    item {
                        SectionTitle("1. 内边距 先background再Size和padding")
                        Box(
                            modifier =
                                Modifier
                                    .background(Color.Blue)
                                    .padding(16.dp), // 内边距
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(100.dp)
                                        .background(Color.White),
                            ) {
                                Text("内边距16dp", color = Color.Black)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // 2. 模拟外边距 margin 效果（使用 padding 实现）
                    item {
                        SectionTitle("2. 外边距 先padding，再size和backgound")
                        Box(
                            modifier =
                                Modifier
                                    .background(Color.LightGray),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .padding(16.dp) // padding放到size和background的前边;
                                        .size(100.dp)
                                        .background(Color.Red),
                            ) {
                                Text("'外边距'16dp", color = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // 新增: 先size再padding
                    item {
                        SectionTitle("2.1 先Size再padding")
                        Box(
                            modifier =
                                Modifier
                                    .size(150.dp)
                                    .background(Color.Red)
                                    .padding(16.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color.White),
                            ) {
                                Text("先size再padding", color = Color.Black)
                            }
                        }
                        Text("注意: 先设置size再设置padding会导致内容区域缩小", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // 新增: 不设置尺寸的外边距示例
                    item {
                        SectionTitle("2.2 不设置尺寸的外边距")
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(Color.Gray),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .padding(16.dp) // 作为外边距
                                        .fillMaxWidth() // 只指定宽度
                                        .background(Color.Cyan)
                                        .padding(8.dp),
                            ) {
                                Text("未指定完整尺寸的外边距", color = Color.Black)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // 新增: offset 与 padding 对比
                    item {
                        SectionTitle("2.3 offset与padding对比")
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(Color.LightGray)
                                    .padding(8.dp),
                        ) {
                            Text("使用 padding 作为外边距：", fontSize = 14.sp, color = Color.Black)
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .background(Color.DarkGray),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .padding(16.dp) // 作为外边距
                                            .size(50.dp)
                                            .background(Color.Blue),
                                ) {
                                    Text("padding", color = Color.White, fontSize = 10.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("使用 offset 偏移：", fontSize = 14.sp, color = Color.Black)
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(80.dp)
                                        .background(Color.DarkGray),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .offset(x = 16.dp, y = 16.dp) // 使用offset
                                            .size(50.dp)
                                            .background(Color.Red),
                                ) {
                                    Text("offset", color = Color.White, fontSize = 10.sp)
                                }
                            }

                            Text("注意: offset不会改变布局空间，而padding会占用空间", fontSize = 12.sp, color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // 3. 嵌套外边距
                    item {
                        SectionTitle("3. 嵌套外边距")
                        Box(
                            modifier =
                                Modifier
                                    .background(Color.Yellow)
                                    .padding(8.dp), // 外层容器的内边距，相当于内部元素的外边距
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .background(Color.Blue)
                                        .padding(8.dp), // 中层容器的内边距，相当于最内层元素的外边距
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .padding(8.dp) // 最内层元素的"外边距"（通过padding实现）
                                            .size(80.dp)
                                            .background(Color.White),
                                ) {
                                    Text("嵌套外边距", color = Color.Black, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "说明：在Compose中，通常用父容器的padding实现子元素的margin效果",
                            fontSize = 12.sp,
                            color = Color.Gray,
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // 新增: 四个方向不同的边距示例
                    item {
                        SectionTitle("3.1 四个方向不同的边距")
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(Color.Yellow),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .padding(start = 8.dp, top = 24.dp, end = 32.dp, bottom = 12.dp)
                                        .background(Color.Green)
                                        .fillMaxWidth()
                                        .height(80.dp),
                            ) {
                                Text(
                                    "四个方向不同的边距\nstart: 8dp\ntop: 24dp\nend: 32dp\nbottom: 12dp",
                                    color = Color.White,
                                    modifier = Modifier.padding(4.dp),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // 4. 嵌套内边距
                    item {
                        SectionTitle("4. 嵌套内边距")
                        Box(
                            modifier =
                                Modifier
                                    .background(Color.Yellow)
                                    .padding(8.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .background(Color.Blue)
                                        .padding(8.dp),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(80.dp)
                                            .background(Color.White),
                                ) {
                                    Text("嵌套内边距", color = Color.Black, fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // 5. Padding与Border结合
                    item {
                        SectionTitle("5. 内边距与边框结合")
                        Column {
                            // 外边框，内padding
                            Box(
                                modifier =
                                    Modifier
                                        .background(Color.LightGray)
                                        .border(2.dp, Color.Blue)
                                        .padding(8.dp)
                                        .size(100.dp),
                            ) {
                                Text("内边距，边框", color = Color.Black, fontSize = 12.sp)
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 先padding后边框
                            Box(
                                modifier =
                                    Modifier
                                        .padding(8.dp)
                                        .border(2.dp, Color.Red)
                                        .size(100.dp)
                                        .background(Color.LightGray),
                            ) {
                                Text("外边距，边框", color = Color.Black, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // 新增: fillMaxSize与padding结合
                    item {
                        SectionTitle("5.1 fillMaxSize与padding结合")
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .background(Color.DarkGray)
                                    .padding(12.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize() // 填充父容器可用空间（已减去padding）
                                        .background(Color.Cyan),
                            ) {
                                Text(
                                    "fillMaxSize会填充padding后的可用空间",
                                    color = Color.Black,
                                    modifier = Modifier.align(Alignment.Center),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // 6. 组合布局中的内边距
                    item {
                        SectionTitle("6. 组合布局中的内边距")
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(Color.LightGray)
                                    .padding(8.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(60.dp)
                                        .background(Color.Red)
                                        .padding(4.dp),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .background(Color.White),
                                ) {
                                    Text("项目1", color = Color.Black)
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp)) // 作为两个元素间的间距

                            Box(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .height(60.dp)
                                        .background(Color.Blue)
                                        .padding(4.dp),
                            ) {
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .background(Color.White),
                                ) {
                                    Text("项目2", color = Color.Black)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // 7. 带圆角的内边距效果
                    item {
                        SectionTitle("7. 带圆角的内边距效果")
                        Box(
                            modifier =
                                Modifier
                                    .background(Color.DarkGray, shape = RoundedCornerShape(8.dp))
                                    .padding(12.dp), // 内边距在圆角内部
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(100.dp)
                                        .background(Color.White),
                            ) {
                                Text("圆角内边距", color = Color.Black)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // 8. 使用Spacer作为margin
                    item {
                        SectionTitle("8. 使用Spacer作为间距")
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .background(Color.LightGray),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .background(Color.Blue),
                            ) {
                                Text("顶部元素", color = Color.White, modifier = Modifier.align(Alignment.Center))
                            }

                            Spacer(modifier = Modifier.height(16.dp)) // 作为竖直方向的间距

                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .background(Color.Red),
                            ) {
                                Text("底部元素", color = Color.White, modifier = Modifier.align(Alignment.Center))
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // 9. 模拟卡片设计
                    item {
                        SectionTitle("9. 卡片设计中的内外边距")
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp) // 外部间距
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(16.dp), // 内部内边距
                        ) {
                            Column {
                                Text("卡片标题", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("卡片内容，这是一个使用内外边距组合的卡片设计示例，展示了如何在实际UI设计中应用内外边距。", fontSize = 14.sp, color = Color.Gray)

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.Blue.copy(alpha = 0.1f))
                                                .clickable { }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Text("确定", color = Color.Blue, fontSize = 14.sp)
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(
                                        modifier =
                                            Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color.Red.copy(alpha = 0.1f))
                                                .clickable { }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Text("取消", color = Color.Red, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // 10. 使用clip和padding配合
                    item {
                        SectionTitle("10. 圆形裁剪与内边距配合")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .padding(12.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(Color.Blue)
                                        .padding(12.dp), // 这个padding是在圆形内部
                            ) {
                                // 头像内容
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxSize()
                                            .background(Color.LightGray),
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text("用户名", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text("这是一个用户信息卡片示例", fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // 11. 动态调整内边距示例
                    item {
                        SectionTitle("11. 动态调整内外边距")
                        Text("使用加减按钮调整内外边距大小", fontSize = 14.sp, color = Color.Gray)

                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                        ) {
                            // 内边距控制
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("内边距值: ${paddingValue}dp", modifier = Modifier.weight(1f))

                                ValueAdjuster(
                                    value = paddingValue,
                                    onValueChange = { paddingValue = it },
                                    minValue = 0,
                                    maxValue = 48,
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 外边距控制
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("外边距值: ${marginValue}dp", modifier = Modifier.weight(1f))

                                ValueAdjuster(
                                    value = marginValue,
                                    onValueChange = { marginValue = it },
                                    minValue = 0,
                                    maxValue = 48,
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 圆角控制
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("圆角大小: ${cornerRadius}dp", modifier = Modifier.weight(1f))

                                ValueAdjuster(
                                    value = cornerRadius,
                                    onValueChange = { cornerRadius = it },
                                    minValue = 0,
                                    maxValue = 24,
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // 动态调整外边距（父容器padding）
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .background(Color.LightGray)
                                        .padding(marginValue.dp), // 动态外边距
                            ) {
                                // 动态调整内边距和圆角
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(cornerRadius.dp))
                                            .background(Color.Blue)
                                            .padding(paddingValue.dp), // 动态内边距
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(80.dp)
                                                .background(Color.White),
                                    ) {
                                        Column(
                                            modifier =
                                                Modifier
                                                    .fillMaxSize()
                                                    .padding(8.dp),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Text(
                                                "动态内边距: ${paddingValue}dp",
                                                fontSize = 14.sp,
                                                color = Color.Black,
                                            )
                                            Text(
                                                "动态外边距: ${marginValue}dp",
                                                fontSize = 14.sp,
                                                color = Color.Black,
                                            )
                                            Text(
                                                "动态圆角: ${cornerRadius}dp",
                                                fontSize = 14.sp,
                                                color = Color.Black,
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 重置按钮
                            Button(
                                onClick = {
                                    paddingValue = 8
                                    marginValue = 16
                                    cornerRadius = 4
                                },
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                            ) {
                                Text("重置所有值")
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun SectionTitle(title: String) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }

    @Composable
    private fun ValueAdjuster(
        value: Int,
        onValueChange: (Int) -> Unit,
        minValue: Int = 0,
        maxValue: Int = 100,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 减小按钮
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (value > minValue) Color.Blue else Color.Gray)
                        .clickable(enabled = value > minValue) {
                            if (value > minValue) onValueChange(value - 1)
                        },
                contentAlignment = Alignment.Center,
            ) {
                Text("-", color = Color.White, fontSize = 18.sp)
            }

            // 数值显示
            Box(
                modifier =
                    Modifier
                        .width(50.dp)
                        .height(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$value",
                    fontSize = 16.sp,
                    color = Color.Black,
                )
            }

            // 增加按钮
            Box(
                modifier =
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (value < maxValue) Color.Blue else Color.Gray)
                        .clickable(enabled = value < maxValue) {
                            if (value < maxValue) onValueChange(value + 1)
                        },
                contentAlignment = Alignment.Center,
            ) {
                Text("+", color = Color.White, fontSize = 18.sp)
            }
        }
    }
} 
