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

package com.tencent.kuikly.compose.ui.scene

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import com.tencent.kuikly.compose.ui.InternalComposeUiApi
import com.tencent.kuikly.compose.ui.graphics.Canvas
import com.tencent.kuikly.compose.ui.input.pointer.PointerInputEvent
import com.tencent.kuikly.compose.ui.node.RootNodeOwner
import com.tencent.kuikly.compose.ui.platform.setContent
import com.tencent.kuikly.compose.ui.unit.Constraints
import com.tencent.kuikly.compose.ui.unit.Density
import com.tencent.kuikly.compose.ui.unit.IntRect
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.unit.LayoutDirection
import com.tencent.kuikly.core.base.DeclarativeBaseView
import kotlin.coroutines.CoroutineContext


@OptIn(InternalComposeUiApi::class)
internal fun KuiklyComposeScene(
    rootKView: DeclarativeBaseView<*, *>,
    density: Density = Density(1f),
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    boundsInWindow: IntRect? = null,
    coroutineContext: CoroutineContext,
    composeSceneContext: ComposeSceneContext = ComposeSceneContext.Empty,
    invalidate: () -> Unit = {},
    ): ComposeScene = KuiklyComposeSceneImpl(
        boundsInWindow = boundsInWindow,
        density = density,
        layoutDirection = layoutDirection,
        coroutineContext = coroutineContext,
        composeSceneContext = composeSceneContext,
        invalidate = invalidate,
        rootKView = rootKView,
    )


@OptIn(InternalComposeUiApi::class)
private class KuiklyComposeSceneImpl @InternalComposeUiApi constructor(
    boundsInWindow: IntRect?,
    density: Density,
    layoutDirection: LayoutDirection,
    coroutineContext: CoroutineContext,
    composeSceneContext: ComposeSceneContext,
    private val invalidate: () -> Unit,
    private val rootKView: DeclarativeBaseView<*, *>,
) : BaseComposeScene(
    coroutineContext = coroutineContext,
    composeSceneContext = composeSceneContext,
    invalidate = invalidate
) {
    private val mainOwner by lazy {
        RootNodeOwner(
            density = density,
            layoutDirection = layoutDirection,
            coroutineContext = compositionContext.effectCoroutineContext,
            size = boundsInWindow?.let { IntSize(it.width, boundsInWindow.height) },
            platformContext = composeSceneContext.platformContext,
            snapshotInvalidationTracker = snapshotInvalidationTracker,
            rootKView = rootKView,
            inputHandler = inputHandler,
            scene = this,
        )
    }

    override var density: Density = density
        set(value) {
            check(!isClosed) { "ComposeScene is closed" }
            field = value
            mainOwner.density = value
        }

    override var layoutDirection: LayoutDirection = layoutDirection
        set(value) {
            check(!isClosed) { "ComposeScene is closed" }
            field = value
            mainOwner.layoutDirection = value
        }

    override var boundsInWindow: IntRect? = boundsInWindow
        set(value) {
            check(!isClosed) { "ComposeScene is closed" }
            check(value == null || (value.size.width >= 0 && value.size.height >= 0)) {
                "Size of ComposeScene cannot be negative"
            }
            field = value
            mainOwner.size = value?.let { IntSize(it.width, it.height) }
        }

    init {
        onOwnerAppended(mainOwner)
    }

    override fun close() {
        check(!isClosed) { "ComposeScene is already closed" }
        onOwnerRemoved(mainOwner)
        mainOwner.dispose()
        super.close()
    }

    override fun calculateContentSize(): IntSize {
        check(!isClosed) { "ComposeScene is closed" }
        return mainOwner.measureInConstraints(Constraints())
    }

    override fun createComposition(content: @Composable () -> Unit): Composition {

        return mainOwner.setContent(
            compositionContext,
            { compositionLocalContext },
            content = content
        )
    }

    override fun processPointerInputEvent(event: PointerInputEvent) =
        mainOwner.onPointerInput(event)

    override fun measureAndLayout() {
        mainOwner.measureAndLayout()
    }

    override fun draw(canvas: Canvas) {
        mainOwner.draw(canvas)
    }

    private fun onOwnerAppended(owner: RootNodeOwner) {
    }

    private fun onOwnerRemoved(owner: RootNodeOwner) {
    }
}

