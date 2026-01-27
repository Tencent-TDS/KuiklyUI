package com.tencent.kuikly.demo.pages.jank

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

/**
 * Compose DSL 版本的 Text 性能测试 Demo
 * 用于测试 Text 组件在 Compose DSL 下的渲染性能
 * 
 * 测试内容：
 * - 300 个 Text 组件
 * - 前 100 个：相同文本内容
 * - 后 200 个：不同文本内容
 * - 使用普通 Column 布局（不可滚动）
 */
@Page("TextPerfComposeDemo")
class TextPerfComposeDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            TextPerfComposeContent()
        }
    }
}

/**
 * 生成测试用的文本内容列表
 * @return 包含 300 个文本的列表
 */
private fun generateTextList(): List<String> {
    val textList = mutableListOf<String>()
    
    // 前 100 个相同文本
    repeat(100) { index ->
        textList.add("相同文本内容_${index + 1}")
    }
    
    // 后 200 个不同文本
    repeat(200) { index ->
        val randomSuffix = "ABC${index}XYZ${index * 2}DEF${index * 3}"
        textList.add("不同文本内容_${index + 101}_$randomSuffix")
    }
    
    return textList
}

// 预生成文本列表，避免每次重组时重新生成
private val textDataList = generateTextList()

@Composable
fun TextPerfComposeContent() {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        textDataList.forEach { text ->
            Text(
                text = text,
                fontSize = 14.sp,
                color = Color.Black,
                maxLines = 1
            )
        }
    }
}
