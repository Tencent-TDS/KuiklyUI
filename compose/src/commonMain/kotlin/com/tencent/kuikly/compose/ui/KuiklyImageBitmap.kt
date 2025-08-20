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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ui.graphics.ImageBitmap
import com.tencent.kuikly.core.module.ImageCacheStatus
import com.tencent.kuikly.core.module.ImageRef

internal class KuiklyImageBitmap(
    val src: String,
    private val onForgotten: KuiklyImageBitmap.() -> Unit
) : ImageBitmap, RememberObserver {

    companion object {
        private val EMPTY = ImageCacheStatus(
            ImageCacheStatus.Complete,
            errorCode = -1,
            errorMsg = "empty",
            cacheKey = ""
        )
    }

    override val width: Int
        get() = if (isReady) status.width else 0

    override val height: Int
        get() = if (isReady) status.height else 0

    var status: ImageCacheStatus by mutableStateOf(EMPTY)

    val isReady: Boolean get() = status.errorCode == 0 && status.state == ImageCacheStatus.Complete

    private var _imageRef: ImageRef? = null

    val imageRef: ImageRef get() {
        if (isReady) {
            if (_imageRef == null) {
                _imageRef = ImageRef(status.cacheKey)
            }
            return _imageRef!!
        } else {
            throw IllegalStateException("ImageBitmap is not ready: ${status.errorMsg}")
        }
    }

    override fun onAbandoned() {
        onForgotten.invoke(this)
    }

    override fun onForgotten() {
        onForgotten.invoke(this)
    }

    override fun onRemembered() {
    }

}
