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
import com.tencent.kuikly.core.module.NotifyModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 * Compose DSL 版本的 Text 性能测试 Demo
 * 用于测试 Text 组件在 Compose DSL 下的渲染性能
 * 
 * 测试内容：
 * - 900 个 Text 组件
 * - 前 300 个：相同文本内容
 * - 后 600 个：不同文本内容
 * - 使用普通 Column 布局（不可滚动）
 */
@Page("TextPerfComposeDemo")
class TextPerfComposeDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            TextPerfComposeContent(
                onAllTextLayoutComplete = { total ->
                    // 通过 NotifyModule 通知原生层性能测试结束
                    getPager().acquireModule<NotifyModule>(NotifyModule.MODULE_NAME).postNotify(
                        "TextPerfAllLayoutComplete",
                        JSONObject().put("type", "Compose").put("total", total)
                    )
                }
            )
        }
    }
}

/**
 * 生成测试用的文本内容列表
 * @return 包含 900 个文本的列表
 */
private fun generateTextList(): List<String> {
    val textList = mutableListOf<String>()
    
    // 前 300 个相同文本
    repeat(300) { index ->
        textList.add("相同文本内容_${index + 1}")
    }
    
    // 后 600 个不同文本
    repeat(600) { index ->
        val randomSuffix = "ABC${index}XYZ${index * 2}DEF${index * 3}"
        textList.add("不同文本内容_${index + 301}_$randomSuffix")
    }
    
    return textList
}

// 预生成文本列表，避免每次重组时重新生成
private val textDataList = generateTextList()

@Composable
fun TextPerfComposeContent(onAllTextLayoutComplete: ((Int) -> Unit)? = null) {
    val totalCount = textDataList.size
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        textDataList.forEachIndexed { index, text ->
            val isLast = index == totalCount - 1
            Text(
                text = text,
                fontSize = 14.sp,
                color = Color.Black,
                maxLines = 1,
                onTextLayout = if (isLast) { _ ->
                    // 最后一个 Text 完成布局，打印性能测试结束标记
                    println("[TextPerf][AllTextLayoutComplete] Compose DSL - Total: $totalCount")
                    // 通知原生层性能测试结束
                    onAllTextLayoutComplete?.invoke(totalCount)
                } else null
            )
        }
    }
}
