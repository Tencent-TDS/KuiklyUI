package com.tencent.kuikly.core.render.android.expand.module

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class KRDebugModuleTest {

    @Test
    fun stopTrace_returnsEmptyPath_whenTracingNeverStarted() {
        val module = KRDebugModule()

        assertEquals("", module.call("stopTrace", null, null))
    }

    @Test
    fun exportTraceFile_returnsStoredTracePath() {
        val module = KRDebugModule()
        val traceFile = File("/tmp/kuikly_list_scroll.trace")
        val currentTraceFileField = module.javaClass.getDeclaredField("currentTraceFile")
        currentTraceFileField.isAccessible = true
        currentTraceFileField.set(module, traceFile)

        assertEquals(traceFile.absolutePath, module.call("exportTraceFile", null, null))
    }
}
