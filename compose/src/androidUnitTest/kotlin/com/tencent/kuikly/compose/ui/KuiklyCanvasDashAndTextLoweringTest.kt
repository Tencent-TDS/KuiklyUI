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

import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.geometry.Size
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.PathEffect
import com.tencent.kuikly.compose.ui.graphics.StrokeCap
import com.tencent.kuikly.compose.ui.graphics.drawscope.CanvasDrawScope
import com.tencent.kuikly.compose.ui.graphics.drawscope.DrawScope
import com.tencent.kuikly.compose.ui.graphics.drawscope.Stroke
import com.tencent.kuikly.compose.ui.graphics.drawscope.withTransform
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The Compose canvas talks to every renderer through `callViewMethod` commands on the
 * CanvasView. These teeth pin how [PathEffect] and the text primitive are lowered onto
 * that wire: which commands go out, in what order, and in density-independent units.
 */
class KuiklyCanvasDashAndTextLoweringTest {

    private val density = Density(density = 2f)

    @Test
    fun dashedStrokeLowersToLineDashInDensityIndependentUnits() {
        withCanvasFixture { fixture ->
            fixture.draw {
                drawLine(
                    color = Color.Red,
                    start = Offset(0f, 0f),
                    end = Offset(100f, 0f),
                    strokeWidth = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )
            }
            val lineDash = fixture.commandsNamed("lineDash")
            assertEquals(1, lineDash.size)
            assertEquals(listOf(6.0, 4.0), lineDash.single().intervals())
            assertEquals(
                listOf("beginPath", "moveTo", "lineTo", "strokeStyle", "lineWidth", "lineDash", "stroke"),
                fixture.commandNames()
            )
        }
    }

    @Test
    fun unchangedDashIsSentOnceAndClearedWhenTheNextStrokeIsSolid() {
        withCanvasFixture { fixture ->
            val dashed = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
            fixture.draw {
                drawRect(Color.Blue, topLeft = Offset.Zero, size = Size(10f, 10f), style = dashed)
                drawCircle(Color.Blue, radius = 5f, style = dashed)
                drawRect(Color.Blue, topLeft = Offset.Zero, size = Size(10f, 10f), style = Stroke(width = 2f))
            }
            val lineDash = fixture.commandsNamed("lineDash")
            assertEquals(2, lineDash.size, "second dashed stroke must reuse the sticky lineDash")
            assertEquals(listOf(3.0, 2.0), lineDash[0].intervals())
            assertNull(lineDash[1].intervals(), "solid stroke after a dash must clear lineDash")
        }
    }

    @Test
    fun dashStateIsResetWhenTheCanvasIsReboundToTheView() {
        withCanvasFixture { fixture ->
            val dashed = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
            fixture.draw { drawRect(Color.Blue, size = Size(10f, 10f), style = dashed) }
            assertEquals(1, fixture.commandsNamed("lineDash").size)
            // The fixture records one draw pass at a time; the rebind resets the native canvas,
            // so the same dash must go out again instead of being treated as already applied.
            fixture.draw { drawRect(Color.Blue, size = Size(10f, 10f), style = dashed) }
            assertEquals(1, fixture.commandsNamed("lineDash").size, "reset clears native dash state; resend after rebind")
        }
    }

    @Test
    fun fillStrokeDoesNotEmitLineDash() {
        withCanvasFixture { fixture ->
            fixture.draw { drawRect(Color.Blue, size = Size(10f, 10f)) }
            assertTrue(fixture.commandsNamed("lineDash").isEmpty())
        }
    }

    @Test
    fun dashPathEffectRejectsOddOrNegativeIntervals() {
        assertFailsWith<IllegalArgumentException> { PathEffect.dashPathEffect(floatArrayOf(4f)) }
        assertFailsWith<IllegalArgumentException> { PathEffect.dashPathEffect(floatArrayOf(4f, 2f, 1f)) }
        assertFailsWith<IllegalArgumentException> { PathEffect.dashPathEffect(floatArrayOf(4f, -2f)) }
        assertEquals(
            PathEffect.dashPathEffect(floatArrayOf(4f, 2f)),
            PathEffect.dashPathEffect(floatArrayOf(4f, 2f))
        )
    }

    @Test
    fun strokeEqualityIncludesPathEffect() {
        val dashed = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
        assertEquals(dashed, Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))))
        assertTrue(dashed != Stroke(width = 2f))
        assertTrue(dashed.hashCode() != Stroke(width = 2f).hashCode())
    }

    @Test
    fun phaseOnlyChangeDoesNotResendLineDash() {
        withCanvasFixture { fixture ->
            fixture.draw {
                drawLine(Color.Red, Offset.Zero, Offset(10f, 0f), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
                drawLine(Color.Red, Offset.Zero, Offset(10f, 0f), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), phase = 3f))
            }
            assertEquals(1, fixture.commandsNamed("lineDash").size, "phase is not lowered, so the command is identical")
        }
    }

    @Test
    fun capAndDashAreResentAfterRestore() {
        withCanvasFixture { fixture ->
            val dashed = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
            val solidRound = Stroke(width = 2f, cap = StrokeCap.Round)
            fixture.draw {
                drawRect(Color.Blue, size = Size(10f, 10f), style = dashed)
                withTransform({ translate(1f, 1f) }) {
                    drawRect(Color.Blue, size = Size(10f, 10f), style = solidRound)
                }
                // iOS, OHOS and web roll the dash and cap back on restore; Android keeps them.
                // The canvas cannot know which, so it must state both again.
                drawRect(Color.Blue, size = Size(10f, 10f), style = solidRound)
            }
            val afterRestore = fixture.commandsAfterFirst("restore")
            val stroke = afterRestore.indexOfFirst { it.method == "stroke" }
            val beforeStroke = afterRestore.subList(0, stroke)
            assertEquals("round", beforeStroke.single { it.method == "lineCap" }.json().optString("style"))
            assertNull(beforeStroke.single { it.method == "lineDash" }.intervals())
        }
    }

    private class RecordedCommand(val method: String, val params: String?) {
        fun json(): JSONObject = JSONObject(params ?: "{}")
        fun intervals(): List<Double>? {
            val array = json().optJSONArray("intervals") ?: return null
            return List(array.length()) { array.optDouble(it) }
        }
    }

    private class CanvasFixture(
        private val view: CanvasView,
        private val commands: MutableList<RecordedCommand>,
        private val density: Density,
    ) {
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

    private fun withCanvasFixture(
        measuredSize: String = "0|0",
        block: (CanvasFixture) -> Unit,
    ) {
        val pagerId = "KuiklyCanvasDashAndTextLoweringTest"
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
            val view = CanvasView()
            view.pagerId = pagerId
            view.createRenderView()
            block(CanvasFixture(view, commands, density))
        } finally {
            PagerManager.destroyPager(pagerId)
            setCurrentPageId(previousPageId)
        }
    }
}
