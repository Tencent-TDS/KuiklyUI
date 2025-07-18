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

package com.tencent.kuikly.core.render.android.css.ktx

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Rect
import android.os.Build
import android.util.ArrayMap
import android.util.DisplayMetrics
import android.util.Size
import android.view.View
import android.view.View.AccessibilityDelegate
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.tencent.kuikly.core.render.android.IKuiklyRenderContext
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.const.KRCssConst
import com.tencent.kuikly.core.render.android.css.animation.KRCSSAnimation
import com.tencent.kuikly.core.render.android.css.animation.KRCSSTransform
import com.tencent.kuikly.core.render.android.css.decoration.KRViewDecoration
import com.tencent.kuikly.core.render.android.css.gesture.KRCSSGestureDetector
import com.tencent.kuikly.core.render.android.css.gesture.KRCSSGestureListener
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import org.json.JSONObject
import java.util.Locale
import kotlin.math.min

/**
 * 设置通用的css样式，支持的属性列表可以查看[KRCssConst]定义的属性
 * 这里为啥不用使用map<key, handler>来处理?
 * 1.通用属性不会太多, 使用when语句的可读性比map<key，handler>的方式好
 * 2.setCommonProp方法比较稳定，一般只有维护者一人编写
 * 3.降低内存开销
 * 这里的value类型是与kuiklyCore侧约定好的，因此没判断就使用强转
 * @param key css样式key
 * @param value css样式值
 * @return 是否处理
 *
 * 注意: 如果有新增基础属性的话，需要在[createBaseAtrKeySet]中也加上
 */
@Suppress("UNCHECKED_CAST")
fun View.setCommonProp(key: String, value: Any): Boolean {
    if (tryAddAnimationOperation(key, value)) {
        return true
    }

    return when (key) {
        KRCssConst.OPACITY -> {
            opacity = (value as Number).toFloat()
            true
        }
        KRCssConst.PREVENT_TOUCH -> {
            setPreventTouch(value as Boolean)
            true
        }
        KRCssConst.CONSUME_TOUCH_DOWN -> {
            setTouchDownConsumeOnce(value as Boolean)
            true
        }
        KRCssConst.VISIBILITY -> {
            visibility = if ((value as Int) == 0) View.GONE else View.VISIBLE
            true
        }
        KRCssConst.OVERFLOW -> {
            overflow = (value as Int) == 1
            true
        }
        KRCssConst.BACKGROUND_COLOR -> {
            hrBackgroundColor = (value as String).toColor()
            true
        }
        KRCssConst.TOUCH_ENABLE -> {
            isEnabled = (value as Int) == 1
            true
        }
        KRCssConst.TRANSFORM -> {
            applyTransform(value)
            true
        }
        KRCssConst.BACKGROUND_IMAGE -> {
            backgroundLinearGradient = value as String
            true
        }
        KRCssConst.BOX_SHADOW -> {
            boxShadow = value as String
            true
        }
        KRCssConst.BORDER_RADIUS -> {
            borderRadius = value as String
            true
        }
        KRCssConst.BORDER -> {
            borderStyle = value as String
            true
        }
        KRCssConst.CLICK -> {
            addEventListener(KRCSSGestureListener.TYPE_CLICK, object : KuiklyRenderCallback {
                private val hrClickCallback = value as KuiklyRenderCallback
                override fun invoke(result: Any?) {
                    getViewData<KuiklyRenderCallback>(KRCssConst.PRE_CLICK)?.invoke(result)
                    hrClickCallback.invoke(result)
                }
            })
            true
        }
        KRCssConst.DOUBLE_CLICK -> {
            addEventListener(KRCSSGestureListener.TYPE_DOUBLE_CLICK, value as KuiklyRenderCallback)
            true
        }
        KRCssConst.LONG_PRESS -> {
            addEventListener(KRCSSGestureListener.TYPE_LONG_PRESS, value as KuiklyRenderCallback)
            true
        }
        KRCssConst.ANIMATION -> {
            setHRAnimation(value as String)
            true
        }
        KRCssConst.FRAME -> {
            frame = value as Rect
            hadSetFrame = true
            dispatchOnSetFrame(value)
            true
        }
        KRCssConst.Z_INDEX -> {
            z = value.toNumberFloat()
            true
        }
        KRCssConst.PAN -> {
            addEventListener(KRCSSGestureListener.TYPE_PAN, value as KuiklyRenderCallback)
            true
        }
        KRCssConst.ANIMATION_COMPLETION_BLOCK -> {
            animationCompletionBlock = value as KuiklyRenderCallback
            true
        }
        KRCssConst.ACCESSIBILITY -> {
            val msg = value as String
            contentDescription = msg
            importantForAccessibility = if (msg.isEmpty()) {
                View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            } else {
                View.IMPORTANT_FOR_ACCESSIBILITY_YES
            }
            initAccessibilityDelegate()
            setFocusable(true)
            true
        }
        KRCssConst.DEBUG_NAME -> {
            contentDescription = value as String
            true
        }
        KRCssConst.AUTO_DARK_ENABLE -> {
            // 系统深色主题只有 Android Q 以上支持
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                isForceDarkAllowed = (value as Int) == 1
            }
            true
        }
        KRCssConst.ACCESSIBILITY_ROLE -> {
            setAccessibilityRole(value)
            true
        }
        KRCssConst.USE_OUTLINE -> {
            viewDecorator?.useOutline = value as Boolean
            true
        }
        else -> false
    }
}

fun View.drawCommonDecoration(canvas: Canvas) {
    getViewData<KRViewDecoration>(KRCssConst.VIEW_DECORATOR)?.drawCommonDecoration(frameWidth, frameHeight, canvas)
}

fun View.drawCommonForegroundDecoration(canvas: Canvas) {
    if (isBeforeM) {
        getViewData<KRViewDecoration>(KRCssConst.VIEW_DECORATOR)?.drawCommonForegroundDecoration(frameWidth, frameHeight, canvas)
    }
}

/**
 * 重置[View]对象的css样式。[com.tencent.kuikly.core.render.android.export.IKuiklyRenderViewExport.reusable]为true时，
 * 可以在[com.tencent.kuikly.core.render.android.export.IKuiklyRenderViewExport.resetProp]中调用该方法重置通用的css样式
 *
 * <p>这里为啥不用使用map<key, handler>来处理?
 *
 * <p>1.通用属性不会太多, 使用when语句的可读性比map<key，handler>的方式好
 *
 * <p>2.setCommonProp方法比较稳定，一般只有维护者一人编写
 *
 * <p>3.降低内存开销
 *
 * @param propKey 重置的key
 * @return 是否处理
 */
fun View.resetCommonProp(propKey: String): Boolean {
    when (propKey) {
        KRCssConst.OPACITY -> {
            opacity = 1f
            return true
        }
        KRCssConst.VISIBILITY -> {
            visibility = View.VISIBLE
            return true
        }
        KRCssConst.OVERFLOW -> {
            resetOverflow()
            return true
        }
        KRCssConst.BACKGROUND_COLOR -> {
            resetHRBackground()
            return true
        }
        KRCssConst.TOUCH_ENABLE -> {
            isEnabled = true
            return true
        }
        KRCssConst.TRANSFORM -> {
            resetTransform()
            return true
        }
        KRCssConst.BACKGROUND_IMAGE -> {
            resetHRBackground()
            return true
        }
        KRCssConst.BOX_SHADOW -> {
            resetHRBackground()
            return true
        }
        KRCssConst.BORDER_RADIUS -> {
            resetHRBackground()
            return true
        }
        KRCssConst.BORDER -> {
            resetBorder()
            return true
        }
        KRCssConst.CLICK -> {
            resetEventListener()
            return true
        }
        KRCssConst.DOUBLE_CLICK -> {
            resetEventListener()
            return true
        }
        KRCssConst.LONG_PRESS -> {
            resetEventListener()
            return true
        }
        KRCssConst.PAN -> {
            resetEventListener()
        }
        KRCssConst.ANIMATION -> {
            setHRAnimation(null)
            return true
        }
        KRCssConst.ANIMATION_COMPLETION_BLOCK -> {
            animationCompletionBlock = null
            return true
        }
        KRCssConst.FRAME -> {
            resetFrame()
            return true
        }
        KRCssConst.Z_INDEX -> {
            z = 0f
            return true
        }
        KRCssConst.ACCESSIBILITY -> {
            removeViewData<String>(KRCssConst.HAD_INIT_ACCESSIBILITY_DELEGATE)
            accessibilityDelegate = null
            contentDescription = null
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            return true
        }
        KRCssConst.DEBUG_NAME -> {
            contentDescription = null
            return true
        }
        KRCssConst.USE_OUTLINE -> {
            viewDecorator = null
            return true
        }
        KRCssConst.ACCESSIBILITY_ROLE -> {
            removeViewData<Boolean>(KRCssConst.HAD_INIT_ACCESSIBILITY_DELEGATE)
            removeViewData<String>(KRCssConst.ACCESSIBILITY_ROLE)
            accessibilityDelegate = null
        }
    }
    return false
}

/**
 * 为View扩展opacity属性
 */
private var View.opacity: Float
    get() {
        return alpha
    }
    set(value) {
        alpha = value
    }

internal var touchConsumeByKuikly = false
internal var touchConsumeByNative = false
internal var touchDownConsumeOnce = false

/**
 * 阻止父亲View处理Touch事件
 */
fun View.setPreventTouch(value: Boolean) {
    touchConsumeByKuikly = value
}

fun View.setTouchDownConsumeOnce(value: Boolean) {
    touchDownConsumeOnce = value
}

/**
 * 为View扩展overflow属性
 */
private var View.overflow: Boolean
    get() = getViewData<Boolean>(KRCssConst.OVERFLOW) ?: false
    set(value) {
        if (this !is ViewGroup) {
            return
        }

        val currentOverFlow = overflow
        if (currentOverFlow == value) {
            return
        }

        putViewData(KRCssConst.OVERFLOW, value)
        if (!value) {
            clipBounds = null
            removeOnSetFrameObserver(KRCssConst.OVERFLOW)
        } else {
            val onSetFrameBlock = observeOnSetFrame(KRCssConst.OVERFLOW) {
                if (Rect(0, 0, it.right, it.bottom) != clipBounds) {
                    clipBounds = Rect().apply {
                        left = 0
                        top = 0
                        right = frame.right
                        bottom = frame.bottom
                    }
                }
            }

            if (hadSetFrame) {
                onSetFrameBlock.invoke(frame)
            }
        }
    }

private fun View.resetOverflow() {
    clipBounds = null
    removeViewData<Boolean>(KRCssConst.OVERFLOW)
}

private fun View.resetFrame() {
    removeOnSetFrameObservers()
    frame = Rect()
    hadSetFrame = false
}

/**
 * 通知触发setFrame
 */
fun View.dispatchOnSetFrame(frame: Rect) {
    getViewData<MutableMap<String, OnSetFrameBlock>>(KRCssConst.ON_SET_FRAME_BLOCK_OBSERVERS)?.forEach {
        it.value(frame)
    }
}

/**
 * 监听frame的设置
 */
fun View.observeOnSetFrame(propKey: String, onSetFrameBlock: OnSetFrameBlock): OnSetFrameBlock {
    val map = getViewData<MutableMap<String, OnSetFrameBlock>>(KRCssConst.ON_SET_FRAME_BLOCK_OBSERVERS)
        ?: mutableMapOf<String, OnSetFrameBlock>().apply {
            putViewData(KRCssConst.ON_SET_FRAME_BLOCK_OBSERVERS, this)
        }
    map[propKey] = onSetFrameBlock
    return onSetFrameBlock
}

/**
 * 移除对setFrame的所有监听
 */
fun View.removeOnSetFrameObservers() {
    removeViewData<MutableMap<String, OnSetFrameBlock>>(KRCssConst.ON_SET_FRAME_BLOCK_OBSERVERS)
}

fun View.removeOnSetFrameObserver(propKey: String) {
    val map = getViewData<MutableMap<String, OnSetFrameBlock>>(KRCssConst.ON_SET_FRAME_BLOCK_OBSERVERS)
    map?.also {
        it.remove(propKey)
        if (it.isEmpty()) {
            removeViewData<MutableMap<String, OnSetFrameBlock>>(KRCssConst.ON_SET_FRAME_BLOCK_OBSERVERS)
        }
    }
}

var View.hadSetFrame: Boolean
    get() = getViewData<Boolean>(KRCssConst.HAD_SET_FRAME) ?: false
    set(value) {
        if (value) {
            putViewData(KRCssConst.HAD_SET_FRAME, true)
        } else {
            removeViewData<Boolean>(KRCssConst.HAD_SET_FRAME)
        }
    }

typealias OnSetFrameBlock = (frame: Rect) -> Unit

/**
 * 为View扩展背景颜色
 */
internal var View.hrBackgroundColor: Int?
    get() {
        return viewDecorator?.backgroundColor
    }
    set(value) {
        viewDecorator?.backgroundColor = value ?: 0
    }

/**
 * 为View扩展渐变属性
 */
private var View.backgroundLinearGradient: String?
    get() = viewDecorator?.backgroundImage
    set(value) {
        viewDecorator?.backgroundImage = value ?: KRCssConst.EMPTY_STRING
    }

/**
 * 为View扩展阴影属性
 */
private var View.boxShadow: String?
    get() = viewDecorator?.boxShadow
    set(value) {
        viewDecorator?.boxShadow = value ?: KRCssConst.EMPTY_STRING
    }

/**
 * 为View扩展borderRadius属性
 */
private var View.borderRadius: String?
    get() = viewDecorator?.borderRadius
    set(value) {
        viewDecorator?.borderRadius = value ?: KRCssConst.EMPTY_STRING
        invalidate()
    }

/**
 * 为View扩展borderStyle属性
 */
private var View.borderStyle: String?
    get() = viewDecorator?.borderStyle
    set(value) {
        viewDecorator?.borderStyle = value ?: KRCssConst.EMPTY_STRING
    }

/**
 * 重置View的background
 */
private fun View.resetHRBackground() {
    background = null
    viewDecorator = null
}

private fun View.resetBorder() {
    viewDecorator = null
    if (!isBeforeM) {
        foreground = null
    }
}

internal var View.viewDecorator: KRViewDecoration?
    get() {
        return getViewData(KRCssConst.VIEW_DECORATOR) ?: KRViewDecoration(this).apply {
            putViewData(KRCssConst.VIEW_DECORATOR, this)
        }
    }
    set(value) {
        if (value == null) {
            removeViewData<KRViewDecoration>(KRCssConst.VIEW_DECORATOR)
            if (outlineProvider != null) {
                outlineProvider = null
                clipToOutline = false
            }
        } else {
            putViewData(KRCssConst.VIEW_DECORATOR, value)
        }
    }

/**
 * 添加事件监听
 * @param type 事件类型
 * @param callback 事件回调
 */
@SuppressLint("ClickableViewAccessibility")
fun View.addEventListener(type: Int, callback: KuiklyRenderCallback) {
    val gestureDetector = getViewData(KRCSSGestureDetector.GESTURE_TAG) ?: KRCSSGestureDetector(
        context, this@addEventListener, KRCSSGestureListener(context as? IKuiklyRenderContext)).also {
        putViewData(KRCSSGestureDetector.GESTURE_TAG, it)
        setOnTouchListener { _, event -> it.onTouchEvent(event) }
    }
    gestureDetector.addListener(type, callback)
}

/**
 * 获取事件是否注册监听
 * @param type 事件类型
 * @return true 事件已监听，false 事件未监听
 */
fun View.hasEventListener(type: Int): Boolean {
    val gestureDetector = getViewData<KRCSSGestureDetector>(KRCSSGestureDetector.GESTURE_TAG)
    if (gestureDetector != null) {
        return gestureDetector.hasListener(type)
    }
    return false
}

/**
 * 重置事件监听
 */
fun View.resetEventListener() {
    setOnTouchListener(null)
    removeViewData<Any>(KRCSSGestureDetector.GESTURE_TAG)
}

/**
 * 尝试记录动画操作
 * @param key 属性key
 * @param value 属性值
 * @return 返回true代表key属性是支持动画并且记录一个待执行的动画到[KRCSSAnimation]中
 * 返回false代表key属性不支持动画
 */
private fun View.tryAddAnimationOperation(key: String, value: Any): Boolean {
    val animation = hrAnimation
    if (animation != null && animation.supportAnimation(key)) {
        animation.addAnimationOperation(key, value)
        return true
    }
    return false
}

/**
 * 设置动画描述类
 * @param animation 描述animation的字符串
 */
private fun View.setHRAnimation(animation: String?) {
    when {
        animation == null -> { // View复用时，清除掉动画
            getViewData<ArrayMap<Int, KRCSSAnimation>>(KRCssConst.ANIMATION_QUEUE)?.toMap()?.forEach { (_, v) ->
                v.cancelAnimation()
                v.removeFromAnimationQueue()
            }
        }
        animation.isEmpty() -> {
            hrAnimation = null
            getViewData<ArrayMap<Int, KRCSSAnimation>>(KRCssConst.ANIMATION_QUEUE)?.toMap()?.forEach { (_, v) ->
                // 取消所有正在播放中的动画，启动新的动画
                if (v.isPlaying()) {
                    v.cancelAnimation()
                    v.removeFromAnimationQueue()
                } else {
                    v.commitAnimation()
                }
            }
        }
        else -> {
            val newAnimation = KRCSSAnimation(animation, this, context as? IKuiklyRenderContext)
            newAnimation.onAnimationEndBlock = { hrAnimation: KRCSSAnimation, finished, propKey, animationKey ->
                animationCompletionBlock?.invoke(mapOf(
                    "finish" to if (finished) 1 else 0,
                    "attr" to propKey,
                    "animationKey" to animationKey
                ))
                hrAnimation.removeFromAnimationQueue()
            }
            hrAnimation = newAnimation
            addHRAnimation(newAnimation)
        }
    }
}

/**
 * 为View扩展[KRCSSAnimation]属性
 * 表示当前正在设置的animation
 */
private var View.hrAnimation: KRCSSAnimation?
    get() {
        return getViewData(KRCssConst.ANIMATION)
    }
    set(value) {
        val animation = hrAnimation
        if (value == animation) {
            return
        }

        if (value == null) {
            removeViewData<KRCSSAnimation>(KRCssConst.ANIMATION) ?: return
        } else {
            putViewData(KRCssConst.ANIMATION, value)
        }
    }

/**
 * 将动画添加到动画队列中
 * @param hrAnimation 待添加的动画
 */
private fun View.addHRAnimation(hrAnimation: KRCSSAnimation) {
    val animationQueue = getViewData<ArrayMap<Int, KRCSSAnimation>>(KRCssConst.ANIMATION_QUEUE)
        ?: ArrayMap<Int, KRCSSAnimation>().apply {
            putViewData(KRCssConst.ANIMATION_QUEUE, this)
        }
    animationQueue[hrAnimation.hashCode()] = hrAnimation
}

/**
 * 将动画从动画队列中移除
 * @param hrAnimation 待移除的动画
 */
internal fun View.removeHRAnimation(hrAnimation: KRCSSAnimation) {
    val animationQueue =
        getViewData<ArrayMap<Int, KRCSSAnimation>>(KRCssConst.ANIMATION_QUEUE) ?: return
    animationQueue.remove(hrAnimation.hashCode())
    if (animationQueue.isEmpty()) {
        removeViewData<ArrayMap<Int, KRCSSAnimation>>(KRCssConst.ANIMATION_QUEUE)
    }
}

/**
 * 清除所有正在运行的动画
 */
internal fun View.stopAnimations() {
    getViewData<ArrayMap<Int, KRCSSAnimation>>(KRCssConst.ANIMATION_QUEUE)?.values?.forEach {
        if (it?.isPlaying() == true) {
            it.cancelAnimation()
        }
    }
}

/**
 * 将[String]类型描述的transform应用到View上
 * @param transform transform描述
 */
private fun View.applyTransform(transform: Any) {
    val onSetFrameBlock = observeTransformBlock(transform)
    if (hadSetFrame) {
        onSetFrameBlock.invoke(frame)
    }
}

private fun View.observeTransformBlock(transform: Any): OnSetFrameBlock {
    return observeOnSetFrame(KRCssConst.TRANSFORM) {
        if (transform is String) {
            KRCSSTransform(transform, this).applyTransform()
        } else if (transform is KRCSSTransform) {
            transform.applyTransform()
        }
    }
}

private var View.animationCompletionBlock: KuiklyRenderCallback?
    get() {
        return getViewData(KRCssConst.ANIMATION_COMPLETION_BLOCK)
    }
    set(value) {
        if (value != null) {
            putViewData(KRCssConst.ANIMATION_COMPLETION_BLOCK, value)
        } else {
            removeViewData<KuiklyRenderCallback>(KRCssConst.ANIMATION_COMPLETION_BLOCK)
        }
    }

/**
 * 重置transform
 */
private fun View.resetTransform() {
    KRCSSTransform(null, this).resetTransform()
}

/**
 * 根据key获取View关联的数据
 * @param T key关联的数据类型
 * @param key 关联数据的key
 * @return 被关联的数据
 */
fun <T> View.getViewData(key: String): T? {
    val hippyRenderViewContext = context as? com.tencent.kuikly.core.render.android.IKuiklyRenderContext
        ?: return null
    return hippyRenderViewContext.getViewData<T>(this, key)
}

/**
 * 将key与value关联起来，并保存到View中
 * @param key 关联的key
 * @param value 被关联的数据
 */
fun View.putViewData(key: String, value: Any) {
    val hippyRenderViewContext = context as? com.tencent.kuikly.core.render.android.IKuiklyRenderContext
        ?: return
    hippyRenderViewContext.putViewData(this, key, value)
}

/**
 * 移除key关联的数据
 * @param T 数据类型
 * @param key 关联的key
 * @return 被移除的数据
 */
fun <T> View.removeViewData(key: String): T? {
    val hippyRenderViewContext = context as? com.tencent.kuikly.core.render.android.IKuiklyRenderContext
        ?: return null
    return hippyRenderViewContext.removeViewData(this, key)
}

fun View.clearViewData() {
    (context as? IKuiklyRenderContext)?.clearViewData(this)
}

/**
 * 将字符串安全转换到JSONObject对象
 */
fun String?.toJSONObjectSafely(): JSONObject = JSONObject(this ?: "{}")

private fun View.setAccessibilityRole(propValue: Any) {
    val value = when (propValue as String) {
        "button" -> Button::class.java.name
        "search" -> EditText::class.java.name
        "text" -> TextView::class.java.name
        "image" -> ImageView::class.java.name
        "checkbox" -> CheckBox::class.java.name
        else -> ""
    }
    putViewData(KRCssConst.ACCESSIBILITY_ROLE, value)
    initAccessibilityDelegate()
}

private fun View.initAccessibilityDelegate() {
    if (getViewData<Boolean>(KRCssConst.HAD_INIT_ACCESSIBILITY_DELEGATE) == true) {
        return
    }
    accessibilityDelegate =  object : AccessibilityDelegate() {
        override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
            super.onInitializeAccessibilityNodeInfo(host, info)
            val name = getViewData<String>(KRCssConst.ACCESSIBILITY_ROLE)
            if (name != null) {
                info.className = name
            }
            if (hasEventListener(KRCSSGestureListener.TYPE_CLICK)) {
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
            }
            if (hasEventListener(KRCSSGestureListener.TYPE_LONG_PRESS)) {
                info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_LONG_CLICK)
            }
        }
    }
    putViewData(KRCssConst.HAD_INIT_ACCESSIBILITY_DELEGATE, true)
}

private var innerStatusBarHeight = -1
val Context.statusBarHeight: Int
    @SuppressLint("InternalInsetResource")
    get() {
        if (innerStatusBarHeight == -1) {
            try {
                resources?.getIdentifier("status_bar_height", "dimen", "android")?.also {
                    innerStatusBarHeight = resources.getDimensionPixelSize(it)
                }
            } catch (e: Resources.NotFoundException) {
                KuiklyRenderLog.i("KRCssViewExt", "getStatusBarError: $e")
            }
        }
        return innerStatusBarHeight
    }

private var innerScreenWidth = -1
val Context.screenWidth: Int
    get() {
        if (innerScreenWidth == -1) {
            innerScreenWidth = getDisplaySize(this).width
        }
        return innerScreenWidth
    }

 val Context.activity: Activity?
    get() {
        var context = this
        if (context is Activity) {
            return context
        } else {
            while (context is ContextWrapper) {
                if (context.baseContext is Activity) {
                    return context.baseContext as Activity
                }
                context = context.baseContext
            }
        }
        return null
    }

private var innerScreenHeight = -1
val Context.screenHeight: Int
    get() {
        if (innerScreenHeight == -1) {
            innerScreenHeight = getDisplaySize(this).height
        }
        return innerScreenHeight
    }

internal fun getDisplaySize(context: Context): Size {
    val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val metrics = DisplayMetrics()
    manager.defaultDisplay.getRealMetrics(metrics)
    return Size(metrics.widthPixels, metrics.heightPixels)
}

private var innerVersionName = "-1"
val Context.versionName: String
    get() {
        if (innerVersionName == "-1") {
            try {
                innerVersionName = packageManager.getPackageInfo(packageName, 0).versionName ?: innerVersionName
            } catch (t: Throwable) {
                KuiklyRenderLog.e("KRCSSViewExtension", "versionName error: $t")
            }
        }
        return innerVersionName
    }

val isBeforeM: Boolean
    get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.M

val isBeforeOreoMr1: Boolean
    get() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.O_MR1

internal fun View.setFrameForAndroidM(frame: Rect) {
    var lp = layoutParams
    if (lp is ViewGroup.MarginLayoutParams) {
        lp.leftMargin = frame.left
        lp.topMargin = frame.top
    } else {
        lp = FrameLayout.LayoutParams(0, 0).apply {
            leftMargin = frame.left
            topMargin = frame.top
        }
    }
    lp.width = frame.right
    lp.height = frame.bottom
    layoutParams = lp
}