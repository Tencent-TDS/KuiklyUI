package com.tencent.kuikly.android.demo

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.tencent.kuikly.core.render.android.expand.component.KRView

class MyModalView(context: Context) : KRView(context) {

    private var didMoveToWindow = false
    private var dialog: Dialog? = null

    override val reusable: Boolean
        get() = false

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return super.setProp(propKey, propValue)
    }

    override fun onAddToParent(parent: ViewGroup) {
        super.onAddToParent(parent)
        if (!didMoveToWindow) {
            didMoveToWindow = true
            parent.removeView(this)
            val activity = findActivity()
            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                Log.e("MyModalView", "activity is null or finishing, skip Dialog creation")
                return
            }
            setupDialog(activity)
        }
    }

    private fun setupDialog(activity: Activity) {
        dialog = Dialog(activity, R.style.MyDialogFullScreen).apply {
            setContentView(this@MyModalView)
            setCancelable(false)
            setCanceledOnTouchOutside(false)

            window?.let { win ->
                // 关键1：清除默认的 INSET_DECOR flag（这是导致 Dialog 避开状态栏的原因）
                win.clearFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or
                            WindowManager.LayoutParams.FLAG_DIM_BEHIND
                )

                // 关键2：设置 LAYOUT_NO_LIMITS 让 Window 突破所有限制
                win.addFlags(
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                            WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                )

                // 关键3：沉浸式 - 让内容延伸到状态栏和导航栏下
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    win.setDecorFitsSystemWindows(false)
                } else {
                    @Suppress("DEPRECATION")
                    win.decorView.systemUiVisibility = (
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            )
                }

                // 状态栏和导航栏透明
                win.statusBarColor = 0
                win.navigationBarColor = 0

                // 清除 DecorView 默认 padding
                win.decorView.setPadding(0, 0, 0, 0)
                win.setBackgroundDrawableResource(android.R.color.transparent)

                // 设置 LayoutParams
                val params = win.attributes.apply {
                    width = WindowManager.LayoutParams.MATCH_PARENT
                    height = WindowManager.LayoutParams.MATCH_PARENT
                    horizontalMargin = 0f
                    verticalMargin = 0f
                    gravity = android.view.Gravity.FILL
                    // 关键：允许 Dialog 延伸到刘海屏（cutout）区域，解决华为等设备高度被限制的问题
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    }
                }
                win.attributes = params
            }

            if (!isShowing) {
                show()
            }
        }
    }

    override fun onDestroy() {
        dialog?.dismiss()
        dialog = null
        super.onDestroy()
    }

    private fun findActivity(): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return context as? Activity
    }

    companion object {
        const val VIEW_NAME = "KRModalView"
    }
}
