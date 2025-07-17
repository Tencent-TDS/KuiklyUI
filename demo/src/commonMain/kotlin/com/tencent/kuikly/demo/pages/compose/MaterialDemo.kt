package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.animation.core.animateFloatAsState
import com.tencent.kuikly.compose.foundation.Canvas
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.material3.CircularProgressIndicator
import com.tencent.kuikly.compose.material3.LinearProgressIndicator
import com.tencent.kuikly.compose.material3.LocalContentColor
import com.tencent.kuikly.compose.material3.MaterialTheme
import com.tencent.kuikly.compose.material3.ProgressIndicatorDefaults
import com.tencent.kuikly.compose.material3.Slider
import com.tencent.kuikly.compose.material3.Switch
import com.tencent.kuikly.compose.material3.SwitchDefaults
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Path
import com.tencent.kuikly.compose.ui.graphics.StrokeCap
import com.tencent.kuikly.compose.ui.graphics.drawscope.scale
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.semantics.contentDescription
import com.tencent.kuikly.compose.ui.semantics.semantics
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

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
                SecondaryText("Switch with Thumb Icon")
                SwitchWithThumbIconSample()
                Text("Linear Progress Indicator")
                LinearProgressIndicatorSample()
                SecondaryText("Legacy Linear Progress Indicator")
                LegacyLinearProgressIndicatorSample()
                SecondaryText("Indeterminate Linear Progress Indicator")
                IndeterminateLinearProgressIndicatorSample()
                SecondaryText("Legacy Indeterminate Linear Progress Indicator")
                LegacyIndeterminateLinearProgressIndicatorSample()
                Text("Circular Progress Indicator")
                CircularProgressIndicatorSample()
                SecondaryText("Legacy Circular Progress Indicator")
                LegacyCircularProgressIndicatorSample()
                SecondaryText("Indeterminate Circular Progress Indicator")
                IndeterminateCircularProgressIndicatorSample()
                SecondaryText("Legacy Indeterminate Circular Progress Indicator")
                LegacyIndeterminateCircularProgressIndicatorSample()
            }
        }
    }
}

@Composable
private fun SecondaryText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.secondary,
        fontSize = 12.sp
    )
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

@Composable
private fun LinearProgressIndicatorSample() {
    var progress by remember { mutableStateOf(0.2f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { animatedProgress },
        )
        Row(modifier = Modifier.padding(10.dp)) {
            SecondaryText("Set progress:")
            Slider(
                modifier = Modifier.width(150.dp).height(12.dp),
                value = progress,
                valueRange = 0f..1f,
                onValueChange = { progress = it },
            )
        }
    }
}

@Composable
private fun LegacyLinearProgressIndicatorSample() {
    var progress by remember { mutableStateOf(0.2f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { animatedProgress },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
        Row(modifier = Modifier.padding(10.dp)) {
            SecondaryText("Set progress:")
            Slider(
                modifier = Modifier.width(150.dp).height(12.dp),
                value = progress,
                valueRange = 0f..1f,
                onValueChange = { progress = it },
            )
        }
    }
}

@Composable
private fun IndeterminateLinearProgressIndicatorSample() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator()
    }
}

@Composable
private fun LegacyIndeterminateLinearProgressIndicatorSample() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp
        )
    }
}

@Composable
private fun CircularProgressIndicatorSample() {
    var progress by remember { mutableStateOf(0.2f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator(progress = { animatedProgress })
        Row(modifier = Modifier.padding(10.dp)) {
            SecondaryText("Set progress:")
            Slider(
                modifier = Modifier.width(150.dp).height(12.dp),
                value = progress,
                valueRange = 0f..1f,
                onValueChange = { progress = it },
            )
        }
    }
}

@Composable
private fun LegacyCircularProgressIndicatorSample() {
    var progress by remember { mutableStateOf(0.2f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp
        )
        Row(modifier = Modifier.padding(10.dp)) {
            SecondaryText("Set progress:")
            Slider(
                modifier = Modifier.width(150.dp).height(12.dp),
                value = progress,
                valueRange = 0f..1f,
                onValueChange = { progress = it },
            )
        }
    }
}

@Composable
private fun IndeterminateCircularProgressIndicatorSample() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LegacyIndeterminateCircularProgressIndicatorSample() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator(strokeCap = StrokeCap.Butt)
    }
}
