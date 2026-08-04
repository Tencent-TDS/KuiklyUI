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

发送 SSE（Server-Sent Events）流式 HTTP 请求。适用于大模型流式输出、实时数据推送等场景。

服务端通过 SSE 协议持续推送数据，客户端通过事件回调逐步接收，无需等待完整响应。

<br/>

**参数**

| 参数  | 描述     | 类型                                              |
|:----|:-------|:------------------------------------------------|
| url <Badge text="必需" type="warn"/> | 请求url  | String                                          |
| isPost <Badge text="必需" type="warn"/> | 是否为POST请求  | Boolean                                         |
| param <Badge text="非必需" type="warn"/> | 请求参数（GET 时拼接到 URL，POST 时作为 body）  | JSONObject                                      |
| headers <Badge text="非必需" type="warn"/> | 请求头参数  | JSONObject                                      |
| cookie <Badge text="非必需" type="warn"/> | 请求cookie  | String                                          |
| timeout <Badge text="非必需" type="warn"/> | 请求超时时间, 单位为秒，默认30  | Int                                             |
| eventCallback <Badge text="必需" type="warn"/> | 流式事件回调  | [NMStreamEventCallback](#nmstreameventcallback) |

**返回值**

| 类型                                          | 描述                     |
|:--------------------------------------------|:-----------------------|
| [StreamRequestHandle](#streamrequesthandle) | 流式请求句柄，用于主动关闭连接 |

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
| response | 网络响应信息（headers、statusCode），**仅在首次 `data` 事件回调中有值**，后续为 null | [NetworkResponse](#networkresponse)? |

**event 事件类型**

| 事件值 | 描述 |
|:------|:-----|
| `data` | 收到服务端推送的数据块。`data` 字段包含本次推送的文本内容。首次回调时 `response` 包含 HTTP 响应头和状态码 |
| `complete` | 流式传输正常结束。`data` 为空字符串 |
| `error` | 请求发生错误。`data` 包含错误信息描述 |

### NetworkResponse

| 参数                                         | 描述     | 类型         |
|:-------------------------------------------|:-------|:-----------|
| headerFields                               | 响应头参数   | JSONObject |
| statusCode <Badge text="非必需" type="warn"/> | 响应状态码   | Int        |
