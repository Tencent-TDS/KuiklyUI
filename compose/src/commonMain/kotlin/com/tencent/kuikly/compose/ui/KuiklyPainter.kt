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

import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.mutableStateOf
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.coil3.AsyncImagePainter
import com.tencent.kuikly.compose.ui.geometry.Size
import com.tencent.kuikly.compose.ui.geometry.isSpecified
import com.tencent.kuikly.compose.ui.graphics.Canvas
import com.tencent.kuikly.compose.ui.graphics.ImageBitmap
import com.tencent.kuikly.compose.ui.graphics.drawscope.DrawScope
import com.tencent.kuikly.compose.ui.graphics.painter.BrushPainter
import com.tencent.kuikly.compose.ui.graphics.painter.ColorPainter
import com.tencent.kuikly.compose.ui.graphics.painter.Painter
import com.tencent.kuikly.compose.ui.unit.toIntSize
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.views.ImageView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class KuiklyPainter(
    private val context: ComposeContainer,
    internal val src: String?,
    internal val placeHolder: Painter? = null,
    private val error: Painter? = null,
    private val fallback: Painter? = null
) : AsyncImagePainter(), RememberObserver {
    private val resolution = mutableStateOf(Size.Unspecified)
    private var success = false
    private val _state: MutableStateFlow<State> = MutableStateFlow(
        if (src.isNullOrEmpty() && fallback !is AsyncImagePainter) {
            State.Error(fallback)
        } else {
            State.Empty
        }
    )
    override val state = _state.asStateFlow()
    internal var onState: ((State) -> Unit)? = null

    private var isActive = false
    private var imageCache: ImageBitmap? = null

    override val intrinsicSize: Size
        get() {
            val self = resolution.value
            (imageCache as? KuiklyImageBitmap)?.also {
                if (it.isReady) {
                    return it.status.let { status ->
                        Size(status.width.toFloat(), status.height.toFloat())
                    }
                }
            }
            error?.intrinsicSize // read to observe changes
            fallback?.intrinsicSize // read to observe changes
            if (placeHolder != null && (_state.value is State.Empty || _state.value is State.Loading)) {
                return placeHolder.intrinsicSize
            }
            if (_state.value is State.Error) {
                return _state.value.painter?.intrinsicSize ?: Size.Unspecified
            }
            return self
        }

    override fun applyTo(view: DeclarativeBaseView<*, *>) {
        val imageView = view as? ImageView ?: return
        imageView.getViewEvent().apply {
            loadResolution {
                resolution.value = Size(it.width.toFloat(), it.height.toFloat())
                if (success) {
                    updateState(State.Success(this@KuiklyPainter))
                }
            }

            loadSuccess {
                if (_state.value !is State.Loading) {
                    return@loadSuccess
                }
                success = true
                if (resolution.value.isSpecified) {
                    updateState(State.Success(this@KuiklyPainter))
                }
            }

            loadFailure {
                if (it.src != this@KuiklyPainter.src) {
                    return@loadFailure
                }
                updateState(State.Error(error))
                error?.applyTo(imageView)
            }
        }

        if (_state.value is State.Empty) {
            if (src.isNullOrEmpty()) {
                updateState(State.Error(fallback))
            } else {
                updateState(State.Loading(placeHolder))
            }
        }
        if (_state.value is State.Error) {
            _state.value.painter?.applyTo(imageView) ?: imageView.getViewAttr().src("")
        } else {
            imageView.getViewAttr().src(src!!)
        }
    }

    private fun updateState(state: State) {
        _state.value = state
        onState?.invoke(state)
    }

    internal fun updateFromReuse(painter: Painter) {
        if (painter is KuiklyPainter && painter.src == this.src && painter._state.value != this._state.value) {
            this.resolution.value = painter.resolution.value
            this.success = painter.success
            when (painter._state.value) {
                is State.Loading -> updateState(State.Loading(placeHolder))
                is State.Success -> updateState(State.Success(this))
                is State.Error -> updateState(State.Error(if (src.isNullOrEmpty()) fallback else error))
                else -> {}
            }
        }
    }

    private fun loadImage(src: String): ImageBitmap {
        return imageCache ?: context.imageCacheManager.loadImage(src).also {
            if (isActive) {
                (it as? RememberObserver)?.onRemembered()
            }
            imageCache = it
        }
    }

    override fun prefetch() {
        val src = this@KuiklyPainter.src ?: return
        loadImage(src)
    }

    override fun restart() {
        this._state.value = State.Empty
    }

    override fun onAbandoned() {
        isActive = false
        (imageCache as? RememberObserver)?.onAbandoned()
    }

    override fun onForgotten() {
        isActive = false
        (imageCache as? RememberObserver)?.onForgotten()
    }

    override fun onRemembered() {
        isActive = true
        (imageCache as? RememberObserver)?.onRemembered()
    }

    override fun DrawScope.onDraw(canvas: Canvas) {
        val src = this@KuiklyPainter.src ?: return
        val cache = loadImage(src)
        drawImage(cache, dstSize = size.toIntSize())
    }
}
