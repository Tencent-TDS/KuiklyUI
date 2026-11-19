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

package com.tencent.kuikly.compose.ui

import com.tencent.kuikly.compose.ui.geometry.Size
import com.tencent.kuikly.compose.ui.graphics.drawscope.CanvasDrawScope
import com.tencent.kuikly.compose.ui.graphics.drawscope.DrawScope
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.LayoutDirection
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.manager.NativeMethod
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.nvi.NativeBridge
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.views.CanvasView

internal class RecordedCommand(val method: String, val params: String?) {
    fun json(): JSONObject = JSONObject(params ?: "{}")
    fun intervals(): List<Double>? {
        val array = json().optJSONArray("intervals") ?: return null
        return List(array.length()) { array.optDouble(it) }
    }
}

internal class CanvasFixture(
    val pagerId: String,
    val createdShadows: List<Int>,
    val removedShadows: List<Int>,
    private val commands: MutableList<RecordedCommand>,
    private val density: Density,
) {
    private val view by lazy {
        CanvasView().also {
            it.pagerId = pagerId
            it.createRenderView()
        }
    }
    private val canvas = KuiklyCanvas()
    private val scope = CanvasDrawScope()

    fun draw(block: DrawScope.() -> Unit) {
        commands.clear()
        canvas.view = view
        scope.draw(density, LayoutDirection.Ltr, canvas, Size(200f, 200f), block)
        canvas.view = null
    }

    /** Commands of the last draw pass without the bind-time `reset` and the save/restore pair DrawScope.draw adds. */
    fun commandNames(): List<String> =
        commands.map { it.method }.filterNot { it == "reset" || it == "save" || it == "restore" }

    fun commandsNamed(method: String): List<RecordedCommand> = commands.filter { it.method == method }

    /** Commands recorded after the first occurrence of [method] in the last draw pass. */
    fun commandsAfterFirst(method: String): List<RecordedCommand> =
        commands.drop(commands.indexOfFirst { it.method == method } + 1)
}

internal fun withCanvasFixture(
    measuredSize: String = "0|0",
    block: (CanvasFixture) -> Unit,
) {
    val density = Density(2f)
    val created = mutableListOf<Int>()
    val removed = mutableListOf<Int>()
    val pagerId = "KuiklyCanvasLoweringTest"
    val pageName = pagerId
    val commands = mutableListOf<RecordedCommand>()
    val bridge = NativeBridge().apply {
        delegate = object : NativeBridge.NativeBridgeDelegate {
            override fun callNative(
                methodId: Int,
                arg0: Any?,
                arg1: Any?,
                arg2: Any?,
                arg3: Any?,
                arg4: Any?,
                arg5: Any?
            ): Any? = when (methodId) {
                NativeMethod.CALL_VIEW_METHOD -> {
                    commands += RecordedCommand(arg2 as String, arg3 as? String)
                    null
                }
                NativeMethod.CREATE_SHADOW -> {
                    created.add(arg1 as Int)
                    null
                }
                NativeMethod.REMOVE_SHADOW -> {
                    removed.add(arg1 as Int)
                    null
                }
                NativeMethod.CALCULATE_RENDER_VIEW_SIZE -> measuredSize
                else -> null
            }
        }
    }
    @Suppress("DEPRECATION")
    val previousPageId = BridgeManager.currentPageId
    @Suppress("DEPRECATION")
    fun setCurrentPageId(value: String) {
        BridgeManager.currentPageId = value
    }
    BridgeManager.registerNativeBridge(pagerId, bridge)
    PagerManager.registerPageRouter(pageName) {
        object : Pager() {
            override fun body(): ViewBuilder = {}
        }
    }
    setCurrentPageId(pagerId)
    PagerManager.createPager(pagerId, pageName, "{\"density\":${density.density}}")
    try {
        block(CanvasFixture(pagerId, created, removed, commands, density))
    } finally {
        PagerManager.destroyPager(pagerId)
        setCurrentPageId(previousPageId)
    }
}
