package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.hoverable
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.tencent.kuikly.compose.foundation.interaction.collectIsHoveredAsState
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.draw.shadow
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.input.pointer.PointerIcon
import com.tencent.kuikly.compose.ui.input.pointer.pointerHoverIcon
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("HoverDemo")
class HoverDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("macOS Hover 事件测试") {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // 场景：hoverable + pointerHoverIcon
                    item {
                        Text("场景：文字悬停选中态 + 手指光标", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("hoverable + pointerHoverIcon(PointerIcon.Hand)", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        HoverSelectStateDemo()
                    }

                    // 测试 1：基础 hoverable
                    item {
                        Text("测试 1：基础 hoverable 状态检测", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        BasicHoverTest()
                    }

                    // 测试 2：hoverable + 阴影
                    item {
                        Text("测试 2：Hover + 阴影效果", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        HoverShadowTest()
                    }

                    // 测试 3：多区域 hoverable
                    item {
                        Text("测试 3：多个 Hover 区域", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        MultiHoverTest()
                    }

                    // 测试 4：各种 PointerIcon 光标样式
                    item {
                        Text("测试 4：PointerIcon 光标样式", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("使用 pointerHoverIcon(PointerIcon.XXX)", fontSize = 14.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        PointerIconStyleTest()
                    }

                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }

    @Composable
    fun HoverSelectStateDemo() {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HoverableTextItem("我的电影清单")
            HoverableTextItem("产品设置")
            HoverableTextItem("版本管理")
        }
    }

    @Composable
    fun HoverableTextItem(text: String) {
        val interactionSource = remember { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .hoverable(interactionSource)
                .pointerHoverIcon(PointerIcon.Hand)
                .background(if (isHovered) Color(0xFFE8E8E8) else Color.Transparent)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = if (isHovered) FontWeight.Medium else FontWeight.Normal,
                color = Color(0xFF333333)
            )
        }
    }

    @Composable
    fun BasicHoverTest() {
        val interactionSource = remember { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()
        Box(
            modifier = Modifier
                .size(120.dp, 80.dp)
                .clip(RoundedCornerShape(8.dp))
                .hoverable(interactionSource)
                .background(if (isHovered) Color(0xFF2196F3) else Color(0xFFBBDEFB))
                .border(2.dp, if (isHovered) Color(0xFF1565C0) else Color(0xFF90CAF9), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isHovered) "Hovered!" else "Move here",
                color = if (isHovered) Color.White else Color(0xFF1565C0),
                fontWeight = FontWeight.Bold
            )
        }
    }

    @Composable
    fun HoverShadowTest() {
        val interactionSource = remember { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .hoverable(interactionSource)
                .shadow(
                    elevation = if (isHovered) 8.dp else 1.dp,
                    shape = RoundedCornerShape(12.dp)
                )
                .background(if (isHovered) Color(0xFFE3F2FD) else Color.White)
                .padding(16.dp),
        ) {
            Column {
                Text("可悬停卡片", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                Spacer(modifier = Modifier.height(4.dp))
                Text("鼠标移入查看阴影和背景效果变化", fontSize = 14.sp, color = Color(0xFF666666))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isHovered) "状态: 悬停中" else "状态: 未悬停",
                    fontSize = 12.sp,
                    color = if (isHovered) Color(0xFF4CAF50) else Color(0xFF999999)
                )
            }
        }
    }

    @Composable
    fun MultiHoverTest() {
        val colors = listOf(
            Color(0xFFE91E63) to Color(0xFFF8BBD0),
            Color(0xFF4CAF50) to Color(0xFFC8E6C9),
            Color(0xFFFF9800) to Color(0xFFFFE0B2),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            colors.forEachIndexed { index, (activeColor, inactiveColor) ->
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .hoverable(interactionSource)
                        .background(if (isHovered) activeColor else inactiveColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHovered) Color.White else activeColor
                    )
                }
            }
        }
    }

    @Composable
    fun PointerIconStyleTest() {
        val iconTypes = listOf(
            PointerIcon.Hand to "Hand（手指）",
            PointerIcon.Text to "Text（文本选择）",
            PointerIcon.Crosshair to "Crosshair（十字准心）",
            PointerIcon.Grab to "Grab（抓手）",
            PointerIcon.NotAllowed to "NotAllowed（禁止）",
            PointerIcon.Default to "Default（默认箭头）",
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            iconTypes.forEach { (icon, label) ->
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .hoverable(interactionSource)
                        .pointerHoverIcon(icon)
                        .background(if (isHovered) Color(0xFFE0E0E0) else Color(0xFFF5F5F5))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = "$label  →  pointerHoverIcon(PointerIcon.${label.substringBefore("（")})",
                        fontSize = 14.sp,
                        color = Color(0xFF333333)
                    )
                }
            }
        }
    }
}
