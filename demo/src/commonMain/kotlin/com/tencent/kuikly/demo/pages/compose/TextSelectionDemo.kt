package com.tencent.kuikly.demo.pages.compose_dsl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose_dsl.kuikly.text.DisableSelection
import com.tencent.kuikly.compose_dsl.kuikly.text.SelectionContainer
import com.tencent.kuikly.compose_dsl.kuikly.text.SelectionFrame
import com.tencent.kuikly.compose_dsl.kuikly.text.TextSelectionType
import com.tencent.kuikly.compose_dsl.kuikly.text.rememberSelectionContainerState
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.pager.Pager

/**
 * Demo page for Compose DSL Text Selection feature
 */
@Page("TextSelectionDemo")
internal class TextSelectionDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            TextSelectionDemoContent()
        }
    }
}

@Composable
private fun TextSelectionDemoContent() {
    var selectionStatus by remember { mutableStateOf("未选择") }
    var selectionFrameText by remember { mutableStateOf("") }
    var selectedText by remember { mutableStateOf("") }
    
    val selectionState = rememberSelectionContainerState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = "Text Selection Demo (Compose)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333333)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Status Panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF5F5F5))
                .padding(12.dp)
        ) {
            Text(
                text = "状态: $selectionStatus",
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
            Text(
                text = "选区: $selectionFrameText",
                fontSize = 12.sp,
                color = Color(0xFF999999)
            )
            Text(
                text = "选中内容: $selectedText",
                fontSize = 12.sp,
                color = Color(0xFF333333)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Select Word Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF4A90D9))
                    .clickable {
                        selectionState.createSelection(150f, 80f, TextSelectionType.WORD)
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "选择单词",
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
            
            // Select Paragraph Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF5AAF6A))
                    .clickable {
                        selectionState.createSelection(150f, 80f, TextSelectionType.PARAGRAPH)
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "选择段落",
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
            
            // Clear Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFE57373))
                    .clickable {
                        selectionState.clearSelection()
                        selectedText = ""
                        selectionFrameText = ""
                        selectionStatus = "已清除"
                    }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "清除选择",
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Get Selection Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF9C27B0))
                .clickable {
                    selectionState.getSelection { texts ->
                        selectedText = texts.joinToString(" | ")
                        if (texts.isEmpty()) {
                            selectionStatus = "无选中内容"
                        }
                    }
                }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "获取选中内容",
                fontSize = 14.sp,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Selectable Text Container
        SelectionContainer(
            state = selectionState,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFFFFDE7))
                .padding(16.dp),
            selectionColor = Color(0xFF2196F3),
            onSelectStart = { frame ->
                selectionStatus = "选择开始"
                selectionFrameText = formatFrame(frame)
            },
            onSelectChange = { frame ->
                selectionStatus = "选择中..."
                selectionFrameText = formatFrame(frame)
            },
            onSelectEnd = { frame ->
                selectionStatus = "选择结束"
                selectionFrameText = formatFrame(frame)
            },
            onSelectCancel = {
                selectionStatus = "选择取消"
                selectionFrameText = ""
                selectedText = ""
            }
        ) {
            Column {
                Text(
                    text = "可选择文本区域",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Kuikly是一个跨平台的UI框架，支持iOS、Android、鸿蒙、Web、小程序等平台。它使用Kotlin Multiplatform技术，让开发者可以使用统一的代码库来构建多平台应用。",
                    fontSize = 15.sp,
                    color = Color(0xFF444444)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "文本选择功能允许用户在多个文本组件之间进行连续选择，支持单词、段落等多种选择模式。选中的文本可以被复制或进行其他操作。",
                    fontSize = 15.sp,
                    color = Color(0xFF444444)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Demonstrate DisableSelection
                DisableSelection {
                    Text(
                        text = "⚠️ 这段文本不可选择 (DisableSelection)",
                        fontSize = 14.sp,
                        color = Color(0xFFE53935),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "The quick brown fox jumps over the lazy dog. This is a classic pangram that contains every letter of the English alphabet.",
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    fontStyle = FontStyle.Italic
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Usage Tips
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE3F2FD))
                .padding(12.dp)
        ) {
            Text(
                text = "使用说明",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1565C0)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "1. 点击上方按钮可以程序化创建选区\n2. 拖动选区两端的游标可以调整选区范围\n3. 点击\"获取选中内容\"可以获取当前选中的文本\n4. DisableSelection内的文本不可选择",
                fontSize = 13.sp,
                color = Color(0xFF1976D2)
            )
        }
    }
}

private fun formatFrame(frame: SelectionFrame): String {
    return "x:${frame.x.toInt()}, y:${frame.y.toInt()}, w:${frame.width.toInt()}, h:${frame.height.toInt()}"
}

