# FileModule

文件读写模块，提供固定 Profiler 目录下的文件写入与目录查询能力，写入结果通过回调返回。

<Badge text="Android" type="warn"/> <Badge text="iOS" type="warn"/> <Badge text="鸿蒙" type="warn"/> <Badge text="Web 不支持" type="warn"/>

:::tip 用途说明
该模块当前主要用于 RecompositionProfiler 导出 JSON 报告供分析使用。业务侧的持久化存储请使用 [SharedPreferencesModule](sp.md)。

文件统一写入固定的 `KuiklyProfiler` 子目录：Android 为 `cacheDir/KuiklyProfiler`，iOS 为 `Library/Caches/KuiklyProfiler`，鸿蒙为 `filesDir/KuiklyProfiler`。
:::

## writeFile方法

将内容写入 `KuiklyProfiler` 目录下的文件（异步，覆盖写）。

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| filename <Badge text="必需" type="warn"/> | 文件名（不含路径），如 `report.json` | String |
| content <Badge text="必需" type="warn"/> | 文件内容 | String |
| callback <Badge text="非必需" type="warn"/> | 完成回调 | CallbackFn |

回调参数 `result["path"]` 为写入路径，`result["error"]` 为错误信息。Android、鸿蒙在 `filename` 或 `content` 为空时直接回调 `error`，不会写入。

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

获取 `KuiklyProfiler` 目录的绝对路径（异步）。

**参数**

| 参数  | 描述     | 类型 |
|:----|:-------|:--|
| callback <Badge text="非必需" type="warn"/> | 完成回调 | CallbackFn |

回调参数 `result["path"]` 为目录绝对路径；获取失败时回调 `result["error"]`。

**示例**

```kotlin
acquireModule<FileModule>(FileModule.MODULE_NAME).getFilesDir { result ->
    val dir = result?.optString("path")
}
```
