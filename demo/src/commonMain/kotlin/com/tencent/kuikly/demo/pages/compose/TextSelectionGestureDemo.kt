package com.tencent.kuikly.demo.pages.compose_dsl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.detectTapGestures
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
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
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.draw.shadow
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.text.font.FontStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose_dsl.kuikly.text.DisableSelection
import com.tencent.kuikly.compose_dsl.kuikly.text.SelectionContainer
import com.tencent.kuikly.compose_dsl.kuikly.text.SelectionFrame
import com.tencent.kuikly.compose_dsl.kuikly.text.TextSelectionType
import com.tencent.kuikly.compose_dsl.kuikly.text.rememberSelectionContainerState
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.log.KLog

/**
 * Demo page for Compose DSL Text Selection with gesture support and popup menu.
 * 
 * Features:
 * - Long press to trigger word selection and show menu
 * - Popup menu with "复制" (Copy) and "全选" (Select All) buttons
 * - Click menu or empty area to dismiss selection
 * - Scroll does not dismiss selection
 */
@Page("TextSelectionGestureDemo")
internal class TextSelectionGestureDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            TextSelectionGestureDemoContent()
        }
    }
}

@Composable
private fun TextSelectionGestureDemoContent() {
    val selectionState = rememberSelectionContainerState()
    
    // Menu state
    var showMenu by remember { mutableStateOf(false) }
    var menuPosition by remember { mutableStateOf(SelectionFrame.Zero) }
    var selectedText by remember { mutableStateOf("") }
    var isSelectionActive by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Title section
            item {
                Text(
                    text = "文本选择手势演示",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "长按文本触发选择，弹出操作菜单",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Status display
            if (selectedText.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "已复制内容:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = selectedText,
                            fontSize = 14.sp,
                            color = Color(0xFF1B5E20)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Selectable Text Container with long press gesture
            item {
                SelectionContainer(
                    state = selectionState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFAFAFA))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = { offset ->
                                    // Long press triggers word selection
                                    // Coordinates are in pixels, SelectionContainerState handles conversion
                                    KLog.i("TextSelectionGesture", "Long press at: $offset")
                                    selectionState.createSelection(offset.x, offset.y, TextSelectionType.WORD)
                                },
                                onTap = { offset ->
                                    KLog.i("TextSelectionGesture", "Tap at: $offset")
                                    // Tap on text area dismisses menu if shown
                                    if (showMenu) {
                                        showMenu = false
                                        selectionState.clearSelection()
                                        isSelectionActive = false
                                    }
                                }
                            )
                        }
                        .padding(20.dp),
                    selectionColor = Color(0xFF2196F3),
                    onSelectStart = { frame ->
                        isSelectionActive = true
                        menuPosition = frame
                        showMenu = true
                        KLog.i("TextSelectionGesture", "Selection started: $frame")
                    },
                    onSelectChange = { frame ->
                        menuPosition = frame
                        KLog.i("TextSelectionGesture", "Selection changed: $frame")
                    },
                    onSelectEnd = { frame ->
                        menuPosition = frame
                        KLog.i("TextSelectionGesture", "Selection ended: $frame")
                    },
                    onSelectCancel = {
                        showMenu = false
                        isSelectionActive = false
                        KLog.i("TextSelectionGesture", "Selection cancelled")
                    }
                ) {
                    Column {
                        // Article title
                        Text(
                            text = "关于Kuikly",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Paragraph 1
                        Text(
                            text = "Kuikly是腾讯开源的新一代跨平台框架。它基于Kotlin Multiplatform技术构建，让开发者可以使用一套代码同时开发iOS、Android、鸿蒙、Web和小程序应用。",
                            fontSize = 16.sp,
                            color = Color(0xFF424242),
                            lineHeight = 26.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Paragraph 2
                        Text(
                            text = "与传统跨平台方案不同，Kuikly采用声明式UI开发范式，提供类似Jetpack Compose的开发体验。同时它还支持与原生代码的无缝互操作，既能享受跨平台带来的效率提升，又不牺牲原生能力。",
                            fontSize = 16.sp,
                            color = Color(0xFF424242),
                            lineHeight = 26.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Non-selectable hint
                        DisableSelection {
                            Text(
                                text = "💡 提示：此段文字不可选择",
                                fontSize = 14.sp,
                                color = Color(0xFFFF9800),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Paragraph 3
                        Text(
                            text = "文本选择是Kuikly的重要特性之一。用户可以通过长按触发选择，然后拖动选区边缘调整范围。选中的内容支持复制、全选等操作。",
                            fontSize = 16.sp,
                            color = Color(0xFF424242),
                            lineHeight = 26.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // English text
                        Text(
                            text = "Kuikly brings the best of both worlds: the efficiency of cross-platform development and the power of native capabilities. Try selecting this text!",
                            fontSize = 15.sp,
                            color = Color(0xFF616161),
                            fontStyle = FontStyle.Italic,
                            lineHeight = 24.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Usage tips
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE3F2FD))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "使用说明",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• 长按文本触发单词选择\n• 拖动游标调整选区范围\n• 点击菜单按钮执行操作\n• 点击空白区域或菜单外部取消选择\n• 上下滑动页面不会取消选择",
                        fontSize = 14.sp,
                        color = Color(0xFF1976D2),
                        lineHeight = 22.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(80.dp)) // Extra space for scrolling
            }
        }
        
        // Popup Menu - positioned above selection
        if (showMenu && isSelectionActive) {
            SelectionMenu(
                selectionFrame = menuPosition,
                onCopy = {
                    selectionState.getSelection { texts ->
                        selectedText = texts.joinToString(" ")
                        KLog.i("TextSelectionGesture", "Copied: $selectedText")
                    }
                    showMenu = false
                    selectionState.clearSelection()
                    isSelectionActive = false
                },
                onSelectAll = {
                    selectionState.createSelectionAll()
                    // Keep menu visible after select all
                },
                onDismiss = {
                    showMenu = false
                    selectionState.clearSelection()
                    isSelectionActive = false
                }
            )
        }
    }
}

/**
 * Selection popup menu component
 */
@Composable
private fun SelectionMenu(
    selectionFrame: SelectionFrame,
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    onDismiss: () -> Unit
) {
    // Calculate menu position (above the selection)
    val menuY = (selectionFrame.y - 50).coerceAtLeast(60f)
    val menuX = (selectionFrame.x + selectionFrame.width / 2 - 70).coerceAtLeast(16f)
    
    // Full screen overlay to detect outside clicks
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onDismiss()
            }
    ) {
        // Menu card
        Row(
            modifier = Modifier
                .offset { IntOffset(menuX.toInt(), menuY.toInt()) }
                .shadow(8.dp, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF424242))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    // Prevent click through
                }
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Copy button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onCopy() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "复制",
                    fontSize = 15.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Divider
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(Color(0xFF616161))
            )
            
            // Select All button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onSelectAll() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "全选",
                    fontSize = 15.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
