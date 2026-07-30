package com.kuikly.table.log

import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Android 平台日志写入实现
 */
actual object PlatformLogWriter {

    private var logDir: File? = null
    private var logFile: File? = null
    private var crashLogFile: File? = null
    private var writer: BufferedWriter? = null
    private var currentFileSize: Long = 0L
    private var config: LogConfig = LogConfig()
    private var fileIndex: Int = 0
    private val writeLock = Any()
    private var dateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.getDefault())

    actual fun init(config: LogConfig) {
        this.config = config
        synchronized(writeLock) {
            try {
                logDir = if (config.logDir.isNotEmpty()) {
                    File(config.logDir)
                } else {
                    // 默认使用应用外部存储目录
                    File(System.getProperty("java.io.tmpdir", "."), "kuikly_table_logs")
                }

                if (logDir?.exists() != true) {
                    logDir?.mkdirs()
                }

                logFile = File(logDir, config.logFileName)
                crashLogFile = File(logDir, config.crashLogFileName)

                // 每次启动覆写日志（不再追加旧日志）
                if (config.overwriteOnStart && logFile?.exists() == true) {
                    logFile?.delete()
                }
                if (config.overwriteOnStart && crashLogFile?.exists() == true) {
                    crashLogFile?.delete()
                }

                // 初始化写入器
                openWriter()

                currentFileSize = logFile?.length() ?: 0L
            } catch (e: Exception) {
                System.err.println("[TableLog] PlatformLogWriter init failed: ${e.message}")
            }
        }
    }

    actual fun writeLog(entry: LogEntry) {
        synchronized(writeLock) {
            try {
                val formatted = formatEntry(entry)
                val bytes = formatted.toByteArray(Charsets.UTF_8)

                // 检查是否需要轮转
                if (currentFileSize + bytes.size > config.maxFileSize) {
                    rotateLogFile()
                }

                writer?.write(formatted)
                writer?.newLine()
                writer?.flush()

                currentFileSize += bytes.size + 1  // +1 for newline

                // ERROR 级别日志同时写入崩溃日志
                if (entry.level == LogLevel.ERROR && config.enableCrashLog) {
                    writeToCrashLog(formatted)
                }
            } catch (e: Exception) {
                System.err.println("[TableLog] writeLog failed: ${e.message}")
            }
        }
    }

    actual fun flush() {
        synchronized(writeLock) {
            try {
                writer?.flush()
            } catch (_: Exception) { }
        }
    }

    actual fun getLogFilePath(): String {
        return logFile?.absolutePath ?: ""
    }

    actual fun getCrashLogFilePath(): String {
        return crashLogFile?.absolutePath ?: ""
    }

    private fun openWriter() {
        try {
            writer?.close()
            writer = BufferedWriter(FileWriter(logFile, true))
            currentFileSize = logFile?.length() ?: 0L
        } catch (e: Exception) {
            System.err.println("[TableLog] openWriter failed: ${e.message}")
        }
    }

    private fun rotateLogFile() {
        try {
            writer?.close()

            // 轮转文件: log.1 -> log.2 -> ... -> log.maxFiles
            for (i in config.maxFiles - 1 downTo 1) {
                val oldFile = File(logDir, "${config.logFileName}.$i")
                val newFile = File(logDir, "${config.logFileName}.${i + 1}")
                if (oldFile.exists()) {
                    if (i == config.maxFiles - 1) {
                        oldFile.delete()
                    } else {
                        oldFile.renameTo(newFile)
                    }
                }
            }

            // 当前日志文件重命名为 .1
            val backupFile = File(logDir, "${config.logFileName}.1")
            logFile?.renameTo(backupFile)

            // 创建新日志文件
            openWriter()
            currentFileSize = 0L

            // 写入轮转说明
            writer?.write(formatEntry(LogEntry(
                timestamp = System.currentTimeMillis(),
                level = LogLevel.INFO,
                tag = "TableLog",
                message = "=== 日志轮转 | 旧日志已保存至 ${backupFile.name} ==="
            )))
            writer?.newLine()
            writer?.flush()
        } catch (e: Exception) {
            System.err.println("[TableLog] rotateLogFile failed: ${e.message}")
        }
    }

    private fun writeToCrashLog(formatted: String) {
        try {
            val crashLog = crashLogFile ?: return
            // 崩溃日志也检查大小限制
            if (crashLog.length() > config.maxFileSize * 2) {
                // 崩溃日志轮转：保留最近1MB的内容
                val content = crashLog.readText()
                val trimmed = content.substring(content.length - (config.maxFileSize / 2).toInt())
                crashLog.writeText(trimmed)
            }

            PrintWriter(FileWriter(crashLog, true)).use { pw ->
                pw.println(formatted)
                pw.flush()
            }
        } catch (_: Exception) { }
    }

    private fun formatEntry(entry: LogEntry): String {
        val timeStr = try {
            dateFormat.format(Date(entry.timestamp))
        } catch (_: Exception) {
            entry.timestamp.toString()
        }
        val sb = StringBuilder()
        sb.append(timeStr)
        sb.append(" [${entry.level.label}]")
        sb.append(" [${entry.tag}]")
        sb.append(" ${entry.message}")
        entry.throwable?.let {
            sb.append("\n${it.stackTraceToString()}")
        }
        return sb.toString()
    }
}

/**
 * Android 时间戳实现
 */
internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()
