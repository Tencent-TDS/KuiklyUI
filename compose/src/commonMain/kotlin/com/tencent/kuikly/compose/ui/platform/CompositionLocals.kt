/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.event.OnBackPressedDispatcherOwner
import com.tencent.kuikly.compose.ui.ExperimentalComposeUiApi
import com.tencent.kuikly.compose.ui.focus.FocusManager
import com.tencent.kuikly.compose.ui.input.InputModeManager
import com.tencent.kuikly.compose.ui.node.Owner
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.LayoutDirection
import com.tencent.kuikly.compose.platform.Configuration
import com.tencent.kuikly.core.manager.PagerManager

/**
 * Provides the [Density] to be used to transform between [density-independent pixel
 * units (DP)][com.tencent.kuikly.compose.ui.unit.Dp] and pixel units or
 * [scale-independent pixel units (SP)][com.tencent.kuikly.compose.ui.unit.TextUnit] and
 * pixel units. This is typically used when a
 * [DP][com.tencent.kuikly.compose.ui.unit.Dp] is provided and it must be converted in the body of
 * [Layout] or [DrawModifier].
 */
val LocalDensity =
    staticCompositionLocalOf<Density> {
        noLocalProvidedFor("LocalDensity")
    }

/**
 * The CompositionLocal that can be used to control focus within Compose.
 */
val LocalFocusManager =
    staticCompositionLocalOf<FocusManager> {
        noLocalProvidedFor("LocalFocusManager")
    }

/**
 * The CompositionLocal to provide an instance of InputModeManager which controls the current
 * input mode.
 */
val LocalInputModeManager =
    staticCompositionLocalOf<InputModeManager> {
        noLocalProvidedFor("LocalInputManager")
    }

/**
 * The CompositionLocal to provide the layout direction.
 */
val LocalLayoutDirection =
    staticCompositionLocalOf<LayoutDirection> {
        noLocalProvidedFor("LocalLayoutDirection")
    }

/**
 * The [CompositionLocal] to provide a [SoftwareKeyboardController] that can control the current
 * software keyboard.
 *
 * Will be null if the software keyboard cannot be controlled.
 */
@OptIn(ExperimentalComposeUiApi::class)
val LocalSoftwareKeyboardController = staticCompositionLocalOf<SoftwareKeyboardController?> { null }

/**
 * The CompositionLocal that provides the ViewConfiguration.
 */
val LocalViewConfiguration =
    staticCompositionLocalOf<ViewConfiguration> {
        noLocalProvidedFor("LocalViewConfiguration")
    }

val LocalConfiguration =
    staticCompositionLocalOf<Configuration> {
        noLocalProvidedFor("Configuration")
    }

val LocalActivity =
    staticCompositionLocalOf<ComposeContainer> {
        noLocalProvidedFor("ComposeContainer")
    }

val LocalOnBackPressedDispatcherOwner =
    staticCompositionLocalOf<OnBackPressedDispatcherOwner> {
        noLocalProvidedFor("OnBackPressedDispatcherOwner")
    }

@ExperimentalComposeUiApi
@Composable
internal fun ProvideCommonCompositionLocals(
    owner: Owner,
    content: @Composable () -> Unit,
) {
    val currentActivity = remember {
        PagerManager.getCurrentPager() as ComposeContainer
    }
    CompositionLocalProvider(
        LocalDensity provides owner.density,
        LocalFocusManager provides owner.focusOwner,
        LocalInputModeManager provides owner.inputModeManager,
        LocalLayoutDirection provides owner.layoutDirection,
        LocalSoftwareKeyboardController provides owner.softwareKeyboardController,
        LocalViewConfiguration provides owner.viewConfiguration,
        // Kuikly
        LocalActivity provides currentActivity,
        LocalOnBackPressedDispatcherOwner provides currentActivity,
        content = content,
    )
}

private fun noLocalProvidedFor(name: String): Nothing {
    error("CompositionLocal $name not present")
}
