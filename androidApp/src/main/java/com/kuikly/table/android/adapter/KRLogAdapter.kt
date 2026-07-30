package com.kuikly.table.android.adapter

import android.util.Log
import com.tencent.kuikly.core.render.android.adapter.IKRLogAdapter
import com.kuikly.table.log.TableLog
import com.kuikly.table.log.LogConfig

object KRLogAdapter : IKRLogAdapter {

    
    var logDir: String = ""
        set(value) {
            field = value
            if (value.isNotEmpty()) {
                TableLog.init(LogConfig(
                    logDir = value,
                    minLevel = com.kuikly.table.log.LogLevel.DEBUG,
                    consoleOutput = false  
                ))
            }
        }

    override val asyncLogEnable: Boolean
        get() = true

    override fun i(tag: String, msg: String) {
        Log.i(tag, msg)
        TableLog.i(tag, msg)
    }

    override fun d(tag: String, msg: String) {
        Log.d(tag, msg)
        TableLog.d(tag, msg)
    }

    override fun e(tag: String, msg: String) {
        Log.e(tag, msg)
        TableLog.e(tag, msg)
    }
}
