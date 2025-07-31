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

import androidx.compose.runtime.mutableStateOf
import com.tencent.kuikly.compose.coil3.AsyncImagePainter
import com.tencent.kuikly.compose.ui.geometry.Size
import com.tencent.kuikly.compose.ui.geometry.isSpecified
import com.tencent.kuikly.compose.ui.graphics.painter.BrushPainter
import com.tencent.kuikly.compose.ui.graphics.painter.ColorPainter
import com.tencent.kuikly.compose.ui.graphics.painter.Painter
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.views.ImageView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class KuiklyPainter(
    internal val src: String?,
    internal val placeHolder: Painter? = null,
    private val error: Painter? = null,
    private val fallback: Painter? = null
) : AsyncImagePainter() {

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

    override val intrinsicSize: Size
        get() {
            val self = resolution.value
            if (placeHolder != null && (_state.value is State.Empty || _state.value is State.Loading)) {
                return placeHolder.intrinsicSize
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
                updateState(State.Error(error))
                error?.also(imageView::applyFallback)
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
            if (_state.value.painter != null) {
                imageView.applyFallback(_state.value.painter!!)
            } else {
                imageView.getViewAttr().src(src ?: "")
            }
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
}

private fun ImageView.applyFallback(painter: Painter) {
    when (painter) {
        is KuiklyPainter -> getViewAttr().src(painter.src ?: "")
        is ColorPainter, is BrushPainter -> painter.applyTo(this)
    }
}
