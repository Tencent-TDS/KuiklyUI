package com.kuikly.table.log

/**
 * 表格组件日志系统
 *
 * 提供跨平台的完整日志记录功能，每次运行自动保存完整日志到文件。
 * 支持日志级别过滤、时间戳、日志轮转、崩溃日志独立保存。
 *
 * 使用方法:
 * ```
 * // 初始化（应用启动时调用一次）
 * TableLog.init(LogConfig(logDir = "/path/to/logs"))
 *
 * // 记录日志
 * TableLog.i("KuiklyTable", "表格渲染开始，列数=${columns.size}")
 * TableLog.e("KuiklyTable", "渲染异常", exception)
 * ```
 */

/**
 * 日志级别
 */
enum class LogLevel(val value: Int, val label: String) {
    DEBUG(1, "DEBUG"),
    INFO(2, "INFO"),
    WARN(3, "WARN"),
    ERROR(4, "ERROR")
}

/**
 * 日志配置
 */
data class LogConfig(
    /** 日志文件目录路径 */
    val logDir: String = "",
    /** 主日志文件名 */
    val logFileName: String = "kuikly_table.log",
    /** 崩溃/错误日志文件名 */
    val crashLogFileName: String = "kuikly_table_crash.log",
    /** 最低日志级别 */
    val minLevel: LogLevel = LogLevel.DEBUG,
    /** 单个日志文件最大大小（字节），超出后轮转 */
    val maxFileSize: Long = 5 * 1024 * 1024L,  // 5MB
    /** 最大日志文件数量 */
    val maxFiles: Int = 5,
    /** 是否同时在控制台输出 */
    val consoleOutput: Boolean = true,
    /** 是否启用崩溃日志独立保存 */
    val enableCrashLog: Boolean = true,
    /** 每次启动时是否覆写日志（true=清空后写入, false=追加） */
    val overwriteOnStart: Boolean = true
)

/**
 * 日志条目
 */
data class LogEntry(
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null
) {
    fun formatted(): String {
        val sb = StringBuilder()
        sb.append(formatTimestamp(timestamp))
        sb.append(" [${level.label}]")
        sb.append(" [${tag}]")
        sb.append(" ${message}")
        throwable?.let {
            sb.append("\n${it.stackTraceToString()}")
        }
        return sb.toString()
    }

    companion object {
        private fun formatTimestamp(millis: Long): String {
            // 使用简单的格式化避免依赖平台API
            val totalSeconds = millis / 1000
            val seconds = totalSeconds % 60
            val totalMinutes = totalSeconds / 60
            val minutes = totalMinutes % 60
            val totalHours = totalMinutes / 60
            val hours = totalHours % 24
            val days = totalHours / 24
            return "${days}d ${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        }
    }
}

/**
 * 跨平台日志接口 — 平台相关实现
 */
expect object PlatformLogWriter {
    /** 初始化日志写入器 */
    fun init(config: LogConfig)

    /** 写入一条日志到文件 */
    fun writeLog(entry: LogEntry)

    /** 刷新缓冲区 */
    fun flush()

    /** 获取当前日志文件路径 */
    fun getLogFilePath(): String

    /** 获取崩溃日志文件路径 */
    fun getCrashLogFilePath(): String
}

/**
 * 表格日志 — 全局单例
 */
object TableLog {

    private var config: LogConfig = LogConfig()
    private var initialized: Boolean = false
    private val logBuffer = mutableListOf<LogEntry>()
    private val bufferLock = object {}

    /**
     * 初始化日志系统。应用启动时调用一次。
     */
    fun init(config: LogConfig) {
        this.config = config
        try {
            PlatformLogWriter.init(config)
            initialized = true
            // 写入启动日志
            val entry = LogEntry(
                timestamp = currentTimeMillis(),
                level = LogLevel.INFO,
                tag = "TableLog",
                message = "=== 日志系统初始化完成 | 日志目录: ${config.logDir} | 级别: ${config.minLevel.label} ==="
            )
            PlatformLogWriter.writeLog(entry)
            if (config.consoleOutput) {
                println(entry.formatted())
            }
        } catch (e: Exception) {
            println("[TableLog] 日志系统初始化失败: ${e.message}")
            initialized = false
        }
    }

    /**
     * DEBUG 级别日志
     */
    fun d(tag: String, message: String) {
        log(LogLevel.DEBUG, tag, message)
    }

    /**
     * INFO 级别日志
     */
    fun i(tag: String, message: String) {
        log(LogLevel.INFO, tag, message)
    }

    /**
     * WARN 级别日志
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.WARN, tag, message, throwable)
    }

    /**
     * ERROR 级别日志
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, tag, message, throwable)
    }

    /**
     * 记录异常（自动标记为 ERROR 级别）
     */
    fun exception(tag: String, throwable: Throwable, context: String = "") {
        val message = if (context.isNotEmpty()) {
            "$context | 异常: ${throwable.message}"
        } else {
            "异常: ${throwable.message}"
        }
        log(LogLevel.ERROR, tag, message, throwable)
    }

    /**
     * 记录组件生命周期事件
     */
    fun lifecycle(tag: String, event: String) {
        log(LogLevel.INFO, tag, "[生命周期] $event")
    }

    /**
     * 记录渲染事件
     */
    fun render(tag: String, event: String) {
        log(LogLevel.DEBUG, tag, "[渲染] $event")
    }

    /**
     * 记录用户交互事件
     */
    fun interaction(tag: String, event: String) {
        log(LogLevel.INFO, tag, "[交互] $event")
    }

    /**
     * 内部日志记录方法
     */
    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        if (level.value < config.minLevel.value) return

        val entry = LogEntry(
            timestamp = currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            throwable = throwable
        )

        // 输出到控制台
        if (config.consoleOutput) {
            println(entry.formatted())
        }

        // 写入文件
        if (initialized) {
            try {
                PlatformLogWriter.writeLog(entry)
            } catch (e: Exception) {
                println("[TableLog] 写入日志失败: ${e.message}")
            }
        } else {
            // 未初始化时缓存日志
            synchronized(bufferLock) {
                logBuffer.add(entry)
                if (logBuffer.size > 500) {
                    logBuffer.removeAt(0)
                }
            }
        }
    }

    /**
     * 手动刷新日志缓冲区
     */
    fun flush() {
        if (initialized) {
            try {
                // 写入缓存的日志
                synchronized(bufferLock) {
                    logBuffer.forEach { entry ->
                        try {
                            PlatformLogWriter.writeLog(entry)
                        } catch (_: Exception) { }
                    }
                    logBuffer.clear()
                }
                PlatformLogWriter.flush()
            } catch (_: Exception) { }
        }
    }

    /**
     * 获取当前日志文件路径
     */
    fun getLogFilePath(): String {
        return if (initialized) {
            PlatformLogWriter.getLogFilePath()
        } else {
            ""
        }
    }

    /**
     * 获取崩溃日志文件路径
     */
    fun getCrashLogFilePath(): String {
        return if (initialized && config.enableCrashLog) {
            PlatformLogWriter.getCrashLogFilePath()
        } else {
            ""
        }
    }

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = initialized
}

/**
 * 获取当前时间戳（毫秒）
 * 跨平台实现，不依赖平台特定API
 */
internal expect fun currentTimeMillis(): Long
