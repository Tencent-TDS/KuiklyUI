# 日志系统模块 (log)

## 概述

本目录包含表格组件的跨平台日志系统，提供完整的日志记录、文件保存和崩溃日志独立存储功能。

## 架构

```
TableLog (commonMain)        — 日志门面（单例）
    ├── 日志级别: DEBUG / INFO / WARN / ERROR
    ├── 日志缓冲: 未初始化时缓存最多500条
    ├── 时间戳生成: currentTimeMillis() expect
    └── 文件写入: PlatformLogWriter expect
            ├── Android actual: java.io.File + BufferedWriter
            └── iOS actual: NSFileManager + NSFileHandle
```

## 日志级别

| 级别 | 方法 | 用途 |
|------|------|------|
| DEBUG | `TableLog.d(tag, msg)` | 渲染调试信息 |
| INFO | `TableLog.i(tag, msg)` | 生命周期/用户交互 |
| WARN | `TableLog.w(tag, msg)` | 配置警告 |
| ERROR | `TableLog.e(tag, msg)` | 运行错误 |
| — | `TableLog.exception(tag, e)` | 异常（含堆栈） |

## 专项日志方法

| 方法 | 用途 |
|------|------|
| `TableLog.lifecycle(tag, event)` | 组件生命周期 |
| `TableLog.render(tag, event)` | 渲染事件（DEBUG级别） |
| `TableLog.interaction(tag, event)` | 用户交互事件 |

## 日志文件

每次运行自动生成以下日志文件：

| 文件 | 说明 |
|------|------|
| `kuikly_table.log` | 主日志文件（所有级别） |
| `kuikly_table.log.1` ~ `.5` | 轮转备份（5MB × 5） |
| `kuikly_table_crash.log` | 崩溃日志（仅ERROR级别） |
| `crash_YYYYMMDD_HHMMSS.log` | 独立崩溃报告（含设备信息） |

## 初始化

### Android (KRApplication.onCreate)

```kotlin
TableLog.init(LogConfig(
    logDir = filesDir/absPath + "/logs",
    logFileName = "kuikly_table.log",
    crashLogFileName = "kuikly_table_crash.log",
    minLevel = LogLevel.DEBUG,
    maxFileSize = 5 * 1024 * 1024L,  // 5MB
    maxFiles = 5,
    enableCrashLog = true
))
```

### iOS (AppDelegate)

```kotlin
TableLog.init(LogConfig(logDir = documentsDir + "/logs"))
```

## 日志格式

```
yyyy-MM-dd'T'HH:mm:ss.SSS [LEVEL] [TAG] message
stacktrace (if throwable)
```

## 使用示例

```kotlin
// 渲染日志
TableLog.render("KuiklyTable", "开始渲染 | 列=5 行=100")

// 错误日志
try {
    renderTable()
} catch (e: Exception) {
    TableLog.exception("KuiklyTable", e, "渲染表格时发生异常")
}
```

## 相关模块

- [core/](../) — 核心组件（集成日志记录）
- Android: `KRLogAdapter` — 桥接 Kuikly 日志到文件
- Android: `KRUncaughtExceptionHandlerAdapter` — 崩溃日志独立保存
