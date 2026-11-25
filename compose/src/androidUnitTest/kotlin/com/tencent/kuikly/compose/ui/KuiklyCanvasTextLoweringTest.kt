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
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.StrokeCap
import com.tencent.kuikly.compose.ui.graphics.drawscope.Stroke
import com.tencent.kuikly.compose.ui.graphics.drawscope.withTransform
import com.tencent.kuikly.compose.ui.text.ExperimentalTextApi
import com.tencent.kuikly.compose.ui.text.TextMeasurer
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.drawText
import com.tencent.kuikly.compose.ui.text.drawTextAtBaseline
import com.tencent.kuikly.compose.ui.text.font.FontFamily
import com.tencent.kuikly.compose.ui.text.font.FontStyle
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The Compose canvas talks to every renderer through `callViewMethod` commands on the
 * CanvasView. These teeth pin how the text primitive is lowered onto that wire: which
 * commands go out, in what order, and in density-independent units.
 */
@OptIn(ExperimentalTextApi::class)
class KuiklyCanvasTextLoweringTest {

    @Test
    fun drawTextLowersFontAlignColorAndBaselineOrigin() {
        withCanvasFixture { fixture ->
            fixture.draw {
                drawTextAtBaseline(
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
            fixture.draw { drawTextAtBaseline("x", Offset(2f, 4f)) }
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
                drawTextAtBaseline("a", Offset(0f, 10f), style)
                drawTextAtBaseline("", Offset(0f, 20f), style)
                drawTextAtBaseline("b", Offset(0f, 30f), style)
                drawTextAtBaseline("c", Offset(0f, 40f), style.copy(fontSize = 16.sp))
            }
            assertEquals(2, fixture.commandsNamed("font").size)
            assertEquals(1, fixture.commandsNamed("textAlign").size)
            assertEquals(listOf("a", "b", "c"), fixture.commandsNamed("fillText").map { it.json().optString("text") })
        }
    }

    @Test
    fun strokedDrawTextUsesStrokeTextWithTheStrokeWidthAndCap() {
        withCanvasFixture { fixture ->
            fixture.draw {
                drawTextAtBaseline(
                    "outline",
                    Offset(0f, 0f),
                    TextStyle(color = Color.Green),
                    drawStyle = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }
            assertEquals(
                listOf("font", "textAlign", "strokeStyle", "lineWidth", "lineCap", "strokeText"),
                fixture.commandNames()
            )
            assertEquals(1.5, fixture.commandsNamed("lineWidth").single().json().optDouble("width"))
            assertEquals("round", fixture.commandsNamed("lineCap").single().json().optString("style"))
        }
    }

    @Test
    fun strokedDrawTextSkipsTheLineCapWhenItMatchesTheRendererState() {
        withCanvasFixture { fixture ->
            fixture.draw {
                // `reset` on bind leaves the renderer on butt caps, so the first run sends none.
                drawTextAtBaseline("a", Offset(0f, 0f), drawStyle = Stroke(width = 2f, cap = StrokeCap.Butt))
                drawTextAtBaseline("b", Offset(0f, 0f), drawStyle = Stroke(width = 2f, cap = StrokeCap.Square))
                drawTextAtBaseline("c", Offset(0f, 0f), drawStyle = Stroke(width = 2f, cap = StrokeCap.Square))
            }
            assertEquals(listOf("square"), fixture.commandsNamed("lineCap").map { it.json().optString("style") })
        }
    }

    @Test
    fun measuresBeforeAnyCanvasExistsAndDrawsBothOverloadsAtTheTopLeft() {
        withCanvasFixture(measuredSize = "50|20") { fixture ->
            val measurer = TextMeasurer(fixture.pagerId, density = 2f, pageDensity = 2f, cacheSize = 8)
            val style = TextStyle(fontSize = 15.sp, textAlign = TextAlign.Right)
            val layout = measurer.measure("Ag", style)
            assertEquals(IntSize(100, 40), layout.size)
            assertEquals(35f, layout.firstBaseline)
            assertEquals(1, fixture.createdShadows.size)
            assertEquals(fixture.createdShadows, fixture.removedShadows)
            fixture.draw {
                drawText(layout, topLeft = Offset(20f, 10f))
                drawText(measurer, "Ag", topLeft = Offset.Zero, style = style)
            }
            val calls = fixture.commandsNamed("fillText").map { it.json() }
            assertEquals(listOf(10.0, 0.0), calls.map { it.optDouble("x") })
            assertEquals(listOf(22.5, 17.5), calls.map { it.optDouble("y") })
            assertEquals(1, fixture.createdShadows.size, "neither draw should remeasure")
        }
    }

    @Test
    fun independentMeasurementUsesLocalDensityAndTheTextComponentsWeightMapping() {
        withCanvasFixture(measuredSize = "20|20") { fixture ->
            val tm = TextMeasurer(fixture.pagerId, density = 3f, pageDensity = 2f, cacheSize = 2)
            val thin = tm.measure("A", TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Thin))
            val black = tm.measure("A", TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Black))
            assertEquals(35f, thin.firstBaseline) // 30 px font in a 40 px line
            fixture.draw {
                drawText(thin)
                drawText(black)
            }
            val fonts = fixture.commandsNamed("font").map { it.json() }
            assertEquals(listOf("300", "700"), fonts.map { it.optString("weight") })
            assertEquals(listOf(15.0, 15.0), fonts.map { it.optDouble("size") })
        }
    }

    @Test
    fun cacheEvictsLeastRecentlyUsedLayoutAndKeysByStyle() {
        withCanvasFixture(measuredSize = "50|20") { fixture ->
            val tm = TextMeasurer(fixture.pagerId, 2f, 2f, cacheSize = 2)
            val a = tm.measure("a")
            val b = tm.measure("b")
            assertSame(a, tm.measure("a"))
            tm.measure("a", TextStyle(fontSize = 20.sp))
            assertSame(a, tm.measure("a"))
            assertNotSame(b, tm.measure("b"))
            assertEquals(4, fixture.createdShadows.size)
        }
    }

    @Test
    fun zeroCacheRemeasuresAndFailedMeasurementsReleaseTheShadow() {
        withCanvasFixture(measuredSize = "20|20") { fixture ->
            val measurer = TextMeasurer(fixture.pagerId, 2f, 2f, cacheSize = 0)
            repeat(2) { measurer.measure("a") }
            assertEquals(2, fixture.createdShadows.size)
            assertEquals(IntSize.Zero, measurer.measure("").size)
            assertFailsWith<IllegalArgumentException> { measurer.measure("a\nb") }
            assertEquals(2, fixture.createdShadows.size)
        }
        withCanvasFixture(measuredSize = "invalid|size") { fixture ->
            val measurer = TextMeasurer(fixture.pagerId, 2f, 2f, cacheSize = 2)
            repeat(2) { assertFailsWith<NumberFormatException> { measurer.measure("a") } }
            assertEquals(2, fixture.createdShadows.size)
            assertEquals(fixture.createdShadows, fixture.removedShadows)
        }
    }

    @Test
    fun fontAndAlignAreResentAfterRestore() {
        withCanvasFixture { fixture ->
            val inner = TextStyle(fontSize = 20.sp, textAlign = TextAlign.Center)
            fixture.draw {
                drawTextAtBaseline("a", Offset(0f, 10f), TextStyle(fontSize = 12.sp))
                withTransform({ translate(1f, 1f) }) {
                    drawTextAtBaseline("b", Offset(0f, 20f), inner)
                }
                // iOS, OHOS and web roll font and alignment back on restore; Android keeps
                // them. The canvas cannot know which, so it must state both again.
                drawTextAtBaseline("c", Offset(0f, 30f), inner)
            }
            val afterRestore = fixture.commandsAfterFirst("restore")
            assertEquals(listOf("font", "textAlign", "fillStyle", "fillText"), afterRestore.map { it.method }.take(4))
            assertEquals(20.0, afterRestore.first { it.method == "font" }.json().optDouble("size"))
            assertEquals("center", afterRestore.first { it.method == "textAlign" }.params)
        }
    }
}
