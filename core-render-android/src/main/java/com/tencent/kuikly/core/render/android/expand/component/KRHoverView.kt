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
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import com.tencent.kuikly.core.render.android.const.KRCssConst
import com.tencent.kuikly.core.render.android.css.ktx.frame
import com.tencent.kuikly.core.render.android.css.ktx.toNumberFloat
import com.tencent.kuikly.core.render.android.css.ktx.toPxF
import com.tencent.kuikly.core.render.android.expand.component.list.IKRRecyclerViewListener
import com.tencent.kuikly.core.render.android.expand.component.list.KRRecyclerContentView
import com.tencent.kuikly.core.render.android.expand.component.list.KRRecyclerView
import java.lang.ref.WeakReference
import kotlin.math.max

/**
 * Created by kam on 2023/2/10.
 */
class KRHoverView(context: Context) : KRView(context), IKRRecyclerViewListener {

    private var krRecyclerViewWeakRef: WeakReference<KRRecyclerView>? = null

    private var cssFrame: Rect? = null
    private var hoverViewMarginTop = 0f
    private var hoverViewSortList = mutableListOf<KRHoverView>()
    var bringIndex = 0

    override val reusable: Boolean
        get() = false

    override fun setProp(propKey: String, propValue: Any): Boolean {
        val handle = when (propKey) {
            MARGIN_TOP -> hoverMarginTop(propValue)
            BRING_INDEX -> bringIndex(propValue)
            else -> super.setProp(propKey, propValue)
        }
        if (propKey == KRCssConst.FRAME) {
            cssFrame = propValue as Rect
            updateFrameToHoverIfNeed()
        }
        return handle
    }

    override fun onAddToParent(parent: ViewGroup) {
        super.onAddToParent(parent)
        val krRecyclerView = parent.parent as? KRRecyclerView
        val setupTask = { hrRV: KRRecyclerView? ->
            setHRRecyclerView(hrRV)
            updateFrameToHoverIfNeed()
        }
        if (krRecyclerView == null) {
            parent.addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    parent.removeOnAttachStateChangeListener(this)
                    setupTask.invoke(parent.parent as? KRRecyclerView)
                }

                override fun onViewDetachedFromWindow(v: View) {
                }
            })
        } else {
            setupTask.invoke(krRecyclerView)
        }
    }

    override fun onRemoveFromParent(parent: ViewGroup) {
        super.onRemoveFromParent(parent)
        setHRRecyclerView(null)
    }

    override fun onContentViewAddChild(contentView: KRRecyclerContentView, contentViewChild: View) {
        super.onContentViewAddChild(contentView, contentViewChild)
        updateFrameToHoverIfNeed()
    }

    override fun onScroll(offsetX: Float, offsetY: Float) {
        updateFrameToHoverIfNeed()
    }

    override fun onEndDrag(offsetX: Float, offsetY: Float) {
        super.onEndDrag(offsetX, offsetY)
        updateFrameToHoverIfNeed()
    }

    private fun hoverMarginTop(propValue: Any): Boolean {
        hoverViewMarginTop = kuiklyRenderContext.toPxF(propValue.toNumberFloat())
        return true
    }

    private fun setHRRecyclerView(krRecyclerView: KRRecyclerView?) {
        krRecyclerViewWeakRef = if (krRecyclerView == null) {
            krRecyclerViewWeakRef?.get()?.removeListener(this)
            null
        } else {
            krRecyclerViewWeakRef?.get()?.removeListener(this)
            krRecyclerView.addScrollListener(this)
            WeakReference(krRecyclerView)
        }
    }

    private fun updateFrameToHoverIfNeed() {
        val hrRV = krRecyclerViewWeakRef?.get() ?: return
        val viewFrame = cssFrame ?: return

        val offsetY = hrRV.contentOffsetY
        if (offsetY > viewFrame.top - hoverViewMarginTop) {
            (layoutParams as? MarginLayoutParams)?.apply {
                topMargin = offsetY.toInt() + hoverViewMarginTop.toInt()
            }?.also {
                layoutParams = it
            }
        } else {
            frame = viewFrame
        }
        adjustHoverViewLayerIfNeed()
    }

    private fun adjustHoverViewLayerIfNeed() {
        val parent = parent as? ViewGroup ?: return
        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val view = parent.getChildAt(i)
            if (view is KRHoverView) {
                hoverViewSortList.add(view)
            }
        }

        hoverViewSortList.sortWith(Comparator { first, second ->
            val firstIndex = max(first.bringIndex, first.z.toInt())
            val secondIndex = max(second.bringIndex, second.z.toInt())
            firstIndex - secondIndex
        })

        for (hoverView in hoverViewSortList) {
            parent.bringChildToFront(hoverView)
        }
        hoverViewSortList.clear()
    }

    private fun bringIndex(propValue: Any): Boolean {
        bringIndex = propValue as Int
        return true
    }

    companion object {
        const val VIEW_NAME = "KRHoverView"
        private const val BRING_INDEX = "bringIndex"
        private const val MARGIN_TOP = "hoverMarginTop"
    }
}