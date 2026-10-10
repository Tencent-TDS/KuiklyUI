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
import com.tencent.kuikly.compose.ui.graphics.StrokeCap
import com.tencent.kuikly.compose.ui.graphics.drawscope.CanvasDrawScope
import com.tencent.kuikly.compose.ui.graphics.drawscope.CanvasTextMetrics
import com.tencent.kuikly.compose.ui.graphics.drawscope.DrawScope
import com.tencent.kuikly.compose.ui.graphics.drawscope.Stroke
import com.tencent.kuikly.compose.ui.graphics.drawscope.drawText
import com.tencent.kuikly.compose.ui.graphics.drawscope.measureText
import com.tencent.kuikly.compose.ui.graphics.drawscope.withTransform
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.font.FontFamily
import com.tencent.kuikly.compose.ui.text.font.FontStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.LayoutDirection
import com.tencent.kuikly.compose.ui.unit.sp
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The Compose canvas talks to every renderer through `callViewMethod` commands on the
 * CanvasView. These teeth pin how the text primitive is lowered onto that wire: which
 * commands go out, in what order, and in density-independent units.
 */
class KuiklyCanvasTextLoweringTest {

    private val density = Density(density = 2f)

    @Test
    fun drawTextLowersFontAlignColorAndBaselineOrigin() {
        withCanvasFixture { fixture ->
            fixture.draw {
                drawText(
                    text = "Node A",
                    origin = Offset(40f, 60f),
                    style = TextStyle(
                        color = Color.Red,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                )
            }
            assertEquals(
                listOf("font", "textAlign", "fillStyle", "fillText"),
                fixture.commandNames()
            )
            val font = fixture.commandsNamed("font").single().json()
            assertEquals("italic", font.optString("style"))
            assertEquals("700", font.optString("weight"))
            assertEquals(14.0, font.optDouble("size"))
            assertEquals("monospace", font.optString("family"))
            assertEquals("center", fixture.commandsNamed("textAlign").single().params)
            val fillText = fixture.commandsNamed("fillText").single().json()
            assertEquals("Node A", fillText.optString("text"))
            assertEquals(20.0, fillText.optDouble("x"))
            assertEquals(30.0, fillText.optDouble("y"))
        }
    }

    @Test
    fun drawTextDefaultsToRendererFontAndStartAlignment() {
        withCanvasFixture { fixture ->
            fixture.draw { drawText("x", Offset(2f, 4f)) }
            val font = fixture.commandsNamed("font").single().json()
            assertEquals("normal", font.optString("style"))
            assertEquals("400", font.optString("weight"))
            assertEquals(15.0, font.optDouble("size"))
            assertTrue(!font.has("family"))
            assertEquals("left", fixture.commandsNamed("textAlign").single().params)
        }
    }

    @Test
    fun drawTextReusesFontAndAlignAcrossRunsAndSkipsEmptyText() {
        withCanvasFixture { fixture ->
            val style = TextStyle(fontSize = 12.sp)
            fixture.draw {
                drawText("a", Offset(0f, 10f), style)
                drawText("", Offset(0f, 20f), style)
                drawText("b", Offset(0f, 30f), style)
                drawText("c", Offset(0f, 40f), style.copy(fontSize = 16.sp))
            }
            assertEquals(2, fixture.commandsNamed("font").size)
            assertEquals(1, fixture.commandsNamed("textAlign").size)
            assertEquals(listOf("a", "b", "c"), fixture.commandsNamed("fillText").map { it.json().optString("text") })
        }
    }

    @Test
    fun strokedDrawTextUsesStrokeTextWithTheStrokeWidth() {
        withCanvasFixture { fixture ->
            fixture.draw {
                drawText(
                    "outline",
                    Offset(0f, 0f),
                    TextStyle(color = Color.Green),
                    drawStyle = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }
            assertEquals(
                listOf("font", "textAlign", "strokeStyle", "lineWidth", "strokeText"),
                fixture.commandNames()
            )
            assertEquals(1.5, fixture.commandsNamed("lineWidth").single().json().optDouble("width"))
        }
    }

    @Test
    fun measureTextScalesRendererMetricsBackToPixels() {
        withCanvasFixture(measuredSize = "50|20") { fixture ->
            var metrics: CanvasTextMetrics? = null
            fixture.draw { metrics = measureText("hello", TextStyle(fontSize = 15.sp)) }
            val measured = assertNotNull(metrics)
            assertEquals(100f, measured.width)
            assertEquals(40f, measured.height)
            // Font size 15dp in a 20dp line box: the renderer splits the leading evenly.
            assertEquals(35f, measured.ascent)
            assertEquals(5f, measured.descent)
            assertEquals(1, fixture.commandsNamed("font").size, "measure pushes the font state once")
        }
    }

    @Test
    fun measureTextOutsideADrawPassReturnsNull() {
        val scope = CanvasDrawScope()
        var metrics: CanvasTextMetrics? = CanvasTextMetrics(1f, 1f, 1f)
        scope.draw(density, LayoutDirection.Ltr, KuiklyCanvas(), Size(10f, 10f)) {
            metrics = measureText("hello")
        }
        assertNull(metrics)
    }

    @Test
    fun fontAndAlignAreResentAfterRestore() {
        withCanvasFixture { fixture ->
            val inner = TextStyle(fontSize = 20.sp, textAlign = TextAlign.Center)
            fixture.draw {
                drawText("a", Offset(0f, 10f), TextStyle(fontSize = 12.sp))
                withTransform({ translate(1f, 1f) }) {
                    drawText("b", Offset(0f, 20f), inner)
                }
                // iOS, OHOS and web roll font and alignment back on restore; Android keeps
                // them. The canvas cannot know which, so it must state both again.
                drawText("c", Offset(0f, 30f), inner)
            }
            val afterRestore = fixture.commandsAfterFirst("restore")
            assertEquals(listOf("font", "textAlign", "fillStyle", "fillText"), afterRestore.map { it.method }.take(4))
            assertEquals(20.0, afterRestore.first { it.method == "font" }.json().optDouble("size"))
            assertEquals("center", afterRestore.first { it.method == "textAlign" }.params)
        }
    }

    private class RecordedCommand(val method: String, val params: String?) {
        fun json(): JSONObject = JSONObject(params ?: "{}")
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
        val pagerId = "KuiklyCanvasTextLoweringTest"
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
