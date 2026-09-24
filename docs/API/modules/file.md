# FileModule

文件读写模块，提供 App 沙盒可写目录下的文件写入与目录查询能力，写入结果通过回调返回。

:::tip 用途说明
该模块当前主要用于 RecompositionProfiler 导出 JSON 报告供分析使用。业务侧的持久化存储请使用 [SharedPreferencesModule](sp.md)。
:::

## writeFile方法

将内容写入 App 可写目录下的文件（异步，覆盖写）。

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| filename <Badge text="必需" type="warn"/> | 文件名（不含路径），如 `report.json` | String |
| content <Badge text="必需" type="warn"/> | 文件内容 | String |
| callback <Badge text="非必需" type="warn"/> | 完成回调 | CallbackFn |

回调参数 `result["path"]` 为写入路径，`result["error"]` 为错误信息。

**示例**

```kotlin
acquireModule<FileModule>(FileModule.MODULE_NAME).writeFile("report.json", jsonContent) { result ->
    val path = result?.optString("path")
}
```

## appendFile方法

追加内容到文件末尾（异步，文件不存在时创建）。每次追加会在末尾补一个换行符，适合写入 JSONL 格式。

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| filename <Badge text="必需" type="warn"/> | 文件名（不含路径） | String |
| content <Badge text="必需" type="warn"/> | 追加内容（通常为一行 JSON） | String |
| callback <Badge text="非必需" type="warn"/> | 完成回调 | CallbackFn |

## getFilesDir方法

获取 App 可写目录的绝对路径（异步）。

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| callback <Badge text="非必需" type="warn"/> | 完成回调 | CallbackFn |

回调参数 `result["path"]` 为目录绝对路径。

**示例**

```kotlin
acquireModule<FileModule>(FileModule.MODULE_NAME).getFilesDir { result ->
    val dir = result?.optString("path")
}
```
