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

### NetworkResponse

| 参数                                         | 描述     | 类型         |
|:-------------------------------------------|:-------|:-----------|
| headerFields                               | 响应头参数   | JSONObject |
| statusCode <Badge text="非必需" type="warn"/> | 响应状态码   | Int        |
