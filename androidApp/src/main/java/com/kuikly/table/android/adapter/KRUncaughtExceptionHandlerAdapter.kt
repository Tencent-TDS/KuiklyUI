package com.kuikly.table.android.adapter

import android.util.Log
import com.tencent.kuikly.core.render.android.adapter.IKRUncaughtExceptionHandlerAdapter
import com.kuikly.table.log.TableLog
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object KRUncaughtExceptionHandlerAdapter : IKRUncaughtExceptionHandlerAdapter {

    private const val TAG = "KRExceptionHandler"

    
    var logDir: String = ""

    override fun uncaughtException(throwable: Throwable) {
        Log.e(TAG, "KR error: ${throwable.stackTraceToString()}")

        
        TableLog.exception(TAG, throwable, "未捕获异常")

        
        saveCrashToFile(throwable)
    }

    private fun saveCrashToFile(throwable: Throwable) {
        try {
            if (logDir.isEmpty()) return

            val dir = File(logDir)
            if (!dir.exists()) dir.mkdirs()

            val crashFile = File(dir, "crash_${crashTimestamp()}.log")
            PrintWriter(FileWriter(crashFile)).use { writer ->
                writer.println("=== KuiklyTableUI 崩溃日志 ===")
                writer.println("时间: ${formatDate(Date())}")
                writer.println("应用版本: ${getAppVersion()}")
                writer.println("设备信息: ${getDeviceInfo()}")
                writer.println()
                writer.println("--- 异常堆栈 ---")
                throwable.printStackTrace(writer)
                writer.println()
                writer.println("--- 日志文件: ${TableLog.getLogFilePath()} ---")
                writer.flush()
            }

            Log.i(TAG, "崩溃日志已保存: ${crashFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "保存崩溃日志失败: ${e.message}")
        }
    }

    private fun crashTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
    }

    private fun formatDate(date: Date): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(date)
    }

    private fun getAppVersion(): String {
        return try {
            val context = com.kuikly.table.android.KRApplication.application
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${pkgInfo.versionName} (${pkgInfo.versionCode})"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun getDeviceInfo(): String {
        return "Android ${android.os.Build.VERSION.RELEASE} | ${android.os.Build.MODEL} | SDK ${android.os.Build.VERSION.SDK_INT}"
    }
}
