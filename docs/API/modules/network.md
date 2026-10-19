# NetworkModule

HTTP请求模块

## requestGet方法

发送HTTP GET请求

<br/>

**参数**

| 参数  | 描述     | 类型                              |
|:----|:-------|:--------------------------------|
| url <Badge text="必需" type="warn"/> | 请求url  | String                          |
| param <Badge text="必需" type="warn"/> | 请求参数  | JSONObject                      |
| responseCallback <Badge text="必需" type="warn"/> | 请求回调闭包  | [NMAllResponse](#nmallresponse) |

## requestPost方法

发送HTTP POST请求

<br/>

**参数**

| 参数  | 描述     | 类型                              |
|:----|:-------|:--------------------------------|
| url <Badge text="必需" type="warn"/> | 请求url  | String                          |
| param <Badge text="必需" type="warn"/> | 请求参数  | JSONObject                      |
| responseCallback <Badge text="必需" type="warn"/> | 请求回调闭包  | [NMAllResponse](#nmallresponse) |

## httpRequest方法

发送HTTP通用请求方法

<br/>

**参数**

| 参数  | 描述     | 类型                              |
|:----|:-------|:--------------------------------|
| url <Badge text="必需" type="warn"/> | 请求url  | String                          |
| isPost <Badge text="必需" type="warn"/> | 是否为POST请求  | Boolean                         |
| param <Badge text="必需" type="warn"/> | 请求参数  | JSONObject                      |
| headers <Badge text="非必需" type="warn"/> | 请求头参数  | JSONObject                      |
| cookie <Badge text="非必需" type="warn"/> | 请求cookie  | String                          |
| timeout <Badge text="非必需" type="warn"/> | 请求超时时间, 单位为秒  | Int                             |
| responseCallback <Badge text="必需" type="warn"/> | 请求回调闭包  | [NMAllResponse](#nmallresponse) |

## requestGetBinary方法

发送二进制HTTP GET请求

<br/>

**参数**

| 参数  | 描述     | 类型                                |
|:----|:-------|:----------------------------------|
| url <Badge text="必需" type="warn"/> | 请求url  | String                            |
| param <Badge text="必需" type="warn"/> | 请求参数  | JSONObject                        |
| responseCallback <Badge text="必需" type="warn"/> | 请求回调闭包  | [NMDataResponse](#nmdataresponse) |

## requestPostBinary方法

发送二进制HTTP POST请求

<br/>

**参数**

| 参数  | 描述     | 类型                                |
|:----|:-------|:----------------------------------|
| url <Badge text="必需" type="warn"/> | 请求url  | String                            |
| bytes <Badge text="必需" type="warn"/> | 请求参数  | ByteArray                         |
| responseCallback <Badge text="必需" type="warn"/> | 请求回调闭包  | [NMDataResponse](#nmdataresponse) |

## httpRequestBinary方法

发送二进制HTTP通用请求方法

<br/>

**参数**

| 参数                                              | 描述     | 类型                                |
|:------------------------------------------------|:-------|:----------------------------------|
| url <Badge text="必需" type="warn"/>              | 请求url  | String                            |
| isPost <Badge text="必需" type="warn"/>           | 是否为POST请求  | Boolean                           |
| bytes <Badge text="必需" type="warn"/>            | 请求参数  | ByteArray                         |
| param <Badge text="非必需" type="warn"/>           | 请求参数  | JSONObject                        |
| headers <Badge text="非必需" type="warn"/>         | 请求头参数  | JSONObject                        |
| cookie <Badge text="非必需" type="warn"/>          | 请求cookie  | String                            |
| timeout <Badge text="非必需" type="warn"/>         | 请求超时时间, 单位为秒  | Int                               |
| responseCallback <Badge text="必需" type="warn"/> | 请求回调闭包  | [NMDataResponse](#nmdataresponse) |

## httpStreamRequest方法

流式 HTTP 文本接口：发起 HTTP 请求，在响应体到达过程中按网络分片逐块回调 UTF-8 文本，无需等待完整响应。适用于大模型流式输出、实时数据推送等场景。

该接口只负责传输文本，**不解析任何上层协议**：`data` 事件给出的是文本分片（chunk），不是完整的 SSE 事件、JSON 行或其他消息。对接 SSE（Server-Sent Events）、NDJSON 等协议时，需要业务侧将分片按顺序拼接后自行做增量解析。

<br/>

**参数**

| 参数  | 描述     | 类型                                              |
|:----|:-------|:------------------------------------------------|
| url <Badge text="必需" type="warn"/> | 请求url  | String                                          |
| isPost <Badge text="必需" type="warn"/> | 是否为POST请求  | Boolean                                         |
| param <Badge text="非必需" type="warn"/> | 请求参数（GET 时拼接到 URL，POST 时作为 body，编码规则见下文）  | JSONObject                                      |
| headers <Badge text="非必需" type="warn"/> | 请求头参数  | JSONObject                                      |
| cookie <Badge text="非必需" type="warn"/> | 请求cookie  | String                                          |
| timeout <Badge text="非必需" type="warn"/> | 连接超时与空闲超时, 单位为秒，默认30，<= 0 时使用默认值  | Int                                             |
| eventCallback <Badge text="必需" type="warn"/> | 流式事件回调  | [NMStreamEventCallback](#nmstreameventcallback) |

**返回值**

| 类型                                          | 描述                     |
|:--------------------------------------------|:-----------------------|
| [StreamRequestHandle](#streamrequesthandle) | 流式请求句柄，用于主动关闭连接 |

**行为约定（Android、iOS、鸿蒙一致）**

- **POST body**：`headers` 中 `Content-Type`（不区分大小写）包含 `json` 时，`param` 序列化为 JSON；否则按 UTF-8 `application/x-www-form-urlencoded` 编码（`&`、`=`、`+`、空格、中文等均会被转义），未指定 `Content-Type` 时自动补齐为 `application/x-www-form-urlencoded; charset=UTF-8`。
- **表单与 GET 参数取值**：key 与 value 均按 UTF-8 转义；`null` 编码为空值（`key=`），布尔值为 `true`/`false`，对象与数组为 JSON 字符串。GET 参数追加在 URL 已有查询参数之后。
- **timeout**：表示连接超时与空闲超时。连续 `timeout` 秒没有收到任何数据时以 `error` 结束（`statusCode` 为 -1001）；收到数据即重新计时，因此持续推送的长连接不会因总时长被中断。
- **终态**：每个请求最多收到一次 `complete` 或 `error`，之后不再回调。HTTP 非 2xx 一律以 `error` 结束，不会收到 `complete`。
- **空响应**：2xx 且响应体为空时，直接收到 `complete`，不会收到 `data` 事件（因此也拿不到 `response`）。
- **并发**：Android 端同时进行的流式请求最多 8 个，超出时立即以 `error` 返回，不会占用普通网络请求的线程池。已 `close()` 的请求立即释放名额，即使其连接仍在后台超时退出。
- **平台支持**：目前仅支持 Android、iOS、鸿蒙，Web 与小程序暂不支持。

**示例：对接 SSE 接口**

下面的示例把分片交给业务侧的 SSE 解析器，解析出完整事件后再处理。`SSEEventParser` 的完整实现见 Demo 中的 `SSETypewriterDemoPage.kt`，它按 SSE 规范处理跨分片的换行（含被拆开的 `\r\n`），遇到空行才派发一条事件。

```kotlin
val parser = SSEEventParser { eventData ->
    // eventData 是一条完整 SSE 事件中 data 字段的内容
    if (eventData != "[DONE]") {
        handleMessage(JSONObject(eventData))
    }
}

val handle = acquireModule<NetworkModule>(NetworkModule.MODULE_NAME).httpStreamRequest(
    url = "https://example.com/v1/chat/completions",
    isPost = true,
    param = requestBody,
    headers = JSONObject().apply {
        put("Content-Type", "application/json")
        put("Accept", "text/event-stream")
    },
    timeout = 60
) { event, data, response ->
    when (event) {
        "data" -> parser.feed(data) // data 只是文本分片，可能是半条或多条事件
        "complete" -> parser.reset()
        "error" -> {
            parser.reset()
            showError(data, response?.statusCode)
        }
    }
}

// 页面退出或用户取消时主动关闭，关闭后不会再收到任何事件
handle.close()
```

---

## 类型说明
### NMAllResponse
```kotlin
NMAllResponse = (data: JSONObject, success : Boolean , errorMsg: String, response: NetworkResponse) -> Unit
```

| 参数  | 描述     | 类型                                  |
|:----|:-------|:------------------------------------|
| data | 返回数据   | JSONObject                          |
| success | 请求是否成功 | Boolean                             |
| errorMsg | 错误信息   | String                              |
| response | 响应包信息  | [NetworkResponse](#networkresponse) |

### NMDataResponse
```kotlin
NMDataResponse = (data: ByteArray, success: Boolean, errorMsg: String, response: NetworkResponse) -> Unit
```

| 参数  | 描述     | 类型                                  |
|:----|:-------|:------------------------------------|
| data | 返回数据   | ByteArray                           |
| success | 请求是否成功 | Boolean                             |
| errorMsg | 错误信息   | String                              |
| response | 响应包信息  | [NetworkResponse](#networkresponse) |

### NMStreamEventCallback
```kotlin
NMStreamEventCallback = (event: String, data: String, response: NetworkResponse?) -> Unit
```

| 参数  | 描述     | 类型                                  |
|:----|:-------|:------------------------------------|
| event | 事件类型，取值见下表 | String                              |
| data | 事件数据 | String                              |
| response | 网络响应信息（headers、statusCode）。首次 `data` 事件与 HTTP 错误导致的 `error` 事件中有值，其余为 null | [NetworkResponse](#networkresponse)? |

**event 事件类型**

| 事件值 | 描述 |
|:------|:-----|
| `data` | 收到一段响应文本分片，`data` 为该分片内容。首次回调时 `response` 包含 HTTP 响应头和状态码 |
| `complete` | 响应体已全部接收，请求正常结束。`data` 为空字符串 |
| `error` | 请求发生错误。`data` 包含错误信息描述；HTTP 非 2xx 时 `response` 携带响应头和状态码 |

::: tip 注意
- `data` 事件中的文本按网络分片切分，分片边界由网络决定，与业务消息边界无关：一个分片可能包含半条或多条消息，行结束符（`\r\n`）也可能被拆到两个分片中。请不要对单个分片直接做 JSON 解析或按行切分，应按顺序拼接后增量解析，SSE 可参考 Demo 中的 `SSEEventParser`。
- 响应按 UTF-8 解码，多字节字符跨分片时会被正确拼接；非法字节会被替换为 `U+FFFD`。
- 鸿蒙平台在数据到达时无法取得 HTTP 状态码，首次 `data` 事件中 `response.statusCode` 为 null；非 2xx 响应的错误响应体可能先以 `data` 事件到达，最终以携带状态码的 `error` 事件结束，请以终态事件为准。
:::

### StreamRequestHandle

`httpStreamRequest` 的返回值，用于主动关闭流式连接。

| 方法 | 描述 |
|:----|:-----|
| close() | 关闭连接并释放资源。可重复调用（幂等）；调用后不会再收到任何事件，包括 `complete` 和 `error` |

### NetworkResponse

| 参数                                         | 描述     | 类型         |
|:-------------------------------------------|:-------|:-----------|
| headerFields                               | 响应头参数   | JSONObject |
| statusCode <Badge text="非必需" type="warn"/> <Badge text="鸿蒙 2.26.0 及以上支持" type="warn"/> | 响应状态码   | Int        |
