package com.kuikly.table.android
import com.tencent.kuikly.core.android.*
import android.app.Application
import com.tencent.kuikly.core.android.KuiklyCoreEntry
import com.kuikly.table.log.LogConfig
import com.kuikly.table.log.LogLevel
import com.kuikly.table.log.TableLog
import com.kuikly.table.android.adapter.KRLogAdapter
import com.kuikly.table.android.adapter.KRUncaughtExceptionHandlerAdapter
import java.io.File

class KRApplication : Application() {

    init {
        application = this
    }

    override fun onCreate() {
        super.onCreate()

        
        initLogSystem()

    }

    

    private fun initLogSystem() {
        try {
            val logDir = File(filesDir, "logs").apply { mkdirs() }
            val logDirPath = logDir.absolutePath

            
            TableLog.init(LogConfig(
                logDir = logDirPath,
                logFileName = "kuikly_table.log",
                crashLogFileName = "kuikly_table_crash.log",
                minLevel = LogLevel.DEBUG,
                maxFileSize = 5 * 1024 * 1024L,  
                maxFiles = 5,
                consoleOutput = false,  
                enableCrashLog = true
            ))

            
            KRLogAdapter.logDir = logDirPath
            KRUncaughtExceptionHandlerAdapter.logDir = logDirPath

            TableLog.i("KRApplication", "=== 应用启动 | 日志系统就绪 | 日志目录: $logDirPath ===")
        } catch (e: Exception) {
            android.util.Log.e("KRApplication", "日志系统初始化失败: ${e.message}")
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        TableLog.i("KRApplication", "=== 应用关闭 ===")
        TableLog.flush()
    }

    companion object {
        lateinit var application: Application
    }
}
