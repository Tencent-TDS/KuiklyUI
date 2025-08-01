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

internal expect fun getPlatform(): String
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
        val promptBoxes = listOf(
            PromptBox("\uD83C\uDF93 高考指南", "请帮我分析高考志愿填报方案，结合我的成绩和兴趣给出建议"),
            PromptBox("\u2600\uFE0F 健康助手", "请告诉我高考期间如何保持身体健康和心理状态"),
            PromptBox("\uD83D\uDCFA 文娱节目单", "请推荐一些适合学生放松的文娱节目或电影"),
            PromptBox("\uD83D\uDCB0 今日金价", "请告诉我今天的黄金价格走势和投资建议"),
        )

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
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 10.dp)
                    ) {
                        items(promptBoxes) { box ->
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .clickable {
                                        inputText = box.prompt
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = box.title,
                                    color = Color.Black,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    // 输入栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val width = if (getPlatform() == "Android") 320 else 300
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .width(width.dp)
                                .weight(1f)
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

                        Spacer(modifier = Modifier.width(10.dp))

                        @OptIn(InternalResourceApi::class)
                        val sendDrawable =
                            DrawableResource(ImageUri.pageAssets(SEND_ICON).toUrl("ChatDemo"))

                        Image(
                            painter = painterResource(sendDrawable),
                            contentDescription = "Send",
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(enabled = inputText.isNotBlank()) {
                                    val messageToSend = inputText
                                    inputText = ""

                                    chatList.add(messageToSend)

                                    /*
                                    GlobalScope.launch {
                                        // Android 和 iOS 通过ktor接口发送并处理接收消息
                                        sendStreamMessage(
                                            client = NetworkClient.client as HttpClient,
                                            url = CHAT_URL,
                                            model = CHAT_MODEL,
                                            apiKey = CHAT_API_KEY,
                                            prompt = messageToSend,
                                            chatList = chatList
                                        )

                                        // OHOS 通过原生的桥接模块发送并处理接收消息
                                        sendOhosMessage(
                                            url = CHAT_URL,
                                            model = CHAT_MODEL,
                                            apiKey = CHAT_API_KEY,
                                            prompt = messageToSend,
                                            chatList = chatList
                                        )
                                    }
                                    */

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
                text = "新闻弟",
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
            @OptIn(InternalResourceApi::class)
            val sendDrawable = DrawableResource(ImageUri.pageAssets(AVATAR).toUrl("ChatDemo"))
            // 第一行：居中的图片
            Image(
                painter = painterResource(sendDrawable),
                contentDescription = "Welcome",
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "了解热点 向我提问",
                fontSize = 24.sp,
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))

            // 第二行：两个并列的 Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable { onInputTextChange("请帮我分析高考志愿填报方案，结合我的成绩和兴趣给出建议") }
                        .padding(vertical = 16.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("\uD83C\uDF93 高考志愿分析", fontSize = 16.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("高考之路，有我护航", fontSize = 14.sp, color = Color(0xFF888888))
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable { onInputTextChange("请帮我分析高考志愿填报方案，结合我的成绩和兴趣给出建议") }
                        .padding(vertical = 16.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("\u26BD 世界杯观赛助手", fontSize = 16.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(" ", fontSize = 14.sp, color = Color(0xFF888888))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 第三行：两个并列的 Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable { onInputTextChange("健康助手prompt") }
                        .padding(vertical = 16.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("\u2600\uFE0F 医学健康助手", fontSize = 16.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("专业、科学", fontSize = 14.sp, color = Color(0xFF888888))
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable { onInputTextChange("新闻妹高考送祝福prompt") }
                        .padding(vertical = 16.dp, horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text("新闻妹高考送祝福", fontSize = 16.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("祝各位考生金榜题名", fontSize = 14.sp, color = Color(0xFF888888))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 第四行：两个并列的 Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        @OptIn(InternalResourceApi::class)
                        val explore = DrawableResource(ImageUri.pageAssets(EXPL_ICON).toUrl("ChatDemo"))
                        Image(
                            painter = painterResource(explore),
                            contentDescription = null,
                            modifier = Modifier
                                .size(16.dp) // 小图标大小
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("发现更多", fontSize = 16.sp, color = Color.Black)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    @OptIn(InternalResourceApi::class)
                    val fresh = DrawableResource(ImageUri.pageAssets(FRSH_ICON).toUrl("ChatDemo"))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(fresh),
                            contentDescription = null,
                            modifier = Modifier
                                .size(16.dp) // 小图标大小
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("换一批", fontSize = 16.sp, color = Color.Black)
                    }
                }
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

    /* 安卓和iOS使用（ktor请求方式）
    private suspend fun sendStreamMessage(
        client: HttpClient,
        url: String,
        model: String,
        apiKey: String,
        prompt: String,
        chatList: MutableList<String>,
    ) {
        try {
            withContext(Dispatchers.Main) {
                chatList.add("")
            }
            val msgIndex = chatList.lastIndex
            var streamingMsg = ""

            withContext(Dispatchers.IO) {
                val response: HttpResponse = client.post(url) {
                    headers {
                        append("Authorization", "Bearer $apiKey")
                        append("Content-Type", "application/json")
                        append("Accept", "text/event-stream")
                    }
                    setBody(
                        """
                        {
                            "model": "$model",
                            "messages": [{"role": "user", "content": "$prompt"}],
                            "stream": true
                        }
                        """.trimIndent()
                    )
                }

                val channel: ByteReadChannel = response.bodyAsChannel()

                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    if (line.startsWith("data:")) {
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") break
                        val delta = extractContentFromDelta(data)
                        if (delta.isNotEmpty()) {
                            streamingMsg += delta
                            withContext(Dispatchers.Main) {
                                chatList[msgIndex] = streamingMsg
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                chatList.add("[出错：${e.message}]")
            }
        }
    }
     */

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
        private const val FRSH_ICON = "ic_fresh.png"
        private const val EXPL_ICON = "ic_explore.png"
        private const val AVATAR = "avatar.jpg"
        private const val PLACEHOLDER = "Type something..."
        private const val CHAT_URL = "https://api.hunyuan.cloud.tencent.com/v1/chat/completions"
        private const val CHAT_MODEL = "hunyuan-turbos-latest"
        private const val CHAT_API_KEY = "<YUANBAO-API-KEY>"
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
    val prompt: String
)
