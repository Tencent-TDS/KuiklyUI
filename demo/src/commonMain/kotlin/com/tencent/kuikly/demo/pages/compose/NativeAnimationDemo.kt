/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.animation.Crossfade
import com.tencent.kuikly.compose.animation.animateColor
import com.tencent.kuikly.compose.animation.animateColorAsState
import com.tencent.kuikly.compose.animation.core.Animatable
import com.tencent.kuikly.compose.animation.core.CubicBezierEasing
import com.tencent.kuikly.compose.animation.core.FiniteAnimationSpec
import com.tencent.kuikly.compose.animation.core.MutableTransitionState
import com.tencent.kuikly.compose.animation.core.Spring
import com.tencent.kuikly.compose.animation.core.animateFloat
import com.tencent.kuikly.compose.animation.core.animateFloatAsState
import com.tencent.kuikly.compose.animation.core.preferNative
import com.tencent.kuikly.compose.animation.core.snap
import com.tencent.kuikly.compose.animation.core.spring
import com.tencent.kuikly.compose.animation.core.tween
import com.tencent.kuikly.compose.animation.core.updateTransition
import com.tencent.kuikly.compose.animation.fadeIn
import com.tencent.kuikly.compose.animation.fadeOut
import com.tencent.kuikly.compose.animation.scaleIn
import com.tencent.kuikly.compose.animation.scaleOut
import com.tencent.kuikly.compose.animation.slideInHorizontally
import com.tencent.kuikly.compose.animation.slideInVertically
import com.tencent.kuikly.compose.animation.slideOutHorizontally
import com.tencent.kuikly.compose.animation.slideOutVertically
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.TransformOrigin
import com.tencent.kuikly.compose.ui.graphics.graphicsLayer
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.ui.window.Dialog
import com.tencent.kuikly.compose.ui.window.Popup
import com.tencent.kuikly.compose.ui.window.PopupProperties
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Page("NativeAnimationDemo")
class NativeAnimationDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("Native 属性动画") {
                NativeAnimationDemoContent()
            }
        }
    }
}

@Composable
private fun NativeAnimationDemoContent() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F5F8))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Native 属性动画验证",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "每组都可重复快速点击，用于观察完成回调、中断连续性和重组繁忙时的流畅度。",
                fontSize = 13.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
            )
        }
        item { ComposeNativeComparisonDemo() }
        item { NativeTransitionDemo() }
        item { NativeVisibilityDemo() }
        item { NativeAnimatableDemo() }
        item { NativeCrossfadeDemo() }
        item { NativeColorAsStateDemo() }
        item { NativeSpecMatrixDemo() }
        item { NativeTransformMatrixDemo() }
        item { NativeSlideDirectionsDemo() }
        item { NativeFallbackDemo() }
        item { NativeAnimationStressDemo() }
        item { NativeDialogContentDemo() }
        item { NativePopupContentDemo() }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ComposeNativeComparisonDemo() {
    DemoSection(
        title = "1. animateAsState：Compose / Native 对照",
        description = "分别触发同一 alpha 动画；Native 完成次数来自 finishedListener。"
    ) {
        var composeOpaque by remember { mutableStateOf(true) }
        var nativeOpaque by remember { mutableStateOf(true) }
        var nativeFinishedCount by remember { mutableIntStateOf(0) }
        val composeAlpha by animateFloatAsState(
            targetValue = if (composeOpaque) 1f else 0.15f,
            animationSpec = tween(1000, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)),
            label = "composeAlpha"
        )
        val nativeAlpha by animateFloatAsState(
            targetValue = if (nativeOpaque) 1f else 0.15f,
            animationSpec = tween<Float>(
                1000,
                easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
            ).preferNative(),
            label = "nativeAlpha",
            finishedListener = { nativeFinishedCount++ }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LabeledAnimatedBox("Compose", composeAlpha)
            LabeledAnimatedBox("Native", nativeAlpha)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoButton("Compose 切换") { composeOpaque = !composeOpaque }
            DemoButton("Native 切换") { nativeOpaque = !nativeOpaque }
        }
        Text("Native finishedListener：$nativeFinishedCount 次", fontSize = 12.sp)
    }
}

private enum class NativeCardState { Start, End }

@Composable
private fun NativeTransitionDemo() {
    DemoSection(
        title = "2. updateTransition：同 View 多属性",
        description = "分别触发 alpha、scale、rotation 和纯色背景动画。"
    ) {
        var composeState by remember { mutableStateOf(NativeCardState.Start) }
        var nativeState by remember { mutableStateOf(NativeCardState.Start) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TransitionComparisonCard("Compose", composeState, useNative = false)
            TransitionComparisonCard("Native", nativeState, useNative = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoButton("Compose 切换") {
                composeState = composeState.opposite()
            }
            DemoButton("Native 切换") {
                nativeState = nativeState.opposite()
            }
        }
    }
}

private fun NativeCardState.opposite(): NativeCardState =
    if (this == NativeCardState.Start) NativeCardState.End else NativeCardState.Start

@Composable
private fun TransitionComparisonCard(
    label: String,
    state: NativeCardState,
    useNative: Boolean
) {
    val transition = updateTransition(state, label = "${label}CardTransition")
    val baseSpec = tween<Float>(
        durationMillis = 900,
        easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    )
    val spec = if (useNative) baseSpec.preferNative() else baseSpec
    val alpha by transition.animateFloat({ spec }, label = "alpha") {
        if (it == NativeCardState.End) 0.45f else 1f
    }
    val scale by transition.animateFloat({ spec }, label = "scale") {
        if (it == NativeCardState.End) 1.35f else 0.8f
    }
    val rotation by transition.animateFloat({ spec }, label = "rotation") {
        if (it == NativeCardState.End) 135f else 0f
    }
    val backgroundColor by transition.animateColor(
        transitionSpec = {
            val colorSpec = tween<Color>(
                900,
                easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
            )
            if (useNative) colorSpec.preferNative() else colorSpec
        },
        label = "backgroundColor"
    ) {
        if (it == NativeCardState.End) Color(0xFFE91E63) else Color(0xFF2196F3)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                }
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = Color.White, fontSize = 12.sp)
        }
        Text(label, fontSize = 12.sp)
    }
}

@Composable
private fun NativeVisibilityDemo() {
    DemoSection(
        title = "3. AnimatedVisibility：fade + scale + slide",
        description = "分别 show/hide；快速点击可比较反向中断和退出节点保留。"
    ) {
        var composeVisible by remember { mutableStateOf(true) }
        var nativeVisible by remember { mutableStateOf(true) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            VisibilityComparison("Compose", composeVisible, useNative = false)
            VisibilityComparison("Native", nativeVisible, useNative = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoButton(if (composeVisible) "Compose Hide" else "Compose Show") {
                composeVisible = !composeVisible
            }
            DemoButton(if (nativeVisible) "Native Hide" else "Native Show") {
                nativeVisible = !nativeVisible
            }
        }
    }
}

@Composable
private fun VisibilityComparison(label: String, visible: Boolean, useNative: Boolean) {
    val baseEnterFloatSpec = tween<Float>(800)
    val baseExitFloatSpec = tween<Float>(800)
    val baseEnterOffsetSpec = tween<com.tencent.kuikly.compose.ui.unit.IntOffset>(800)
    val baseExitOffsetSpec = tween<com.tencent.kuikly.compose.ui.unit.IntOffset>(800)
    val enterFloatSpec =
        if (useNative) baseEnterFloatSpec.preferNative() else baseEnterFloatSpec
    val exitFloatSpec =
        if (useNative) baseExitFloatSpec.preferNative() else baseExitFloatSpec
    val enterOffsetSpec =
        if (useNative) baseEnterOffsetSpec.preferNative() else baseEnterOffsetSpec
    val exitOffsetSpec =
        if (useNative) baseExitOffsetSpec.preferNative() else baseExitOffsetSpec

    Column(
        modifier = Modifier.width(150.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 12.sp)
        Box(
            modifier = Modifier.width(150.dp).height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            com.tencent.kuikly.compose.animation.AnimatedVisibility(
                visible = visible,
                enter = fadeIn(enterFloatSpec) +
                    scaleIn(enterFloatSpec, initialScale = 0.6f) +
                    slideInVertically(enterOffsetSpec) { it / 2 },
                exit = fadeOut(exitFloatSpec) +
                    scaleOut(exitFloatSpec, targetScale = 0.6f) +
                    slideOutVertically(exitOffsetSpec) { it / 2 }
            ) {
                Box(
                    modifier = Modifier
                        .width(138.dp)
                        .height(88.dp)
                        .background(
                            if (useNative) Color(0xFF4CAF50) else Color(0xFF1976D2)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun NativeAnimatableDemo() {
    DemoSection(
        title = "4. Animatable.animateTo：Spring 与中断",
        description = "分别移动；连续点击可比较 Spring 和反向连续性。"
    ) {
        val composeTranslation = remember { Animatable(0f) }
        val nativeTranslation = remember { Animatable(0f) }
        val scope = rememberCoroutineScope()
        var composeMoveRight by remember { mutableStateOf(true) }
        var nativeMoveRight by remember { mutableStateOf(true) }
        MovementLane("Compose", composeTranslation.value, Color(0xFF1976D2))
        MovementLane("Native", nativeTranslation.value, Color(0xFFFF9800))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoButton("Compose 移动") {
                val target = if (composeMoveRight) 220f else 0f
                composeMoveRight = !composeMoveRight
                scope.launch {
                    composeTranslation.animateTo(
                        targetValue = target,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
                }
            }
            DemoButton("Native 移动") {
                val target = if (nativeMoveRight) 220f else 0f
                nativeMoveRight = !nativeMoveRight
                scope.launch {
                    nativeTranslation.animateTo(
                        targetValue = target,
                        animationSpec = spring<Float>(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ).preferNative()
                    )
                }
            }
        }
    }
}

@Composable
private fun MovementLane(label: String, translation: Float, color: Color) {
    Column {
        Text(label, fontSize = 12.sp)
        Box(modifier = Modifier.fillMaxWidth().height(58.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer { translationX = translation }
                    .background(color)
            )
        }
    }
}

@Composable
private fun NativeCrossfadeDemo() {
    DemoSection(
        title = "5. Crossfade",
        description = "Compose 与 Native 使用相同 700ms tween，分别切换。"
    ) {
        var composePage by remember { mutableIntStateOf(0) }
        var nativePage by remember { mutableIntStateOf(0) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CrossfadeComparison("Compose", composePage, useNative = false)
            CrossfadeComparison("Native", nativePage, useNative = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoButton("Compose 切换") { composePage++ }
            DemoButton("Native 切换") { nativePage++ }
        }
    }
}

@Composable
private fun CrossfadeComparison(label: String, page: Int, useNative: Boolean) {
    val baseSpec = tween<Float>(700)
    Column(
        modifier = Modifier.width(150.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 12.sp)
        Crossfade(
            targetState = page,
            animationSpec = if (useNative) baseSpec.preferNative() else baseSpec,
            label = "${label}Crossfade"
        ) { current ->
            Box(
                modifier = Modifier.width(150.dp).height(90.dp).background(
                    if (current % 2 == 0) Color(0xFF673AB7) else Color(0xFF009688)
                ),
                contentAlignment = Alignment.Center
            ) {
                Text("$label ${current % 2 + 1}", color = Color.White, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun NativeColorAsStateDemo() {
    DemoSection(
        title = "6. animateColorAsState：纯色背景",
        description = "只验证 sRGB 纯色背景；文字色、渐变和阴影不属于首期 Native 范围。"
    ) {
        var composePink by remember { mutableStateOf(false) }
        var nativePink by remember { mutableStateOf(false) }
        val composeColor by animateColorAsState(
            targetValue = if (composePink) Color(0xFFE91E63) else Color(0xFF00BCD4),
            animationSpec = tween(900),
            label = "composeBackgroundColor"
        )
        val nativeColor by animateColorAsState(
            targetValue = if (nativePink) Color(0xFFE91E63) else Color(0xFF00BCD4),
            animationSpec = tween<Color>(900).preferNative(),
            label = "nativeBackgroundColor"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ColorComparisonBox("Compose", composeColor)
            ColorComparisonBox("Native", nativeColor)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoButton("Compose 变色") { composePink = !composePink }
            DemoButton("Native 变色") { nativePink = !nativePink }
        }
    }
}

@Composable
private fun ColorComparisonBox(label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(82.dp).background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
private fun NativeSpecMatrixDemo() {
    DemoSection(
        title = "7. Spec 矩阵：Tween / Spring / Snap",
        description = "每行独立对照。Tween 含自定义 cubic；Spring 验证过冲；Snap 含 350ms delay。"
    ) {
        SpecComparisonCase(
            title = "Tween cubic",
            baseSpec = tween(
                durationMillis = 900,
                easing = CubicBezierEasing(0.1f, 0.8f, 0.2f, 1f)
            )
        )
        SpecComparisonCase(
            title = "Spring bouncy",
            baseSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        SpecComparisonCase(
            title = "Snap delay 350ms",
            baseSpec = snap(delayMillis = 350)
        )
    }
}

@Composable
private fun SpecComparisonCase(
    title: String,
    baseSpec: FiniteAnimationSpec<Float>
) {
    var composeEnd by remember { mutableStateOf(false) }
    var nativeEnd by remember { mutableStateOf(false) }
    val composeValue by animateFloatAsState(
        targetValue = if (composeEnd) 220f else 0f,
        animationSpec = baseSpec,
        label = "compose$title"
    )
    val nativeValue by animateFloatAsState(
        targetValue = if (nativeEnd) 220f else 0f,
        animationSpec = baseSpec.preferNative(),
        label = "native$title"
    )
    Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    MovementLane("Compose", composeValue, Color(0xFF1976D2))
    MovementLane("Native", nativeValue, Color(0xFFFF7043))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DemoButton("Compose") { composeEnd = !composeEnd }
        DemoButton("Native") { nativeEnd = !nativeEnd }
    }
}

@Composable
private fun NativeTransformMatrixDemo() {
    DemoSection(
        title = "8. Transform：分项对照",
        description = "拆开验证常用 2D、3D rotation 和 scale + origin，避免复杂矩阵组合干扰判断。"
    ) {
        TransformComparisonCase(
            "2D 组合：translate + scale + rotationZ",
            TransformKind.Combined2D
        )
        TransformComparisonCase("translate + uniform scale", TransformKind.TwoD)
        TransformComparisonCase("rotationZ", TransformKind.RotationZ)
        TransformComparisonCase("rotationX", TransformKind.RotationX)
        TransformComparisonCase("rotationY", TransformKind.RotationY)
        TransformComparisonCase("Origin：uniform scale + transformOrigin", TransformKind.Origin)
    }
}

private enum class TransformKind {
    Combined2D,
    TwoD,
    RotationZ,
    RotationX,
    RotationY,
    Origin
}

@Composable
private fun TransformComparisonCase(title: String, kind: TransformKind) {
    var composeEnd by remember { mutableStateOf(false) }
    var nativeEnd by remember { mutableStateOf(false) }
    Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TransformComparisonCard("Compose", composeEnd, kind, useNative = false)
        TransformComparisonCard("Native", nativeEnd, kind, useNative = true)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DemoButton("Compose") { composeEnd = !composeEnd }
        DemoButton("Native") { nativeEnd = !nativeEnd }
    }
}

@Composable
private fun TransformComparisonCard(
    label: String,
    end: Boolean,
    kind: TransformKind,
    useNative: Boolean
) {
    val transition = updateTransition(end, label = "${label}${kind}Transform")
    val baseSpec = tween<Float>(
        durationMillis = 1000,
        easing = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)
    )
    val spec = if (useNative) baseSpec.preferNative() else baseSpec
    val translationX = if (kind == TransformKind.Combined2D || kind == TransformKind.TwoD) {
        transition.animateFloat({ spec }, label = "translationX") {
            if (it) 30f else -15f
        }.value
    } else 0f
    val translationY = if (kind == TransformKind.Combined2D || kind == TransformKind.TwoD) {
        transition.animateFloat({ spec }, label = "translationY") {
            if (it) 18f else -9f
        }.value
    } else 0f
    val scale = if (
        kind == TransformKind.Combined2D ||
        kind == TransformKind.TwoD ||
        kind == TransformKind.Origin
    ) {
        transition.animateFloat({ spec }, label = "uniformScale") {
            if (it) 1.18f else 0.72f
        }.value
    } else 1f
    val rotationX = if (kind == TransformKind.RotationX) {
        transition.animateFloat({ spec }, label = "rotationX") {
            if (it) 42f else -12f
        }.value
    } else 0f
    val rotationY = if (kind == TransformKind.RotationY) {
        transition.animateFloat({ spec }, label = "rotationY") {
            if (it) -38f else 12f
        }.value
    } else 0f
    val rotationZ = if (kind == TransformKind.Combined2D || kind == TransformKind.RotationZ) {
        transition.animateFloat({ spec }, label = "rotationZ") {
            if (it) 70f else -15f
        }.value
    } else 0f
    val pivotX = if (kind == TransformKind.Origin) {
        transition.animateFloat({ spec }, label = "transformOriginX") {
            if (it) 0.2f else 0.8f
        }.value
    } else 0.5f
    val pivotY = if (kind == TransformKind.Origin) {
        transition.animateFloat({ spec }, label = "transformOriginY") {
            if (it) 0.8f else 0.2f
        }.value
    } else 0.5f

    Column(
        modifier = Modifier.width(150.dp).height(126.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(82.dp)
                .graphicsLayer {
                    this.translationX = translationX
                    this.translationY = translationY
                    scaleX = scale
                    scaleY = scale
                    this.rotationX = rotationX
                    this.rotationY = rotationY
                    this.rotationZ = rotationZ
                    transformOrigin = TransformOrigin(pivotX, pivotY)
                }
                .background(if (useNative) Color(0xFF7E57C2) else Color(0xFF1976D2)),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = Color.White, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(label, fontSize = 12.sp)
    }
}

private enum class SlideEdge { Bottom, Top, Right, Left }

@Composable
private fun NativeSlideDirectionsDemo() {
    DemoSection(
        title = "9. AnimatedVisibility：四方向 Slide",
        description = "方向按钮依次切换 Bottom / Top / Right / Left；每个方向均可快速 Hide/Show。"
    ) {
        var edgeIndex by remember { mutableIntStateOf(0) }
        var composeVisible by remember { mutableStateOf(true) }
        var nativeVisible by remember { mutableStateOf(true) }
        val edge = SlideEdge.entries[edgeIndex % SlideEdge.entries.size]
        Text("当前方向：$edge", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DirectionalVisibility("Compose", composeVisible, edge, useNative = false)
            DirectionalVisibility("Native", nativeVisible, edge, useNative = true)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoButton(if (composeVisible) "Compose Hide" else "Compose Show") {
                composeVisible = !composeVisible
            }
            DemoButton(if (nativeVisible) "Native Hide" else "Native Show") {
                nativeVisible = !nativeVisible
            }
        }
        DemoButton("切换方向") {
            edgeIndex++
        }
    }
}

@Composable
private fun DirectionalVisibility(
    label: String,
    visible: Boolean,
    edge: SlideEdge,
    useNative: Boolean
) {
    val baseFloatSpec = tween<Float>(800)
    val baseOffsetSpec = tween<com.tencent.kuikly.compose.ui.unit.IntOffset>(800)
    val floatSpec = if (useNative) baseFloatSpec.preferNative() else baseFloatSpec
    val offsetSpec = if (useNative) baseOffsetSpec.preferNative() else baseOffsetSpec
    val enterSlide = when (edge) {
        SlideEdge.Bottom -> slideInVertically(offsetSpec) { it / 2 }
        SlideEdge.Top -> slideInVertically(offsetSpec) { -it / 2 }
        SlideEdge.Right -> slideInHorizontally(offsetSpec) { it / 2 }
        SlideEdge.Left -> slideInHorizontally(offsetSpec) { -it / 2 }
    }
    val exitSlide = when (edge) {
        SlideEdge.Bottom -> slideOutVertically(offsetSpec) { it / 2 }
        SlideEdge.Top -> slideOutVertically(offsetSpec) { -it / 2 }
        SlideEdge.Right -> slideOutHorizontally(offsetSpec) { it / 2 }
        SlideEdge.Left -> slideOutHorizontally(offsetSpec) { -it / 2 }
    }
    Column(
        modifier = Modifier.width(150.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 12.sp)
        Box(
            modifier = Modifier.width(150.dp).height(112.dp),
            contentAlignment = Alignment.Center
        ) {
            com.tencent.kuikly.compose.animation.AnimatedVisibility(
                visible = visible,
                enter = fadeIn(floatSpec) + scaleIn(floatSpec, initialScale = 0.75f) + enterSlide,
                exit = fadeOut(floatSpec) + scaleOut(floatSpec, targetScale = 0.75f) + exitSlide
            ) {
                Box(
                    modifier = Modifier
                        .width(126.dp)
                        .height(76.dp)
                        .background(if (useNative) Color(0xFF43A047) else Color(0xFF1E88E5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$label\n$edge", color = Color.White, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun NativeFallbackDemo() {
    DemoSection(
        title = "10. 不支持能力回退：布局宽度",
        description = "这里故意给布局宽度使用 preferNative()。预期整体走 Compose，宽度平滑变化且不跳终值。"
    ) {
        var composeExpanded by remember { mutableStateOf(false) }
        var fallbackExpanded by remember { mutableStateOf(false) }
        val composeWidth by animateFloatAsState(
            targetValue = if (composeExpanded) 130f else 60f,
            animationSpec = tween(900),
            label = "composeLayoutWidth"
        )
        val fallbackWidth by animateFloatAsState(
            targetValue = if (fallbackExpanded) 130f else 60f,
            animationSpec = tween<Float>(900).preferNative(),
            label = "nativeUnsupportedLayoutWidth"
        )
        LayoutWidthBar("Compose", composeWidth, Color(0xFF1976D2))
        LayoutWidthBar("Native opt-in → fallback", fallbackWidth, Color(0xFF8E24AA))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoButton("Compose") { composeExpanded = !composeExpanded }
            DemoButton("Fallback") { fallbackExpanded = !fallbackExpanded }
        }
    }
}

@Composable
private fun LayoutWidthBar(label: String, widthValue: Float, color: Color) {
    Column {
        Text(label, fontSize = 12.sp)
        Box(
            modifier = Modifier.width(widthValue.dp).height(42.dp).background(color),
            contentAlignment = Alignment.Center
        ) {
            Text("${widthValue.toInt()} dp", color = Color.White, fontSize = 11.sp)
        }
    }
}

@Composable
private fun NativeAnimationStressDemo() {
    DemoSection(
        title = "11. 重组压力测试",
        description = "开启负载后，分别运行 1600ms 位移动画，对比是否被重组阻塞。"
    ) {
        var loadEnabled by remember { mutableStateOf(false) }
        var composeTargetRight by remember { mutableStateOf(false) }
        var nativeTargetRight by remember { mutableStateOf(false) }
        var stressTick by remember { mutableIntStateOf(0) }
        LaunchedEffect(loadEnabled) {
            while (loadEnabled) {
                stressTick++
                delay(16)
            }
        }

        var checksum = stressTick
        if (loadEnabled) {
            repeat(180_000) { index ->
                checksum = (checksum * 1_664_525 + 1_013_904_223) xor index
            }
        }
        val composeTranslation by animateFloatAsState(
            targetValue = if (composeTargetRight) 220f else 0f,
            animationSpec = tween(1600),
            label = "composeStressSlide"
        )
        val nativeTranslation by animateFloatAsState(
            targetValue = if (nativeTargetRight) 220f else 0f,
            animationSpec = tween<Float>(1600).preferNative(),
            label = "nativeStressSlide"
        )

        Text(
            text = "负载：${if (loadEnabled) "ON" else "OFF"}，校验值：${checksum and 0xFFFF}",
            fontSize = 12.sp
        )
        MovementLane("Compose", composeTranslation, Color(0xFF1976D2))
        MovementLane("Native", nativeTranslation, Color(0xFFF44336))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { loadEnabled = !loadEnabled }) {
                Text(if (loadEnabled) "关闭负载" else "开启负载")
            }
            Button(onClick = { composeTargetRight = !composeTargetRight }) {
                Text("Compose")
            }
            Button(onClick = { nativeTargetRight = !nativeTargetRight }) {
                Text("Native")
            }
        }
    }
}

@Composable
private fun NativeDialogContentDemo() {
    DemoSection(
        title = "12. Dialog 内容：fade + scale",
        description = "Dialog 容器保持原实现，仅将内容的出现/退出动画切换为 Native。"
    ) {
        var composeVisible by remember { mutableStateOf(false) }
        var nativeVisible by remember { mutableStateOf(false) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoButton("Compose Dialog") { composeVisible = true }
            DemoButton("Native Dialog") { nativeVisible = true }
        }
        AnimatedDialogContent(
            visible = composeVisible,
            useNative = false,
            onDismissRequest = { composeVisible = false }
        )
        AnimatedDialogContent(
            visible = nativeVisible,
            useNative = true,
            onDismissRequest = { nativeVisible = false }
        )
    }
}

@Composable
private fun AnimatedDialogContent(
    visible: Boolean,
    useNative: Boolean,
    onDismissRequest: () -> Unit
) {
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(visible) {
        visibleState.targetState = visible
    }
    if (visible || visibleState.currentState || !visibleState.isIdle) {
        Dialog(onDismissRequest = onDismissRequest) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val baseSpec = tween<Float>(500)
                val spec = if (useNative) baseSpec.preferNative() else baseSpec
                com.tencent.kuikly.compose.animation.AnimatedVisibility(
                    visibleState = visibleState,
                    enter = fadeIn(spec) + scaleIn(spec, initialScale = 0.72f),
                    exit = fadeOut(spec) + scaleOut(spec, targetScale = 0.72f)
                ) {
                    Column(
                        modifier = Modifier
                            .width(260.dp)
                            .background(if (useNative) Color(0xFF2E7D32) else Color(0xFF1565C0))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            if (useNative) "Native Dialog" else "Compose Dialog",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("点击关闭，观察 fade + scale 的退出过程", color = Color.White)
                        DemoButton("关闭") { onDismissRequest() }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun NativePopupContentDemo() {
    DemoSection(
        title = "13. Popup 内容：fade + scale",
        description = "Popup 锚点保持静态，仅将菜单内容的 fade + scale 切换为 Native。"
    ) {
        var composeVisible by remember { mutableStateOf(false) }
        var nativeVisible by remember { mutableStateOf(false) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoButton("Compose Popup") { composeVisible = true }
            DemoButton("Native Popup") { nativeVisible = true }
        }
        AnimatedPopupContent(
            visible = composeVisible,
            useNative = false,
            onDismissRequest = { composeVisible = false }
        )
        AnimatedPopupContent(
            visible = nativeVisible,
            useNative = true,
            onDismissRequest = { nativeVisible = false }
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AnimatedPopupContent(
    visible: Boolean,
    useNative: Boolean,
    onDismissRequest: () -> Unit
) {
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(visible) {
        visibleState.targetState = visible
    }
    if (visible || visibleState.currentState || !visibleState.isIdle) {
        Popup(
            alignment = Alignment.TopEnd,
            offset = IntOffset(-32, 180),
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(
                focusable = true,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                clippingEnabled = true
            )
        ) {
            val baseSpec = tween<Float>(450)
            val spec = if (useNative) baseSpec.preferNative() else baseSpec
            val origin = TransformOrigin(1f, 0f)
            com.tencent.kuikly.compose.animation.AnimatedVisibility(
                visibleState = visibleState,
                enter = fadeIn(spec) +
                    scaleIn(spec, initialScale = 0.7f, transformOrigin = origin),
                exit = fadeOut(spec) +
                    scaleOut(spec, targetScale = 0.7f, transformOrigin = origin)
            ) {
                Column(
                    modifier = Modifier
                        .width(210.dp)
                        .background(if (useNative) Color(0xFF6A1B9A) else Color(0xFF00695C))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        if (useNative) "Native Popup" else "Compose Popup",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text("菜单项 A", color = Color.White)
                    Text("菜单项 B", color = Color.White)
                    DemoButton("关闭") { onDismissRequest() }
                }
            }
        }
    }
}


@Composable
private fun DemoSection(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(description, fontSize = 12.sp, color = Color.DarkGray)
        content()
    }
}

@Composable
private fun LabeledAnimatedBox(label: String, alpha: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer { this.alpha = alpha }
                .background(Color(0xFF03A9F4))
        )
        Text(label, fontSize = 12.sp)
    }
}

@Composable
private fun DemoButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text)
    }
}
