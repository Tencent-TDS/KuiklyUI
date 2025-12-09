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

package com.tencent.kuikly.compose_dsl.kuikly.text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.BoxScope
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose_dsl.kuikly.extension.nativeRef
import com.tencent.kuikly.compose_dsl.kuikly.extension.setEvent
import com.tencent.kuikly.compose_dsl.kuikly.extension.setProp
import com.tencent.kuikly.core.layout.Frame
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.views.DivView
import com.tencent.kuikly.core.views.SelectableOption
import com.tencent.kuikly.core.views.SelectionType

/**
 * Selection type for text selection
 */
enum class TextSelectionType {
    CHARACTER,
    WORD,
    PARAGRAPH,
    SENTENCE
}

/**
 * Frame data for selection events
 */
data class SelectionFrame(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) {
    companion object {
        val Zero = SelectionFrame(0f, 0f, 0f, 0f)
        
        internal fun fromJson(json: Any?): SelectionFrame {
            val jsonObj = json as? JSONObject ?: return Zero
            return SelectionFrame(
                x = jsonObj.optDouble("x").toFloat(),
                y = jsonObj.optDouble("y").toFloat(),
                width = jsonObj.optDouble("width").toFloat(),
                height = jsonObj.optDouble("height").toFloat()
            )
        }
    }
}

/**
 * CompositionLocal to track whether selection is enabled in the current context.
 * Used by DisableSelection to override the parent's selection setting.
 */
val LocalSelectionEnabled = compositionLocalOf { true }

/**
 * A container that enables text selection for its children.
 * Similar to Jetpack Compose's SelectionContainer, this allows users to select
 * and interact with text content within the container.
 *
 * Example usage:
 * ```kotlin
 * SelectionContainer {
 *     Text("This text is selectable")
 * }
 * ```
 *
 * @param modifier Modifier to be applied to the container
 * @param selectionColor The color used for text selection highlight (optional)
 * @param onSelectStart Callback when selection starts, provides selection frame
 * @param onSelectChange Callback when selection changes, provides selection frame
 * @param onSelectEnd Callback when selection ends, provides selection frame
 * @param onSelectCancel Callback when selection is cancelled
 * @param content The content to be wrapped with selection capability
 */
@Composable
fun SelectionContainer(
    modifier: Modifier = Modifier,
    selectionColor: Color? = null,
    onSelectStart: ((SelectionFrame) -> Unit)? = null,
    onSelectChange: ((SelectionFrame) -> Unit)? = null,
    onSelectEnd: ((SelectionFrame) -> Unit)? = null,
    onSelectCancel: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val selectionEnabled = LocalSelectionEnabled.current
    
    // Build modifier with selection properties
    var selectionModifier = modifier
        .setProp("selectable", if (selectionEnabled) SelectableOption.ENABLE.value else SelectableOption.DISABLE.value)
    
    // Apply selection color if provided
    if (selectionColor != null && selectionEnabled) {
        val colorHex = selectionColor.toArgb()
        val colorJson = JSONObject().apply {
            put("background", 0x66000000L or (colorHex.toLong() and 0x00FFFFFF))
            put("cursor", 0xFF000000L or (colorHex.toLong() and 0x00FFFFFF))
        }.toString()
        selectionModifier = selectionModifier.setProp("selectionColor", colorJson)
    }
    
    // Apply event handlers
    if (onSelectStart != null && selectionEnabled) {
        selectionModifier = selectionModifier.setEvent("selectStart") {
            onSelectStart(SelectionFrame.fromJson(it))
        }
    }
    if (onSelectChange != null && selectionEnabled) {
        selectionModifier = selectionModifier.setEvent("selectChange") {
            onSelectChange(SelectionFrame.fromJson(it))
        }
    }
    if (onSelectEnd != null && selectionEnabled) {
        selectionModifier = selectionModifier.setEvent("selectEnd") {
            onSelectEnd(SelectionFrame.fromJson(it))
        }
    }
    if (onSelectCancel != null && selectionEnabled) {
        selectionModifier = selectionModifier.setEvent("selectCancel") {
            onSelectCancel()
        }
    }
    
    CompositionLocalProvider(LocalSelectionEnabled provides selectionEnabled) {
        Box(
            modifier = selectionModifier,
            content = content
        )
    }
}

/**
 * A container that enables text selection with state management for programmatic control.
 *
 * Example usage:
 * ```kotlin
 * val selectionState = rememberSelectionContainerState()
 * 
 * SelectionContainer(state = selectionState) {
 *     Text("This text is selectable")
 * }
 * 
 * // Programmatically create selection
 * Button(onClick = { selectionState.createSelection(100f, 50f, TextSelectionType.WORD) }) {
 *     Text("Select Word")
 * }
 * ```
 *
 * @param state The state object for controlling selection programmatically
 * @param modifier Modifier to be applied to the container
 * @param selectionColor The color used for text selection highlight (optional)
 * @param onSelectStart Callback when selection starts, provides selection frame
 * @param onSelectChange Callback when selection changes, provides selection frame
 * @param onSelectEnd Callback when selection ends, provides selection frame
 * @param onSelectCancel Callback when selection is cancelled
 * @param content The content to be wrapped with selection capability
 */
@Composable
fun SelectionContainer(
    state: SelectionContainerState,
    modifier: Modifier = Modifier,
    selectionColor: Color? = null,
    onSelectStart: ((SelectionFrame) -> Unit)? = null,
    onSelectChange: ((SelectionFrame) -> Unit)? = null,
    onSelectEnd: ((SelectionFrame) -> Unit)? = null,
    onSelectCancel: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    SelectionContainer(
        modifier = modifier.nativeRef { viewRef ->
            state.bindView(this)
        },
        selectionColor = selectionColor,
        onSelectStart = { frame ->
            state.updateFrame(frame)
            onSelectStart?.invoke(frame)
        },
        onSelectChange = { frame ->
            state.updateFrame(frame)
            onSelectChange?.invoke(frame)
        },
        onSelectEnd = { frame ->
            state.updateFrame(frame)
            onSelectEnd?.invoke(frame)
        },
        onSelectCancel = {
            state.updateFrame(SelectionFrame.Zero)
            onSelectCancel?.invoke()
        },
        content = content
    )
}

/**
 * Disables text selection for its children, even when inside a SelectionContainer.
 *
 * Example usage:
 * ```kotlin
 * SelectionContainer {
 *     Column {
 *         Text("This text is selectable")
 *         DisableSelection {
 *             Text("This text is NOT selectable")
 *         }
 *         Text("This text is also selectable")
 *     }
 * }
 * ```
 *
 * @param modifier Modifier to be applied to the container
 * @param content The content to have selection disabled
 */
@Composable
fun DisableSelection(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    CompositionLocalProvider(LocalSelectionEnabled provides false) {
        Box(
            modifier = modifier.setProp("selectable", SelectableOption.DISABLE.value),
            content = content
        )
    }
}

/**
 * State holder for SelectionContainer that enables programmatic control of text selection.
 */
@Stable
class SelectionContainerState internal constructor() {
    private var boundView: DivView? = null
    
    /**
     * The currently selected texts as a list of strings.
     */
    var selectedTexts by mutableStateOf<List<String>>(emptyList())
        private set
    
    /**
     * The current selection frame.
     */
    var selectionFrame by mutableStateOf(SelectionFrame.Zero)
        private set
    
    internal fun bindView(view: Any?) {
        boundView = view as? DivView
    }
    
    /**
     * Creates a text selection at the specified point.
     *
     * @param x The x coordinate relative to the container
     * @param y The y coordinate relative to the container
     * @param type The type of selection (CHARACTER, WORD, PARAGRAPH, SENTENCE)
     */
    fun createSelection(x: Float, y: Float, type: TextSelectionType = TextSelectionType.WORD) {
        val view = boundView ?: return
        val selectionType = when (type) {
            TextSelectionType.CHARACTER -> SelectionType.CHARACTER
            TextSelectionType.WORD -> SelectionType.WORD
            TextSelectionType.PARAGRAPH -> SelectionType.PARAGRAPH
            TextSelectionType.SENTENCE -> SelectionType.SENTENCE
        }
        view.createSelection(x, y, selectionType)
    }
    
    /**
     * Gets the currently selected text content.
     *
     * @param callback Callback with the list of selected text strings
     */
    fun getSelection(callback: (List<String>) -> Unit) {
        val view = boundView
        if (view != null) {
            view.getSelection { texts ->
                selectedTexts = texts
                callback(texts)
            }
        } else {
            callback(emptyList())
        }
    }
    
    /**
     * Clears the current text selection.
     */
    fun clearSelection() {
        boundView?.clearSelection()
        selectedTexts = emptyList()
        selectionFrame = SelectionFrame.Zero
    }
    
    internal fun updateFrame(frame: SelectionFrame) {
        selectionFrame = frame
    }
}

/**
 * Creates and remembers a SelectionContainerState.
 */
@Composable
fun rememberSelectionContainerState(): SelectionContainerState {
    return remember { SelectionContainerState() }
}

// Extension to convert Color to ARGB int
private fun Color.toArgb(): Int {
    return ((alpha * 255).toInt() shl 24) or
           ((red * 255).toInt() shl 16) or
           ((green * 255).toInt() shl 8) or
           (blue * 255).toInt()
}

