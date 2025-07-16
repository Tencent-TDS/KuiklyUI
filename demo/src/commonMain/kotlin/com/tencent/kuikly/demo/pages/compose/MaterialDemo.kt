package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.Canvas
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.material3.LocalContentColor
import com.tencent.kuikly.compose.material3.Switch
import com.tencent.kuikly.compose.material3.SwitchDefaults
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Path
import com.tencent.kuikly.compose.ui.graphics.drawscope.scale
import com.tencent.kuikly.compose.ui.graphics.drawscope.translate
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.semantics.contentDescription
import com.tencent.kuikly.compose.ui.semantics.semantics
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder

@Page("material_demo")
internal class MaterialDemo : ComposeContainer() {

    override fun willInit() {
        setContent {
            DemoScaffold(
                title = "Material Demo",
                back = true
            ) {
                Text("Switch")
                SwitchSample()
                Text("Switch with Thumb Icon")
                SwitchWithThumbIconSample()
            }
        }
    }
}

@Composable
private fun SwitchSample() {
    var checked by remember { mutableStateOf(true) }
    Switch(
        modifier = Modifier.semantics { contentDescription = "Demo" },
        checked = checked,
        onCheckedChange = { checked = it }
    )
}

@Composable
private fun SwitchWithThumbIconSample() {
    var checked by remember { mutableStateOf(true) }

    Switch(
        modifier = Modifier.semantics { contentDescription = "Demo with icon" },
        checked = checked,
        onCheckedChange = { checked = it },
        thumbContent = {
            if (checked) {
                // Icon isn't focusable, no need for content description
                val color = LocalContentColor.current
                val canvasSize = with(LocalDensity.current) { SwitchDefaults.IconSize.toPx() }
                val iconDimension = IconChecked.getBounds().let { it.width + it.left * 2 }
                Canvas(modifier = Modifier.size(SwitchDefaults.IconSize)) {
                    scale(canvasSize / iconDimension, Offset.Zero) {
                        drawPath(IconChecked, color)
                    }
                }
            }
        }
    )
}

private val IconChecked by lazy(LazyThreadSafetyMode.NONE) {
    Path().apply {
        moveTo(9.0f, 16.17f)
        lineTo(4.83f, 12.0f)
        relativeLineTo(-1.42f, 1.41f)
        lineTo(9.0f, 19.0f)
        lineTo(21.0f, 7.0f)
        relativeLineTo(-1.41f, -1.41f)
        close()
    }
}