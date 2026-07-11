package com.tencent.kuikly.demo.pages.compose

import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.Canvas
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.core.annotations.Page

import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.setContent
import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.ui.unit.dp

/**
 * Reproduction page for Issue #1362: Missing drawText support in Compose Canvas.
 */
@Page("DrawTextReproDemo")
internal class DrawTextReproDemo : ComposeContainer() {
    override fun willInit() {
        setContent {
            ReproContent()
        }
    }

    @Composable
    private fun ReproContent() {
        DemoScaffold("drawText Repro", back = true) {
            // Use a fixed height for the Canvas because it's inside a LazyColumn (via DemoScaffold)
            // fillMaxSize() inside a scrollable container often results in zero height.
            Canvas(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                // 1. Attempt to draw a background to show Canvas is working
                drawRect(color = Color.LightGray)

                // 2. Test drawText at different positions
                drawText("Hello Kuikly (TopLeft)", Color.Black, topLeft = Offset(20f, 20f))
                drawText("Hello Kuikly (Center)", Color.Red, topLeft = Offset(size.width / 2, size.height / 2))
                drawText("Hello Kuikly (Big)", Color.Blue, topLeft = Offset(20f, 150f), style = TextStyle(fontSize = 30.sp))
            }
        }
    }
}
