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

package com.tencent.kuikly.compose.animation

import com.tencent.kuikly.compose.animation.core.FiniteAnimationSpec
import com.tencent.kuikly.compose.animation.core.Transition
import com.tencent.kuikly.compose.animation.core.nativePreferredOriginalOrNull
import com.tencent.kuikly.compose.animation.core.preferNative
import com.tencent.kuikly.compose.animation.core.retargetNativeCurve
import com.tencent.kuikly.compose.animation.core.spring
import com.tencent.kuikly.compose.ui.graphics.TransformOrigin
import com.tencent.kuikly.compose.ui.unit.IntOffset

internal fun Transition.Segment<EnterExitState>.nativeTransformOriginSpec(
    enter: EnterTransition,
    exit: ExitTransition
): FiniteAnimationSpec<TransformOrigin> = when {
    EnterExitState.PreEnter isTransitioningTo EnterExitState.Visible ->
        enter.data.scale?.animationSpec
            ?.retargetNativeCurve<Float, TransformOrigin>()
            ?: spring()

    EnterExitState.Visible isTransitioningTo EnterExitState.PostExit ->
        exit.data.scale?.animationSpec
            ?.retargetNativeCurve<Float, TransformOrigin>()
            ?: spring()

    else -> {
        val directionalSpec =
            if (targetState == EnterExitState.Visible) {
                enter.data.scale?.animationSpec
            } else {
                exit.data.scale?.animationSpec
            }
        directionalSpec
            ?.retargetNativeCurve<Float, TransformOrigin>()
            ?: spring()
    }
}

internal fun FiniteAnimationSpec<IntOffset>.usesNativeGraphicsTranslation(): Boolean =
    nativePreferredOriginalOrNull() != null

/**
 * Compose normally switches a reversing Enter/Exit animation to an internal spring. Native Render
 * already continues from the platform presentation value, so use the explicitly opted-in curve
 * for the new direction. This also keeps scale and slide on one descriptor for their shared
 * transform property.
 */
internal fun <T> Transition.Segment<EnterExitState>.nativeInterruptionSpec(
    enterSpec: FiniteAnimationSpec<T>?,
    exitSpec: FiniteAnimationSpec<T>?,
    interruptionSpec: FiniteAnimationSpec<T>
): FiniteAnimationSpec<T> {
    val directionalSpec =
        if (targetState == EnterExitState.Visible) enterSpec else exitSpec
    return if (directionalSpec?.nativePreferredOriginalOrNull() != null) {
        directionalSpec
    } else {
        interruptionSpec
    }
}

/**
 * Keep an explicitly native enter transition available after it has settled.
 *
 * Compose normally clears this metadata at the stable Visible state. A quick Exit -> Enter
 * reversal can then observe the default interruption spec for alpha/scale before the enter
 * metadata is restored, while slide (resolved later during measurement) already sees the native
 * spec. Retaining only the fully supported native subset keeps the reverse group on one clock.
 */
internal fun EnterTransition.retainForNativeInterruptionOrNone(): EnterTransition {
    val hasVisualEffect =
        data.fade != null || data.scale != null || data.slide != null
    val allVisualEffectsAreNative =
        (data.fade?.animationSpec?.nativePreferredOriginalOrNull() != null || data.fade == null) &&
            (data.scale?.animationSpec?.nativePreferredOriginalOrNull() != null ||
                data.scale == null) &&
            (data.slide?.animationSpec?.nativePreferredOriginalOrNull() != null ||
                data.slide == null)
    return if (
        hasVisualEffect &&
        data.changeSize == null &&
        allVisualEffectsAreNative
    ) {
        this
    } else {
        EnterTransition.None
    }
}

/**
 * NavHost owns the page-transition policy, so callers should not need to opt each spec in
 * manually. Pure slide and slide + fade transforms are promoted together. Scale and layout
 * effects are returned unchanged and therefore keep the Compose clock as one logical group.
 */
internal fun ContentTransform.preferNativeNavHostSlide(): ContentTransform {
    val enterData = targetContentEnter.data
    val exitData = initialContentExit.data
    val hasSlide = enterData.slide != null || exitData.slide != null
    if (
        !hasSlide ||
        !enterData.hasOnlySlideAndFadeOrNoEffect() ||
        !exitData.hasOnlySlideAndFadeOrNoEffect()
    ) {
        return this
    }

    var nativeEnter = targetContentEnter
    enterData.fade?.let { fade ->
        nativeEnter += fadeIn(
            animationSpec = fade.animationSpec.preferNative(),
            initialAlpha = fade.alpha
        )
    }
    enterData.slide?.let { slide ->
        nativeEnter += slideIn(
            animationSpec = slide.animationSpec.preferNative(),
            initialOffset = slide.slideOffset
        )
    }

    var nativeExit = initialContentExit
    exitData.fade?.let { fade ->
        nativeExit += fadeOut(
            animationSpec = fade.animationSpec.preferNative(),
            targetAlpha = fade.alpha
        )
    }
    exitData.slide?.let { slide ->
        nativeExit += slideOut(
            animationSpec = slide.animationSpec.preferNative(),
            targetOffset = slide.slideOffset
        )
    }

    return ContentTransform(
        targetContentEnter = nativeEnter,
        initialContentExit = nativeExit,
        targetContentZIndex = targetContentZIndex,
        sizeTransform = null
    ).also {
        it.initialTargetContentZIndex = initialTargetContentZIndex
    }
}

/**
 * Keep navigation destinations in back-stack order while a slide transition is running.
 *
 * AnimatedContent otherwise places the target content above content with the same z-index. That
 * is correct for push, but during pop it lets the previous page immediately cover most of the
 * outgoing page. A stable back-stack z-index keeps the newer page above the older page for both
 * directions and is independent of whether the slide uses the Compose or Native clock.
 */
internal fun ContentTransform.applyNavHostSlideZIndex(
    targetZIndex: Float,
    initialTargetZIndex: Float = targetZIndex
): ContentTransform {
    if (targetContentEnter.data.slide != null || initialContentExit.data.slide != null) {
        targetContentZIndex = targetZIndex
        initialTargetContentZIndex = initialTargetZIndex
    }
    return this
}

private fun TransitionData.hasOnlySlideAndFadeOrNoEffect(): Boolean =
    scale == null &&
        changeSize == null &&
        effectsMap.isEmpty()
