# MemoryCacheModule

页面级别内存缓存模块, 可以用来做**页面级别**的键值对缓存，通常用于复杂数据的缓存和传输，可参考`cacheImage`方法和`Canvas`的`drawImage`方法搭配使用的[示例](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/demo/CanvasTestPage.kt)。

:::tip 注意
缓存有效期为页面级别, 页面退出后, 缓存就会失效
:::

## setObject方法

缓存键值对

<br/>

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| key <Badge text="必需" type="warn"/> | 缓存key  | String |
| value <Badge text="必需" type="warn"/> | 缓存值  | Any |

## cacheImage方法

缓存图片

<br/>

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| src <Badge text="必需" type="warn"/> | 需要缓存的图片的源  | String |
| sync <Badge text="必需" type="warn"/> | 是否同步缓存（本地资源和base64图片支持同步缓存）  | Boolean |
| callback <Badge text="可选" type="warn"/> | 是否同步缓存（本地资源和base64图片支持同步缓存）  | ImageCacheCallback |

如果是同步缓存，`cacheImage`方法会返回`ImageCacheStatus`类型，该类型的成员为：
| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| state | 缓存图片的状态 | String |
| errorCode | 错误码，0代表成功  | Int |
| errorMsg  | 错误信息  | String |
| cacheKey  | 缓存图片的key  | String |

如果是异步缓存，需传入callback，callback会传入`ImageCacheStatus`参数。