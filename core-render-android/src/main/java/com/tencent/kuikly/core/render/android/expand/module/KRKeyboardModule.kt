/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.kuikly.core.render.android.expand.module

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.PopupWindow
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule

/**
 * 用于获取/监听键盘的相关状态
 */
class KRKeyboardModule : KuiklyRenderBaseModule() {

    companion object {
        const val MODULE_NAME = "KRKeyboardModule"
    }

    private var keyboardStatusWatcher: KeyboardStatusWatcher? = null

    fun addListener(listener: KeyboardStatusListener) {
        if (keyboardStatusWatcher == null) {
            activity?.let {
                keyboardStatusWatcher = KeyboardStatusWatcher(it)
            }
        }
        keyboardStatusWatcher?.addListener(listener)
    }

    fun removeListener(listener: KeyboardStatusListener) {
        keyboardStatusWatcher?.removeListener(listener)
    }

    override fun onDestroy() {
        super.onDestroy()
        keyboardStatusWatcher?.destroy()
    }

}

/**
 * 键盘状态监听，通过往 Activity 添加一个 popupView 监听键盘状态变化
 */
class KeyboardStatusWatcher(private val activity: Activity) : PopupWindow(activity),
    ViewTreeObserver.OnGlobalLayoutListener {

    private val popupView by lazy {
        FrameLayout(activity).also {
            it.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    private var lastVisibleHeight = -1
    private var lastVisibleBottom = -1
    private var listeners = ArrayList<KeyboardStatusListener>()

    init {
        contentView = popupView
        softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        inputMethodMode = INPUT_METHOD_NEEDED

        width = 0
        height = WindowManager.LayoutParams.MATCH_PARENT

        // add popupWindow，监听 globalLayoutListener
        val parentView = activity.findViewById<View>(android.R.id.content)
        parentView?.post {
            popupView.viewTreeObserver.addOnGlobalLayoutListener(this)
            if (!isShowing && parentView.windowToken != null) {
                showAtLocation(parentView, Gravity.NO_GRAVITY, 0, 0)
            }
        }
    }

    override fun onGlobalLayout() {
        val rect = Rect()
        popupView.getWindowVisibleDisplayFrame(rect)
        val visibleHeight = rect.bottom - rect.top
        // 可见区域不变，不重复通知
        if (visibleHeight == lastVisibleHeight) {
            return
        }
        lastVisibleHeight = visibleHeight

        val screenHeight = getScreenHeight()
        // 可视区域占屏幕高度大于 80%，则认为键盘关闭，记录首次关闭时键盘底部的高度
        if (lastVisibleBottom < 0 && visibleHeight > screenHeight * 0.8) {
            lastVisibleBottom = rect.bottom
        }

        val keyboardHeight = if (lastVisibleBottom < 0) {
            // 避免首次回调非关闭状态，使用 decorView.bottom 计算
            val decorViewRect = Rect()
            activity.window.decorView.getWindowVisibleDisplayFrame(decorViewRect)
            decorViewRect.bottom - rect.bottom
        } else {
            lastVisibleBottom - rect.bottom
        }

        // 键盘高度占屏幕高度超过 20%，则认为键盘打开
        if (keyboardHeight > screenHeight * 0.2) {
            notifyKeyboardHeightChanged(keyboardHeight)
        } else {
            notifyKeyboardHeightChanged(0)
        }
    }

    /**
     * 获取屏幕高度，获取失败返回 -1
     * 此方式获取的屏幕高度是不包含底部导航栏和状态栏的
     */
    private fun getScreenHeight(): Int {
        try {
            val windowManager = activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            if (display != null) {
                return display.height
            }
        } catch (e: SecurityException) {
            KuiklyRenderLog.e(KRKeyboardModule.MODULE_NAME, "getScreenHeight: " + e.message)
        }
        return -1
    }

    fun addListener(listener: KeyboardStatusListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: KeyboardStatusListener) {
        listeners.remove(listener)
    }

    private fun notifyKeyboardHeightChanged(height: Int) {
        listeners.forEach {
            it.onHeightChanged(height)
        }
    }

    fun destroy() {
        if (isShowing) {
            // 销毁 popupView，避免内存泄露
            dismiss()
        }
        popupView.viewTreeObserver.removeOnGlobalLayoutListener(this)
        listeners.clear()
    }

}

/**
 * 键盘状态监听相关回调
 */
interface KeyboardStatusListener {
    fun onHeightChanged(height: Int)
}