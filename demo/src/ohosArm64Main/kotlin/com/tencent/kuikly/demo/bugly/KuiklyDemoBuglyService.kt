package com.tencent.kuikly.demo

import kotlin.experimental.ExperimentalNativeApi
import com.tencent.tmm.knoi.annotation.ServiceProvider
import com.tencent.tmm.knoi.type.JSValue
import com.tencent.tmm.knoi.mainTid
import com.tencent.kuikly.core.exception.*

@ServiceProvider(singleton = true)
open class KuiklyDemoBuglyService {
    @OptIn(ExperimentalNativeApi::class)
    fun registerUnhandledExceptionHook(reportOperation: (Array<JSValue>) -> Unit) {
        var old: ReportUnhandledExceptionHook? = null
        old = setUnhandledExceptionHook {
            val errorName = it::class.qualifiedName ?: "unknown"
            val message = it.message ?: "unknown"
            val callstack = it.getStackTraceForBuglyReport()

            reportOperation(
                arrayOf(
                    JSValue.createJSValue(errorName, mainTid),
                    JSValue.createJSValue(message, mainTid),
                    JSValue.createJSValue(callstack, mainTid),
                    JSValue.createJSValue(true, mainTid)
                )
            )
            old?.invoke(it)
            terminateWithUnhandledException(it)
        }
    }
}
