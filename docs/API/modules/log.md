# LogModule

日志模块，提供 Info / Debug / Error 三个级别的日志输出。

日志经端侧的 `KRLogModule` 下发，由宿主决定具体的打印实现。

:::tip 使用方式
该能力在端侧以 `KRLogModule` 承载，Kotlin 侧封装为 `KLog` 对象，业务直接调用 `KLog.i/d/e` 即可，**不需要** `acquireModule<...>()` 获取模块实例。这也是它与其他内置 Module 在使用方式上的差异。
:::

## KLog.i方法

输出 Info 级别日志。

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| tag <Badge text="必需" type="warn"/> | 日志标签 | String |
| msg <Badge text="必需" type="warn"/> | 日志内容 | String |

## KLog.d方法

输出 Debug 级别日志。

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| tag <Badge text="必需" type="warn"/> | 日志标签 | String |
| msg <Badge text="必需" type="warn"/> | 日志内容 | String |

## KLog.e方法

输出 Error 级别日志。

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| tag <Badge text="必需" type="warn"/> | 日志标签 | String |
| msg <Badge text="必需" type="warn"/> | 日志内容 | String |

**示例**

```kotlin
KLog.i("DemoPage", "page created")
KLog.d("DemoPage", "data loaded, count = ${list.size}")
KLog.e("DemoPage", "request failed: $errorMsg")
```

::::tip 端侧实现
`KLog` 本身不做格式化与落盘，最终由宿主在端侧实现 `KRLogModule` 的打印逻辑，因此不同工程的日志输出位置与级别过滤策略可能不同。
::::
