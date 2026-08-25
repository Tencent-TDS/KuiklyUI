package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.module.NetworkModule
import com.tencent.kuikly.core.module.NetworkResponse
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.core.views.layout.Row
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
 * SSE 流式效果 Demo
 * 展示大模型逐 token 输出的效果。
 */
@Page("SSETypewriterDemoPage")
internal class SSETypewriterDemoPage : BasePager() {

    /** 当前已显示的文本 */
    private var displayText by observable("")
    /** 状态文本 */
    private var statusText by observable("就绪")
    /** 是否正在流式请求中 */
    private var isStreaming by observable(false)
    /** 当前流式请求句柄 */
    private var currentHandle: NetworkModule.StreamRequestHandle? = null
    /** 已收到的数据块数量 */
    private var chunkCount by observable(0)
    /** 用户输入的提问内容 */
    private var userPrompt by observable("请用简洁的语言介绍一下你自己，以及你能做什么？")

    companion object {
        // 使用前需填写以下参数
        /** API 地址（兼容 OpenAI 格式） */
        private const val API_URL = "xxx"
        /** API Key */
        private const val API_KEY = "xxx"
        /** 使用的模型 */
        private const val MODEL = "xxx"
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color(0xFFF8F9FAL))
            }

            NavBar {
                attr {
                title = "AI 流式对话"
                }
            }

            // 状态栏
            View {
                attr {
                    marginLeft(12f)
                    marginRight(12f)
                    marginTop(10f)
                    padding(12f)
                    borderRadius(10f)
                    backgroundColor(
                        if (ctx.isStreaming) Color(0xFFE3F2FDL)
                        else if (ctx.statusText.contains("完成")) Color(0xFFE8F5E9L)
                        else if (ctx.statusText.contains("错误")) Color(0xFFFFEBEEL)
                        else Color(0xFFF5F5F5L)
                    )
                }
                Text {
                    attr {
                        text("状态: ${ctx.statusText}")
                        fontSize(13f)
                        color(Color(0xFF666666L))
                    }
                }
            }

            // 输入区域
            View {
                attr {
                    marginLeft(12f)
                    marginRight(12f)
                    marginTop(10f)
                    padding(12f)
                    borderRadius(10f)
                    backgroundColor(Color.WHITE)
                }
                Text {
                    attr {
                        text("输入你的问题：")
                        fontSize(13f)
                        fontWeightBold()
                        color(Color(0xFF333333L))
                        marginBottom(8f)
                    }
                }
                Input {
                    attr {
                        height(80f)
                        alignSelfStretch()
                        backgroundColor(Color(0xFFF5F5F5L))
                        borderRadius(8f)
                        margin(10f)
                        fontSize(14f)
                        color(Color(0xFF333333L))
                        text(ctx.userPrompt)
                        placeholder("请输入...")
                    }
                    event {
                        textDidChange {
                            ctx.userPrompt = it.text
                        }
                    }
                }
            }

            // 按钮区域
            Row {
                attr {
                    marginTop(10f)
                    marginLeft(8f)
                    marginRight(8f)
                }
                // 发送按钮
                Button {
                    attr {
                        flex(1f)
                        height(44f)
                        borderRadius(22f)
                        marginLeft(4f)
                        marginRight(4f)
                        backgroundColor(
                            if (ctx.isStreaming) Color(0xFFBDBDBDL) else Color(0xFF2196F3L)
                        )
                        titleAttr {
                            text("发送")
                            color(Color.WHITE)
                            fontSize(14f)
                        }
                    }
                    event {
                        click {
                            if (!ctx.isStreaming && ctx.userPrompt.isNotBlank()) {
                                ctx.startStream()
                            }
                        }
                    }
                }
                // 停止按钮
                Button {
                    attr {
                        flex(1f)
                        height(44f)
                        borderRadius(22f)
                        marginLeft(4f)
                        marginRight(4f)
                        backgroundColor(
                            if (!ctx.isStreaming) Color(0xFFBDBDBDL) else Color(0xFFF44336L)
                        )
                        titleAttr {
                            text("停止")
                            color(Color.WHITE)
                            fontSize(14f)
                        }
                    }
                    event {
                        click {
                            if (ctx.isStreaming) {
                                ctx.stopStream()
                            }
                        }
                    }
                }
                // 清空按钮
                Button {
                    attr {
                        flex(1f)
                        height(44f)
                        borderRadius(22f)
                        marginLeft(4f)
                        marginRight(4f)
                        backgroundColor(
                            if (ctx.isStreaming) Color(0xFFBDBDBDL) else Color(0xFF757575L)
                        )
                        titleAttr {
                            text("清空")
                            color(Color.WHITE)
                            fontSize(14f)
                        }
                    }
                    event {
                        click {
                            if (!ctx.isStreaming) {
                                ctx.clearAll()
                            }
                        }
                    }
                }
            }

            // AI 回复输出区域
            View {
                attr {
                    marginLeft(12f)
                    marginRight(12f)
                    marginTop(12f)
                    marginBottom(12f)
                    flex(1f)
                    borderRadius(12f)
                    backgroundColor(Color.WHITE)
                }
                // 顶部标题栏
                View {
                    attr {
                        padding(12f)
                        backgroundColor(Color(0xFF424242L))
                        borderRadius(12f, 12f, 0f, 0f)
                    }
                    Text {
                        attr {
                            text("AI回复")
                            fontSize(14f)
                            fontWeightBold()
                            color(Color.WHITE)
                        }
                    }
                }
                // 文本内容区域
                Scroller {
                    attr {
                        padding(16f)
                        flex(1f)
                        alignSelfStretch()
                    }
                    Text {
                        attr {
                            text(
                                if (ctx.displayText.isEmpty() && !ctx.isStreaming)
                                    "输入问题并点击「发送提问"
                                else if (ctx.displayText.isEmpty() && ctx.isStreaming)
                                    "等待回复中..."
                                else
                                    ctx.displayText
                            )
                            fontSize(15f)
                            lineHeight(24f)
                            color(
                                if (ctx.displayText.isEmpty())
                                    Color(0xFF999999L)
                                else
                                    Color(0xFF333333L)
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * 发起大模型流式请求
     */
    private fun startStream() {
        displayText = ""
        chunkCount = 0
        statusText = "正在连接..."
        isStreaming = true

        // 构建请求体（OpenAI 兼容格式）
        val requestBody = JSONObject().apply {
            put("model", MODEL)
            put("stream", true)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "你是人工智能助手。请用中文回答用户的问题。")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userPrompt)
                })
            })
        }

        currentHandle = acquireModule<NetworkModule>(NetworkModule.MODULE_NAME)
            .httpStreamRequest(
                url = API_URL,
                isPost = true,
                param = requestBody,
                headers = JSONObject().apply {
                    put("Authorization", "Bearer $API_KEY")
                    put("Content-Type", "application/json")
                    put("Accept", "text/event-stream")
                },
                timeout = 60
            ) { event, data, response ->
                handleSSEEvent(event, data, response)
            }
    }

    /**
     * 处理 SSE 流式事件
     */
    private fun handleSSEEvent(
        event: String,
        data: String,
        response: NetworkResponse?
    ) {
        when (event) {
            "data" -> {
                chunkCount++
                if (response != null) {
                    statusText = "已连接 (HTTP ${response.statusCode})，接收中..."
                }

                // SSE 返回格式：每行 "data: {json}" 或 "data: [DONE]"
                // NetworkModule 可能一次回调包含多行 SSE 数据
                val lines = data.split("\n")
                for (line in lines) {
                    val trimmed = line.trim()
                    // 处理标准 SSE 格式 "data: ..."
                    val jsonStr = when {
                        trimmed.startsWith("data: ") -> trimmed.removePrefix("data: ").trim()
                        trimmed.startsWith("{") -> trimmed  // 直接是 JSON
                        else -> continue
                    }

                    if (jsonStr == "[DONE]") {
                        // 流结束标记
                        continue
                    }

                    try {
                        val json = JSONObject(jsonStr)
                        val choices = json.optJSONArray("choices")
                        if (choices != null && choices.length() > 0) {
                            val delta = choices.optJSONObject(0)?.optJSONObject("delta")
                            val content = delta?.optString("content", "") ?: ""
                            if (content.isNotEmpty()) {
                                displayText += content
                            }
                        }
                    } catch (_: Exception) {
                        // 解析失败的行跳过（可能是不完整的 JSON 片段）
                    }
                }
            }
            "complete" -> {
                statusText = "回复完成，共收到 $chunkCount 块数据"
                isStreaming = false
                currentHandle = null
            }
            "error" -> {
                statusText = "错误: $data"
                isStreaming = false
                currentHandle = null
            }
        }
    }

    /**
     * 停止流式请求
     */
    private fun stopStream() {
        currentHandle?.close()
        currentHandle = null
        statusText = "已停止，共收到 $chunkCount 块数据"
        isStreaming = false
    }

    /**
     * 清空所有内容
     */
    private fun clearAll() {
        displayText = ""
        chunkCount = 0
        statusText = "就绪"
    }
}