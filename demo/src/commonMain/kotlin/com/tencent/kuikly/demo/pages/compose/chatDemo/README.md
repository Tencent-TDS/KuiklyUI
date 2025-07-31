<div align = center>

# AI Chat Demo

</div>

## Demo Showcase

<div style="display: flex; flex-wrap: wrap; justify-content: center; gap: 20px;">
    <img src="/img/chatdemo_android.png" width=150 />
    <img src="/img/chatdemo_ios.png" width=150 />
    <img src="/img/chatdemo_ohos.jpg" width=150 />
</div>

## API Key Application

This demo uses Tencent Hunyuan Large Model as the conversational model, and sends requests using an OpenAI-compatible interface. For details on how to apply for an API Key, please refer to [this link](https://cloud.tencent.com/document/product/1729/111008). The main steps include:

- After registering and completing personal or enterprise authentication, log in to [Tencent Cloud](https://cloud.tencent.com/).
- The Tencent Hunyuan Large Model API is now open to the public. Go to the [console](https://console.cloud.tencent.com/hunyuan/settings) to enable the service.
- Go to [Console > Start Access](https://console.cloud.tencent.com/hunyuan/start), select the OpenAI SDK access method, and click to create an API KEY.

After obtaining the API Key, fill the key into the `companion object` section in the `ChatDemo.kt` file:

```Kotlin
companion object {
    ...
    private const val CHAT_API_KEY = "<YUANBAO-API-KEY>"
    ...
}
```

Of course, you can also choose to integrate other models that are compatible with the OpenAI API. In addition to changing the `CHAT_API_KEY`, you will also need to modify the `CHAT_URL` and `CHAT_MODEL` values.

```Kotlin
companion object {
    ...
    private const val CHAT_URL = "https://URL/v1/chat/completions"
    private const val CHAT_MODEL = "model"
    private const val CHAT_API_KEY = "<YUANBAO-API-KEY>"
    ...
}
```

## Compilation on Different Platforms

Since the AI request implementation differs across platforms in this demo: Android and iOS use ktor to implement SSE network requests, while HarmonyOS uses native methods for SSE network requests. Therefore, to run this demo, you need to modify some files (if not modified, only a default Markdown text will be returned).

### Android and iOS

- First, uncomment the following function and import the corresponding ktor package:

```Kotlin
/* For Android and iOS (ktor request method) */
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
            chatList.add("[Error：${e.message}]")
        }
    }
}
```

- At line 220 in `ChatDemo.kt`, uncomment the Android and iOS request logic:

```Kotlin
GlobalScope.launch {
    // For Android and iOS, send and process messages via ktor interface
    sendStreamMessage(
        client = NetworkClient.client as HttpClient,
        url = CHAT_URL,
        model = CHAT_MODEL,
        apiKey = CHAT_API_KEY,
        prompt = messageToSend,
        chatList = chatList
    )

    /* For OHOS, send and process messages via native bridge module
    sendOhosMessage(
        url = CHAT_URL,
        model = CHAT_MODEL,
        apiKey = CHAT_API_KEY,
        prompt = messageToSend,
        chatList = chatList
    ) */
}

/* Comment out the default simulated reply text
GlobalScope.launch {
    chatList.add("")
    markdown.forEachIndexed { index, _ ->
        delay(16)
        chatList[chatList.lastIndex] =
            markdown.substring(0, index + 1)
    }
} */
```

- Re-sync and then build and run.

### HarmonyOS

- Comment out the `sendStreamMessage` function for Android and iOS and the ktor-related imports.
- At line 220 in `ChatDemo.kt`, uncomment the HarmonyOS request logic:

```Kotlin
GlobalScope.launch {
    /* For Android and iOS, send and process messages via ktor interface
    sendStreamMessage(
        client = NetworkClient.client as HttpClient,
        url = CHAT_URL,
        model = CHAT_MODEL,
        apiKey = CHAT_API_KEY,
        prompt = messageToSend,
        chatList = chatList
    ) */

    // For OHOS, send and process messages via native bridge module
    sendOhosMessage(
        url = CHAT_URL,
        model = CHAT_MODEL,
        apiKey = CHAT_API_KEY,
        prompt = messageToSend,
        chatList = chatList
    )
}

/* Comment out the default simulated reply text
GlobalScope.launch {
    chatList.add("")
    markdown.forEachIndexed { index, _ ->
        delay(16)
        chatList[chatList.lastIndex] =
            markdown.substring(0, index + 1)
    }
} */
```

- Rebuild and run.

## Integrating Markdown Render Separately

Specify the Maven repository and add the dependency:

```gradle
maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
```

```gradle
dependencies {
    implementation("com.tencent.kuiklybase:markdown:${version}")
}
```

> The latest version for Android and iOS is 0.1.0, and for HarmonyOS it is 0.1.0-ohos.
