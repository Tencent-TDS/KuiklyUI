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

package com.tencent.kuikly.core.render.android.expand.component

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.css.ktx.removeFromParent

class KRModalView(context: Context) : KRView(context) {
    private var didMoveToWindow = false
    override val reusable: Boolean
        get() = false

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return super.setProp(propKey, propValue)
    }

    override fun onAddToParent(parent: ViewGroup) {
        super.onAddToParent(parent)
        // 移动到全屏Dialog，对标iOS Window
        if (!didMoveToWindow) {
            didMoveToWindow = true
            parent.removeView(this)
            activity?.addContentView(this, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
            if (activity == null) {
                KuiklyRenderLog.e(VIEW_NAME, "activity is null")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFromParent()
    }

    companion object {
        const val VIEW_NAME = "KRModalView"
        const val CONTAINER_SIZE_CHANGED = "containerSizeChanged"
    }
}
