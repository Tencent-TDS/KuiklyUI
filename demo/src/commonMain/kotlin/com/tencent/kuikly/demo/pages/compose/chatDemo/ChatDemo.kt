package com.tencent.kuikly.demo.pages.compose.chatDemo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.extension.keyboardHeightChange
import com.tencent.kuikly.compose.foundation.Canvas
import com.tencent.kuikly.compose.foundation.Image
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
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.layout.widthIn
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.lazy.itemsIndexed
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TextField
import com.tencent.kuikly.compose.material3.TextFieldDefaults
import com.tencent.kuikly.compose.resources.DrawableResource
import com.tencent.kuikly.compose.resources.InternalResourceApi
import com.tencent.kuikly.compose.resources.painterResource
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Path
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.coroutines.GlobalScope
import com.tencent.kuikly.core.coroutines.launch
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuiklybase.markdown.compose.Markdown
import com.tencent.kuiklybase.markdown.model.rememberMarkdownState
import kotlinx.coroutines.delay

internal expect object NetworkClient {
    val client: Any?
}

@Page("ChatDemo")
internal class ChatDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            ChatScreen()
        }
    }

    @Composable
    internal fun ChatScreen() {
        var inputText by remember { mutableStateOf("") }
        val chatList = remember { mutableStateListOf<String>() }
        var keyboardHeight by remember { mutableStateOf(0f) }

        // 聊天列表滚动状态
        val listState = rememberLazyListState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF4F4FE))
        ) {
            // 顶部导航栏区（固定）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
            ) {
                // 状态栏占位
                Spacer(modifier = Modifier.height(pagerData.statusBarHeight.dp))

                // 导航栏
                NavBar(onBack = {
                    getPager().acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
                })

                // 聊天列表
                if (chatList.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        state = listState
                    ) {
                        itemsIndexed(chatList) { index, message ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                horizontalArrangement = if (index % 2 == 0) Arrangement.End else Arrangement.Start
                            ) {
                                ChatMessageItem(
                                    message = message,
                                    isUser = (index % 2 == 0),
                                    maxWidth = (0.7f * pagerData.pageViewWidth).dp
                                )
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(1.dp))
                        }
                    }
                    LaunchedEffect(chatList.size) {
                        if (chatList.isNotEmpty()) {
                            listState.animateScrollToItem(chatList.size)
                        }
                    }
                } else {
                    welcome(
                        onInputTextChange = { inputText = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(bottom = keyboardHeight.dp)
                ) {

                    // 输入栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            TextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                modifier = Modifier
                                    .padding(end = 40.dp) // 给右侧按钮留出空间
                                    .fillMaxWidth()
                                    .keyboardHeightChange {
                                        keyboardHeight = it.height
                                    },
                                placeholder = { Text(PLACEHOLDER) },
                                shape = RoundedCornerShape(16.dp),
                                colors = TextFieldDefaults.colors(
                                    unfocusedContainerColor = Color.White,
                                    focusedContainerColor = Color.White
                                )
                            )
                        }


                        Spacer(modifier = Modifier.width(10.dp))

                        @OptIn(InternalResourceApi::class)
                        val sendDrawable =
                            DrawableResource(ImageUri.pageAssets(SEND_ICON).toUrl("ChatDemo"))

                        Image(
                            painter = painterResource(sendDrawable),
                            contentDescription = "Send",
                            modifier = Modifier
                                .size(30.dp)
                                .clickable(enabled = inputText.isNotBlank()) {
                                    val messageToSend = inputText
                                    inputText = ""
                                    chatList.add(messageToSend)
                                    GlobalScope.launch {
                                        chatList.add("")
                                        markdown.forEachIndexed { index, _ ->
                                            delay(16)
                                            chatList[chatList.lastIndex] =
                                                markdown.substring(0, index + 1)
                                        }
                                    }
                                }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun NavBar(onBack: () -> Unit) {
        // 顶部导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            @OptIn(InternalResourceApi::class)
            val drawable = DrawableResource(ImageUri.pageAssets(BACK_ICON).toUrl("ChatDemo"))
            Image(
                painter = painterResource(drawable),
                contentDescription = "Back",
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "AI Chat",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(modifier = Modifier.width(20.dp))
        }

        // 横线分割线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFE3E3E3))
        )
    }

    @Composable
    fun welcome(onInputTextChange: (String) -> Unit,
                modifier: Modifier = Modifier) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Kuikly logo
            @OptIn(InternalResourceApi::class)
            val logoDrawable = DrawableResource(ImageUri.pageAssets(LOGO_ICON).toUrl("ChatDemo"))
            Image(
                painter = painterResource(logoDrawable),
                contentDescription = "Kuikly Logo",
                modifier = Modifier
                    .width(240.dp)
                    .height(70.dp)
            )
            
            Spacer(modifier = Modifier.height(40.dp))

            // 使用循环生成卡片 - 艺术化设计
            val promptBoxes = listOf(
                PromptBox(
                    "\uD83C\uDF93 高考志愿分析",
                    "请帮我分析高考志愿填报方案，结合我的成绩和兴趣给出建议",
                    "高考之路，有我护航",
                    Color(0xFFCDC4BB)
                ),
                PromptBox(
                    "\u26BD 世界杯观赛助手",
                    "分析今天的世界杯战况如何",
                    "分析比赛战况",
                    Color(0xFFFEE1D3)
                ),
                PromptBox(
                    "\u2600\uFE0F 医学健康助手",
                    "请给出健康生活建议",
                    "专业、科学",
                    Color(0xFFF6BEBD)
                ),
                PromptBox(
                    "\uD83C\uDF89 高考送祝福",
                    "请写一段高考祝福语，祝考生金榜题名",
                    "祝各位考生金榜题名",
                    Color(0xFFCFAAA1)
                ),
                PromptBox(
                    "\uD83D\uDCDA 学习计划助手",
                    "帮我制定一个高效的学习计划，提升学习效率",
                    "科学规划，高效学习",
                    Color(0xFFD4E4F7)
                ),
                PromptBox(
                    "\uD83C\uDFA8 创意写作助手",
                    "帮我写一篇富有创意的短文或故事",
                    "激发灵感，妙笔生花",
                    Color(0xFFE8D5F2)
                )
            )
            // kuikly logo

            promptBoxes.forEachIndexed { _, box ->
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                com.tencent.kuikly.compose.ui.graphics.Brush.Companion.horizontalGradient(
                                    colors = listOf(box.startColor, box.endColor)
                                )
                            )
                            .clickable { onInputTextChange(box.prompt) }
                            .padding(vertical = 16.dp, horizontal = 18.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = box.title,
                                fontSize = 20.sp,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = box.subtitle,
                                fontSize = 15.sp,
                                color = Color.Black.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

        }
    }


    @Composable
    fun ChatMessageItem(
        message: String,
        isUser: Boolean,
        maxWidth: Dp
    ) {
        if (isUser) {
            Box(
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .padding(bottom = 4.dp, end = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFFE9E9EB),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = message,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    }
                    // 右侧三角
                    Canvas(
                        modifier = Modifier
                            .size(6.dp, 12.dp)
                            .align(Alignment.CenterVertically)
                    ) {
                        val width = size.width
                        val height = size.height
                        val path = Path().apply {
                            moveTo(0f, 0f)              // Box 右侧边的上点
                            lineTo(0f, height)             // Box 右侧边的下点
                            lineTo(width, height / 2f)     // 三角顶点
                            close()
                        }
                        drawPath(
                            path = path,
                            color = Color(0xFFE9E9EB)
                        )
                    }
                }
            }
        } else {
            val markdownState = rememberMarkdownState()
            LaunchedEffect(message) {
                markdownState.parse(message, false)
            }
            Markdown(
                state = markdownState,
                colors = markdownColor(text = Color.Black),
                typography = markdownTypography(),
                modifier = Modifier
                    .widthIn(max = pagerData.pageViewWidth.dp)
                    .padding(horizontal = 24.dp)
            )
        }
    }

    // ohos使用（原生模块方式）
    private fun sendOhosMessage(
        url: String,
        model: String,
        apiKey: String,
        prompt: String,
        chatList: MutableList<String>
    ) {
        chatList.add(prompt)
        chatList.add("")
        val msgIndex = chatList.lastIndex
        println("prompt: $prompt")

        getPager().acquireModule<OhosStreamRequestModule>(OhosStreamRequestModule.MODULE_NAME)
            .request(url, model, apiKey, prompt) { event ->

                when (event?.optString("event")) {
                    "data" -> {
                        // ArkTS端每次推送一段流式内容
                        val delta = extractContentFromDelta(event.optString("data"))
                        if (delta.isNotEmpty()) {
                            chatList[msgIndex] = chatList[msgIndex] + delta
                            println(chatList[msgIndex])
                        }
                    }
                    "error" -> {
                        chatList.add("[出错：${event.optString("data")}]")
                    }
                }
            }
    }

    private fun extractContentFromDelta(delta: String): String {
        val json = JSONObject(delta)
        val choices = json.optJSONArray("choices")
        if (choices != null && choices.length() > 0) {
            val firstChoice = choices.optJSONObject(0)
            val deltaObj = firstChoice?.optJSONObject("delta")
            if (deltaObj != null) {
                return deltaObj.optString("content", "")
            }
        }
        return ""
    }


    companion object {
        private const val BACK_ICON = "ic_back.png"
        private const val SEND_ICON = "ic_send.png"
        private const val LOGO_ICON = "kuikly_logo.png"

        private const val PLACEHOLDER = "Type something..."
        private val markdown = """
            # 一级标题
            ## 二级标题
            这是一段模拟AI回复的markdown文本，**这是一段AI回复的加粗markdown文本**
            *这是一段模拟AI回复的斜体markdown文本*

            ~~这是一段模拟AI回复的删除线markdown文本~~
            > 这是一段AI引用的markdown文本

            | 列1         |  列2  |
            |------------|-----------|
            | 数据1 [1](@ref) | 数据2 |
            | 示例A        | 示例B |
            | 测试1        | 测试2 |
            | 临时A        | 临时B |

            这是一段AI回复的无序列表:
            - 项目1
        """.trimIndent()
    }

}

internal data class PromptBox(
    val title: String,
    val prompt: String,
    val subtitle: String = "",
    val startColor: Color = Color.White,
    val endColor: Color = Color.White
)
