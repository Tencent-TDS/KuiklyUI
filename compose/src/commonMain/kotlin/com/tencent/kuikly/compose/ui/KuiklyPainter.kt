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
import com.tencent.kuikly.compose.ui.graphics.Brush
import com.tencent.kuikly.compose.ui.graphics.LinearGradient
import com.tencent.kuikly.compose.ui.graphics.SolidColor
import com.tencent.kuikly.compose.ui.graphics.painter.BrushPainter
import com.tencent.kuikly.compose.ui.graphics.painter.ColorPainter
import com.tencent.kuikly.compose.ui.graphics.painter.Painter
import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.views.ImageConst
import com.tencent.kuikly.core.views.ImageView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class KuiklyPainter(
    internal val src: String?,
    private val placeHolder: Painter? = null,
    private val error: Painter? = null,
    private val fallback: Painter? = null
) : AsyncImagePainter() {
    
    private val resolution = mutableStateOf(Size.Unspecified)
    private var success = false
    private val _state: MutableStateFlow<State> = MutableStateFlow(State.Empty)
    override val state = _state.asStateFlow()
    internal var onState: ((State) -> Unit)? = null

    override val intrinsicSize: Size
        get() = resolution.value

    override fun applyTo(view: ImageView) {
        view.getViewEvent().apply {
            loadResolution {
                resolution.value = Size(it.width.toFloat(), it.height.toFloat())
                if (success) {
                    updateState(State.Success(this@KuiklyPainter))
                }
            }
            
            loadSuccess {
                success = true
                if (intrinsicSize.isSpecified) {
                    updateState(State.Success(this@KuiklyPainter))
                }
                placeHolder?.also(view::clearPlaceHolder)
            }
            
            loadFailure {
                updateState(State.Error(error))
                error?.also(view::applyFallback)
            }
        }

        if (_state.value is State.Empty) {
            if (src.isNullOrEmpty()) {
                updateState(State.Error(fallback))
            } else {
                updateState(State.Loading(placeHolder))
            }
        }

        if (_state.value is State.Loading) {
            placeHolder?.also(view::applyPlaceHolder)
        }

        if (_state.value is State.Error) {
            if (_state.value.painter != null) {
                view.applyFallback(_state.value.painter!!)
            } else {
                view.getViewAttr().src(src ?: "")
            }
        } else {
            view.getViewAttr().src(src!!)
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

private fun ImageView.applyPlaceHolder(painter: Painter) {
    when (painter) {
        is KuiklyPainter -> {
            if (!painter.src.isNullOrEmpty()) {
                getViewAttr().placeholderSrc(painter.src)
            }
        }
        is ColorPainter, is BrushPainter -> painter.applyTo(this)
    }
}

private fun ImageView.clearPlaceHolder(painter: Painter) {
    when (painter) {
        is KuiklyPainter -> {
            getViewAttr().setProp(ImageConst.PLACEHOLDER, "")
        }
        is ColorPainter -> {
            getViewAttr().setProp(Attr.StyleConst.BACKGROUND_COLOR, Color.TRANSPARENT)
        }
        is BrushPainter -> {
            getViewAttr().setProp(Attr.StyleConst.BACKGROUND_IMAGE, "")
        }
    }
}

private fun ImageView.applyFallback(painter: Painter) {
    when (painter) {
        is KuiklyPainter -> getViewAttr().src(painter.src ?: "")
        is ColorPainter, is BrushPainter -> painter.applyTo(this)
    }
}

fun DeclarativeBaseView<*, *>.applyBrushToBackground(
    brush: Brush?,
    alpha: Float
): Boolean {
    val viewAttr = this.getViewAttr()
    return when (brush) {
        is LinearGradient -> {
            val newBrush = brush.copy(alpha) as LinearGradient
            viewAttr.backgroundLinearGradient(
                newBrush.direction,
                *newBrush.colorStops.toTypedArray()
            )
            true
        }
        is SolidColor -> {
            viewAttr.backgroundColor(brush.value.copy(alpha).toKuiklyColor())
            true
        }
        else -> false
    }
}

