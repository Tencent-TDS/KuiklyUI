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

package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.material3.Slider
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment.Companion.CenterHorizontally
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.DefaultCameraDistance
import com.tencent.kuikly.compose.ui.graphics.TransformOrigin
import com.tencent.kuikly.compose.ui.graphics.graphicsLayer
import com.tencent.kuikly.compose.ui.text.font.FontFamily
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import kotlin.math.round
import kotlin.math.roundToInt

@Page("graphicslayer3")
internal class GraphicsLayerDemo3 : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar {
                GraphicsLayerSettings()
            }
        }
    }
}

@Composable
fun GraphicsLayerSettings() {
    var scaleX by remember { mutableStateOf(1f) }
    var scaleY by remember { mutableStateOf(1f) }
    var translationX by remember { mutableStateOf(0f) }
    var translationY by remember { mutableStateOf(0f) }
    var rotationX by remember { mutableStateOf(0f) }
    var rotationY by remember { mutableStateOf(0f) }
    var rotationZ by remember { mutableStateOf(0f) }
    var cameraDistance by remember { mutableStateOf(DefaultCameraDistance) }
    var originX by remember { mutableStateOf(0.5f) }
    var originY by remember { mutableStateOf(0.5f) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(20.dp),
    ) {
        Box(
            Modifier
                .graphicsLayer(
                    scaleX = scaleX,
                    scaleY = scaleY,
                    translationX = translationX,
                    translationY = translationY,
                    rotationX = rotationX,
                    rotationY = rotationY,
                    rotationZ = rotationZ,
                    cameraDistance = cameraDistance,
                    transformOrigin = TransformOrigin(originX, originY),
                ).align(CenterHorizontally)
                .size(200.dp)
                .background(Color.Cyan),
            // .pointerHoverIcon(PointerIcon.Hand)
        ) {
            Box(
                modifier =
                    Modifier
                        .offset(10.dp, 10.dp)
                        .size(80.dp)
                        .background(Color.Magenta),
                // .pointerHoverIcon(PointerIcon.Crosshair)
            )
        }

        Spacer(Modifier.height(20.dp))
        SliderSetting("ScaleX", scaleX, 0.5f..2f) { scaleX = it }
        SliderSetting("ScaleY", scaleY, 0.5f..2f) { scaleY = it }
        SliderSetting("TranslationX", translationX, -250f..250f) { translationX = it }
        SliderSetting("TranslationY", translationY, -250f..250f) { translationY = it }
        SliderSetting("RotateX", rotationX, -180f..180f) { rotationX = it }
        SliderSetting("RotateY", rotationY, -180f..180f) { rotationY = it }
        SliderSetting("RotateZ", rotationZ, -180f..180f) { rotationZ = it }
        SliderSetting("OriginX", originX, 0f..1f) { originX = it }
        SliderSetting("OriginY", originY, 0f..1f) { originY = it }
        SliderSetting("CameraDistance", cameraDistance, 3f..30f) { cameraDistance = it }
    }
}

@Composable
fun SliderSetting(
    text: String,
    value: Float,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Row {
            Text(
                text = text,
                fontSize = 12.sp,
                modifier = Modifier.width(120.dp).weight(1f),
            )
            Text(
                text = "${round(value * 10f) / 10f}",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.width(50.dp),
            )
        }
        var steps = ((range.endInclusive - range.start) / 0.1f).roundToInt()
        if (steps > 100) steps /= 10
        if (steps > 100) steps /= 10
        Slider(
            value = value,
            onValueChange,
            valueRange = range,
            steps = steps - 1,
            modifier = Modifier.height(25.dp),
        )
    }
}
