@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.kuikly.table.log

import platform.Foundation.*

/**
 * NSDate 基准日期（2001-01-01 00:00:00 UTC）相对 1970 epoch 的秒数偏移，
 * 用于在仅暴露 timeIntervalSinceReferenceDate 构造器的 interop 下构造 epoch 时间。
 */
private const val SECONDS_FROM_1970_TO_REFERENCE_DATE = 978307200.0

/**
 * iOS 平台日志写入实现
 */
actual object PlatformLogWriter {

    private var logDirPath: String = ""
    private var logFilePath: String = ""
    private var crashLogFilePath: String = ""
    private var currentFileSize: Long = 0L
    private var config: LogConfig = LogConfig()
    private val writeLock = NSLock()
    private var fileHandle: NSFileHandle? = null
    private val dateFormatter: NSDateFormatter = NSDateFormatter().apply {
        dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS"
        locale = NSLocale(localeIdentifier = "en_US_POSIX")
    }

    actual fun init(config: LogConfig) {
        this.config = config
        writeLock.lock()
        try {
            try {
                logDirPath = if (config.logDir.isNotEmpty()) {
                    config.logDir
                } else {
                    val paths = NSSearchPathForDirectoriesInDomains(
                        NSDocumentDirectory, NSUserDomainMask, true
                    )
                    val docsDir = paths.firstOrNull() as? String
                        ?: NSTemporaryDirectory()
                    "$docsDir/kuikly_table_logs"
                }

                // 创建日志目录
                val fileManager = NSFileManager.defaultManager
                if (!fileManager.fileExistsAtPath(logDirPath)) {
                    fileManager.createDirectoryAtPath(logDirPath,
                        true, null, null)
                }

                logFilePath = "$logDirPath/${config.logFileName}"
                crashLogFilePath = "$logDirPath/${config.crashLogFileName}"

                // 每次启动覆写日志
                if (config.overwriteOnStart) {
                    if (fileManager.fileExistsAtPath(logFilePath)) {
                        fileManager.removeItemAtPath(logFilePath, null)
                    }
                    if (fileManager.fileExistsAtPath(crashLogFilePath)) {
                        fileManager.removeItemAtPath(crashLogFilePath, null)
                    }
                }

                // 确保文件存在
                if (!fileManager.fileExistsAtPath(logFilePath)) {
                    fileManager.createFileAtPath(logFilePath, NSData(), null)
                }

                fileHandle = NSFileHandle.fileHandleForUpdatingAtPath(logFilePath)
                fileHandle?.truncateFileAtOffset(0uL)  // 从头开始写
                fileHandle?.seekToEndOfFile()

                // 获取当前文件大小
                val attributes = fileManager.attributesOfItemAtPath(logFilePath, null)
                currentFileSize = (attributes?.get(NSFileSize) as? NSNumber)?.longValue ?: 0L
            } catch (e: Exception) {
                println("[TableLog] PlatformLogWriter init failed: ${e.message}")
            }
        } finally {
            writeLock.unlock()
        }
    }

    actual fun writeLog(entry: LogEntry) {
        writeLock.lock()
        try {
            try {
                val formatted = formatEntry(entry)
                val line = formatted + "\n"
                val data = (line as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
                val bytes = data.length.toLong()

                // 检查是否需要轮转
                if (currentFileSize + bytes > config.maxFileSize) {
                    rotateLogFile()
                }

                fileHandle?.seekToEndOfFile()
                fileHandle?.writeData(data)
                // Note: NSFileHandle.writeData does an implicit sync/flush on iOS

                currentFileSize += bytes

                // ERROR 级别日志同时写入崩溃日志
                if (entry.level == LogLevel.ERROR && config.enableCrashLog) {
                    writeToCrashLog(formatted)
                }
            } catch (e: Exception) {
                println("[TableLog] writeLog failed: ${e.message}")
            }
        } finally {
            writeLock.unlock()
        }
    }

    actual fun flush() {
        writeLock.lock()
        try {
            try {
                fileHandle?.synchronizeFile()
            } catch (_: Exception) { }
        } finally {
            writeLock.unlock()
        }
    }

    actual fun getLogFilePath(): String = logFilePath

    actual fun getCrashLogFilePath(): String = crashLogFilePath

    private fun rotateLogFile() {
        try {
            fileHandle?.closeFile()
            val fileManager = NSFileManager.defaultManager

            // 轮转文件
            for (i in config.maxFiles - 1 downTo 1) {
                val oldPath = "$logDirPath/${config.logFileName}.$i"
                val newPath = "$logDirPath/${config.logFileName}.${i + 1}"
                if (fileManager.fileExistsAtPath(oldPath)) {
                    if (i == config.maxFiles - 1) {
                        fileManager.removeItemAtPath(oldPath, null)
                    } else {
                        fileManager.moveItemAtPath(oldPath, newPath, null)
                    }
                }
            }

            // 当前日志文件重命名为 .1
            val backupPath = "$logDirPath/${config.logFileName}.1"
            fileManager.moveItemAtPath(logFilePath, backupPath, null)

            // 创建新日志文件
            fileManager.createFileAtPath(logFilePath, NSData(), null)
            fileHandle = NSFileHandle.fileHandleForUpdatingAtPath(logFilePath)
            fileHandle?.seekToEndOfFile()
            currentFileSize = 0L

            // 写入轮转说明
            val entry = LogEntry(
                timestamp = currentTimeMillis(),
                level = LogLevel.INFO,
                tag = "TableLog",
                message = "=== 日志轮转 | 旧日志已保存至 ${config.logFileName}.1 ==="
            )
            val line = formatEntry(entry) + "\n"
            val data = (line as NSString).dataUsingEncoding(NSUTF8StringEncoding)
            if (data != null) {
                fileHandle?.writeData(data)
                currentFileSize += data.length.toLong()
            }
        } catch (e: Exception) {
            println("[TableLog] rotateLogFile failed: ${e.message}")
        }
    }

    private fun writeToCrashLog(formatted: String) {
        try {
            val fileManager = NSFileManager.defaultManager
            if (!fileManager.fileExistsAtPath(crashLogFilePath)) {
                fileManager.createFileAtPath(crashLogFilePath, NSData(), null)
            }

            // 崩溃日志大小限制检查
            val attr = fileManager.attributesOfItemAtPath(crashLogFilePath, null)
            val crashSize = (attr?.get(NSFileSize) as? NSNumber)?.longValue ?: 0L
            if (crashSize > config.maxFileSize * 2) {
                val existingContent = NSString.stringWithContentsOfFile(
                    crashLogFilePath, NSUTF8StringEncoding, null
                ) as NSString? ?: return
                val keepSize = (config.maxFileSize / 2).toInt()
                val trimmedNs: NSString = if (existingContent.length > keepSize.toULong()) {
                    existingContent.substringFromIndex(existingContent.length - keepSize.toULong()) as NSString
                } else {
                    existingContent
                }
                trimmedNs.writeToFile(crashLogFilePath, true, NSUTF8StringEncoding, null)
            }

            val line = formatted + "\n"
            val data = (line as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
            val handle = NSFileHandle.fileHandleForUpdatingAtPath(crashLogFilePath)
            handle?.seekToEndOfFile()
            handle?.writeData(data)
            handle?.closeFile()
        } catch (_: Exception) { }
    }

    private fun formatEntry(entry: LogEntry): String {
        val timeStr = try {
            val date = NSDate(timeIntervalSinceReferenceDate = entry.timestamp / 1000.0 - SECONDS_FROM_1970_TO_REFERENCE_DATE)
            dateFormatter.stringFromDate(date)
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
 * iOS 跨平台加锁实现（Kotlin/Native 无 synchronized，使用 NSLock）
 */
private val logLock = NSLock()

internal actual fun <R> withLogLock(block: () -> R): R {
    logLock.lock()
    try {
        return block()
    } finally {
        logLock.unlock()
    }
}

/**
 * iOS 时间戳实现
 */
internal actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * 1000).toLong()
