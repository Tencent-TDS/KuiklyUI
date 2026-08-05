/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.android.demo.module

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * 迁移自 core-render-android 中已删除的 KRDebugModuleTest，
 * 覆盖 Demo Android 宿主侧 KRDebugModule 的核心回归场景：
 *
 * 1. 从未 startTrace 就调用 stopTrace，应返回空字符串（不崩溃、不返回 null）。
 * 2. exportTraceFile 在有已保存 trace 文件路径时返回该绝对路径。
 *
 * 这两个用例都是纯 JVM 断言，不依赖 Android runtime，可以在 unit test 阶段执行。
 */
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
