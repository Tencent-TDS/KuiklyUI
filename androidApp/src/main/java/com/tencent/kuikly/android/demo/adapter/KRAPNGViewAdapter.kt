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

package com.tencent.kuikly.android.demo.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat.AnimationCallback
import com.github.penfeizhou.animation.apng.APNGDrawable
import com.github.penfeizhou.animation.loader.FileLoader
import com.tencent.kuikly.core.render.android.adapter.IAPNGView
import com.tencent.kuikly.core.render.android.adapter.IAPNGViewAnimationListener
import com.tencent.kuikly.core.render.android.adapter.IKRAPNGViewAdapter

// 依赖库 最轻量apng库 ：implementation("com.github.penfeizhou.android.animation:apng:2.25.0")
class KRAPNGViewAdapter : IKRAPNGViewAdapter {

    override fun createAPNGView(context: Context): IAPNGView {
        return KRAPNGImpView(context)
    }
}

class KRAPNGImpView(context: Context) : AppCompatImageView(context), IAPNGView {
    private var apngDrawable : APNGDrawable? = null
    private var currentLoopCount = 0
    override fun setFilePath(filePath: String) {
        apngDrawable = APNGDrawable(FileLoader(filePath))
        apngDrawable?.setAutoPlay(false)
        setImageDrawable(apngDrawable)
    }

    override fun asView(): View {
        return this
    }

    override fun setRepeatCount(count: Int) {
        apngDrawable?.setLoopLimit(count)
    }

    override fun playAnimation() {
        apngDrawable?.start()
    }

    override fun stopAnimation() {
        apngDrawable?.stop()
    }

    override fun addAnimationListener(listener: IAPNGViewAnimationListener) {
        apngDrawable?.registerAnimationCallback(object : AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable?) {
                super.onAnimationEnd(drawable)
                listener.onAnimationEnd(this@KRAPNGImpView)
            }
        })
    }

    override fun removeAnimationListener(listener: IAPNGViewAnimationListener) {
        apngDrawable?.clearAnimationCallbacks()
    }

}