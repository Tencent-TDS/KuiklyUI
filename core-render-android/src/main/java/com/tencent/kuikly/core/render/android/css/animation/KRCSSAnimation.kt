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

package com.tencent.kuikly.core.render.android.css.animation

import android.graphics.Matrix
import android.util.ArrayMap
import android.view.View
import com.tencent.kuikly.core.render.android.IKuiklyRenderContext
import com.tencent.kuikly.core.render.android.const.KRCssConst
import com.tencent.kuikly.core.render.android.css.ktx.frameHeight
import com.tencent.kuikly.core.render.android.css.ktx.frameWidth
import com.tencent.kuikly.core.render.android.css.ktx.removeHRAnimation
import com.tencent.kuikly.core.render.android.css.ktx.toPxF
import com.tencent.kuikly.core.render.android.css.ktx.viewDecorator
import java.lang.ref.WeakReference

/**
 * KTV页面动画模块实现类
 *
 * <p>支持plain和spring两种动画类型
 *
 * @param animation KTV侧传递过来的动画描述
 * @param view 动画应用到的View
 */
class KRCSSAnimation(animation: String, view: View, context: IKuiklyRenderContext?) {

    /**
     * 动画结束回调
     */
    var onAnimationEndBlock: ((hrAnimation: KRCSSAnimation, isCancel: Boolean, propKey: String, animationKey: String) -> Unit)? = null

    /**
     * 正在执行动画的数量。等于0时代表动画结束
     */
    private var animationRunningCount = 0

    /**
     * 动画作用到的View的弱引用
     */
    private val weakView = WeakReference(view)

    /**
     * 动画的时间
     *
     * <p>该字段只在plain动画上生效
     */
    private var duration: Float = 0f

    /**
     * 动画差值器类型
     */
    private var timingFuncType: Int = 0

    /**
     * 动画类型
     */
    private var animationType: Int = 0

    /**
     * spring动画的弹簧系数
     */
    private var damping = 0f

    /**
     * spring动画的开始速率
     */
    private var velocity = 0f

    /**
     * 动画延迟执行时间
     */
    private var delay = 0f

    /**
     * 循环动画
     */
    private var repeatForever = false

    /**
     * 动画key
     */
    private var animationKey = ""

    private val supportAnimationHandlerCreator = ArrayMap<String, () -> KRCSSAnimationHandler>()
    private val animationOperationMap = ArrayMap<String, KRCSSAnimationHandler>()

    /**
     * 动画类型key
     */
    private var propKey: String? = null

    private var animationCommit = false

    private val contextWeakRef = WeakReference(context)

    init {
        parseAnimation(animation)
        setupAnimationHandler()
    }

    /**
     * 判断propKey是否支持动画，
     * 目前支持以下动画类型
     * 1.frame动画[KRCSSSpringFrameAnimationHandler],[KRCSSPlainFrameAnimationHandler]
     * 2.transform动画[KRCSSSpringTransformAnimationHandler],[KRCSSPlainTransformAnimationHandler]
     * 3.backgroundColor动画[KRCSSSpringBackgroundColorAnimationHandler],[KRCSSPlainBackgroundColorAnimationHandler]
     * 4.alpha动画[KRCSSSpringOpacityAnimationHandler],[KRCSSPlainBackgroundColorAnimationHandler]
     * @param propKey 属性key
     * @return 该propKey是否支持动画
     */
    fun supportAnimation(propKey: String): Boolean = supportAnimationHandlerCreator.containsKey(propKey)

    /**
     * 记录待执行的动画，动画执行的时机为[commitAnimation]
     * @param animationType 动画类型，目前支持以下动画类型
     * 1.[KRCssConst.OPACITY]
     * 2.[KRCssConst.TRANSFORM]
     * 3.[KRCssConst.BACKGROUND_COLOR]
     * 4.[KRCssConst.FRAME]
     * @param finalValue 动画最终值
     */
    fun addAnimationOperation(animationType: String, finalValue: Any) {
        val handler = supportAnimationHandlerCreator[animationType]?.invoke() ?: return
        handler.finalValue = finalValue
        propKey = animationType
        animationOperationMap[animationType] = handler
    }

    /**
     * 批量执行先前调用过[addAnimationOperation]方法记录下的动画
     */
    fun commitAnimation() {
        if (animationCommit) {
            return
        }
        animationCommit = true
        val targetView = weakView.get() ?: return
        val values = animationOperationMap.values
        for (value in values) {
            configAnimationHandler(value)
            value.start(targetView) {
                animationRunningCount--
                if (animationRunningCount == 0) {
                    onAnimationEndBlock?.invoke(this, false, propKey ?: KRCssConst.EMPTY_STRING, animationKey)
                }
            }
            animationRunningCount++
        }
    }

    /**
     * 动画是否在播放中
     */
    fun isPlaying(): Boolean {
        return animationRunningCount > 0
    }

    /**
     * 批量取消正在执行的动画
     */
    fun cancelAnimation() {
        val values = animationOperationMap.values
        for (value in values) {
            value.cancel()
        }
        onAnimationEndBlock?.invoke(this, true, propKey ?: KRCssConst.EMPTY_STRING, animationKey)
    }

    fun removeFromAnimationQueue() {
        weakView.get()?.removeHRAnimation(this)
    }

    private fun configAnimationHandler(handler: KRCSSAnimationHandler) {
        handler.durationS = duration
        handler.delay = delay
        handler.repeatForever = repeatForever
        handler.animationKey = animationKey
        if (handler is KRCSSSpringAnimationHandler) {
            handler.damping = damping
            handler.velocity = velocity
        } else if (handler is KRCSSPlainAnimationHandler) {
            handler.durationS = duration
            handler.timingFuncType = timingFuncType
        }
    }

    private fun parseAnimation(animation: String) {
        val animationSpilt = animation.split(KRCssConst.BLANK_SEPARATOR)
        animationType = animationSpilt[ANIMATION_TYPE_INDEX].toInt()
        timingFuncType = animationSpilt[TIMING_FUNC_TYPE_INDEX].toInt()
        duration = animationSpilt[DURATION_INDEX].toFloat()
        damping = animationSpilt[DAMPING_INDEX].toFloat()
        velocity = contextWeakRef.get().toPxF(animationSpilt[VELOCITY_INDEX].toFloat())
        // 兼容旧版本
        if (animationSpilt.size > DELAY_INDEX) {
            delay = animationSpilt[DELAY_INDEX].toFloat()
        }
        if (animationSpilt.size > REPEAT_INDEX) {
            repeatForever = animationSpilt[REPEAT_INDEX].toInt() == 1
        }
        if (animationSpilt.size > ANIMATION_KEY_INDEX) {
            animationKey = animationSpilt[ANIMATION_KEY_INDEX]
        }
    }

    private fun setupAnimationHandler() {
        val isSpring = animationType == SPRING_ANIMATION_TYPE
        supportAnimationHandlerCreator[KRCssConst.OPACITY] = {
            if (isSpring) KRCSSSpringOpacityAnimationHandler() else KRCSSPlainOpacityAnimationHandler()
        }
        supportAnimationHandlerCreator[KRCssConst.TRANSFORM] = {
            if (isSpring) KRCSSSpringTransformAnimationHandler() else KRCSSPlainTransformAnimationHandler()
        }
        supportAnimationHandlerCreator[KRCssConst.BACKGROUND_COLOR] = {
            if (isSpring) KRCSSSpringBackgroundColorAnimationHandler() else KRCSSPlainBackgroundColorAnimationHandler()
        }
        supportAnimationHandlerCreator[KRCssConst.FRAME] = {
            if (isSpring) KRCSSSpringFrameAnimationHandler() else KRCSSPlainFrameAnimationHandler()
        }
    }

    companion object {
        private const val ANIMATION_TYPE_INDEX = 0
        private const val TIMING_FUNC_TYPE_INDEX = 1
        private const val DURATION_INDEX = 2
        private const val DAMPING_INDEX = 3
        private const val VELOCITY_INDEX = 4
        private const val DELAY_INDEX = 5
        private const val REPEAT_INDEX = 6
        private const val ANIMATION_KEY_INDEX = 7

        private const val SPRING_ANIMATION_TYPE = 1
    }
}

/**
 * 动画逻辑处理器基类，目前动画分为两种类型
 * 1.弹性动画，对应的处理器基类：[KRCSSSpringAnimationHandler]
 * 2.属性动画，对应的处理器基类：[KRCSSPlainAnimationHandler]
 */
abstract class KRCSSAnimationHandler {

    var weakTarget: WeakReference<View>? = null
    var durationS: Float = 0f
    var finalValue: Any? = null
    var delay: Float = 0f
    var repeatForever = false
    var animationKey = ""

    /**
     * 启动动画
     * @param target 应用动画的View
     * @param onAnimationEndBlock 动画结束的回调block
     */
    abstract fun start(target: View, onAnimationEndBlock: () -> Unit)

    /**
     * 取消动画
     */
    abstract fun cancel()

}

/**
 * 为View扩展transform属性
 */
internal val View.transform: KRCSSTransform
    get() {
        return KRCSSTransform(null, this)
    }

/**
 * transform动画每一帧的记录类
 * transform动画包含以下属性：
 * 1.[KRCSSTransform.rotate]: 对应[android.view.View.ROTATION]
 * 2.[KRCSSTransform.scaleX]: 对应[android.view.View.SCALE_X]
 * 3.[KRCSSTransform.scaleY]: 对应[android.view.View.SCALE_Y]
 * 4.[KRCSSTransform.translateX]: 对应[android.view.View.TRANSLATION_X]
 * 5.[KRCSSTransform.translateY]: 对应[android.view.View.TRANSLATION_Y]
 * 6.[KRCSSTransform.pivotX]: 对应[android.view.View.getPivotX]
 * 7.[KRCSSTransform.pivotY]: 对应[android.view.View.getPivotY]
 * 8.[KRCSSTransform.rotateX]: 对应[android.view.View.ROTATION_X]
 * 9.[KRCSSTransform.rotateY]: 对应[android.view.View.ROTATION_Y]
 */
class KRCSSTransform(transform: String?, private val target: View) {

    var rotate = DEFAULT_ROTATE
    var rotateX = DEFAULT_ROTATE_X
    var rotateY = DEFAULT_ROTATE_Y

    var scaleX = DEFAULT_SCALE_X
    var scaleY = DEFAULT_SCALE_Y

    var translateX = DEFAULT_TRANSLATE_X
    var translateY = DEFAULT_TRANSLATE_Y

    var pivotX = DEFAULT_PIVOT_X
    var pivotY = DEFAULT_PIVOT_Y

    var skewX: Float? = DEFAULT_SKEW_X
    var skewY: Float? = DEFAULT_SKEW_Y

    init {
        initTransform(transform)
    }

    /**
     * 应用transform到targetView
     */
    fun applyTransform() {
        target.rotation = rotate
        target.rotationX = rotateX
        target.rotationY = rotateY
        target.scaleX = scaleX
        target.scaleY = scaleY
        target.translationX = translateX
        target.translationY = translateY
        target.pivotX = pivotX
        target.pivotY = pivotY
        applySkewTransform()
    }

    /**
     * 重置transform
     */
    fun resetTransform() {
        if (rotate != DEFAULT_ROTATE) {
            target.rotation = DEFAULT_ROTATE
        }
        if (rotateX != DEFAULT_ROTATE_X) {
            target.rotationX = DEFAULT_ROTATE_X
        }
        if (rotateY != DEFAULT_ROTATE_Y) {
            target.rotationY = DEFAULT_ROTATE_Y
        }
        if (scaleX != DEFAULT_SCALE_X) {
            target.scaleX = DEFAULT_SCALE_X
        }
        if (scaleY != DEFAULT_SCALE_Y) {
            target.scaleY = DEFAULT_SCALE_Y
        }
        if (translateX != DEFAULT_TRANSLATE_X) {
            target.translationX = DEFAULT_TRANSLATE_X
        }
        if (translateY != DEFAULT_TRANSLATE_Y) {
            target.translationY = DEFAULT_TRANSLATE_Y
        }
        if (pivotX != DEFAULT_PIVOT_X) {
            target.pivotX = DEFAULT_PIVOT_X
        }
        if (pivotY != DEFAULT_PIVOT_Y) {
            target.pivotY = DEFAULT_PIVOT_Y
        }
        resetSkewTransform()
    }

    private fun initTransform(transform: String?) {
        when (transform) {
            null -> { // 如果transform为null的话，使用View的初始值作为transform内部的值
                initTransformFromTargetView()
            }
            KRCssConst.EMPTY_STRING -> { // 空字符串的话, 使用默认值
                initDefaultTransform()
            }
            else -> { // 从transform字符串中解析
                initTransformFromStr(transform)
            }
        }
    }

    private fun initTransformFromTargetView() {
        rotate = target.rotation
        rotateX = target.rotationX
        rotateY = target.rotationY
        scaleX = target.scaleX
        scaleY = target.scaleY
        translateX = target.translationX
        translateY = target.translationY
        pivotX = target.pivotX
        pivotY = target.pivotY
    }

    private fun initDefaultTransform() {
        rotate = DEFAULT_ROTATE
        rotateX = DEFAULT_ROTATE_X
        rotateY = DEFAULT_ROTATE_Y
        scaleX = DEFAULT_SCALE_X
        scaleY = DEFAULT_SCALE_Y
        translateX = DEFAULT_TRANSLATE_X
        translateY = DEFAULT_TRANSLATE_Y
        pivotX = DEFAULT_PIVOT_X
        pivotY = DEFAULT_PIVOT_Y
        resetSkewTransform()
    }

    private fun initTransformFromStr(transform: String) {
        val splits = transform.split(TRANSFORM_SEPARATOR)

        rotate = splits[ROTATE_INDEX].toFloat()

        val scaleSpilt = splits[SCALE_INDEX].split(KRCssConst.BLANK_SEPARATOR)
        scaleX = scaleSpilt[SCALE_X_INDEX].toFloat()
        scaleY = scaleSpilt[SCALE_Y_INDEX].toFloat()

        val translateSpilt = splits[TRANSLATE_INDEX].split(KRCssConst.BLANK_SEPARATOR)
        translateX = translateSpilt[TRANSLATE_X_INDEX].toFloat() * target.frameWidth
        translateY = translateSpilt[TRANSLATE_Y_INDEX].toFloat() * target.frameHeight

        val anchorSpilt = splits[ANCHOR_INDEX].split(KRCssConst.BLANK_SEPARATOR)
        pivotX = anchorSpilt[ANCHOR_X_INDEX].toFloat() * target.frameWidth
        pivotY = anchorSpilt[ANCHOR_Y_INDEX].toFloat() * target.frameHeight

        if (splits.size > SKEW_INDEX) {
            val skewSplit = splits[SKEW_INDEX].split(KRCssConst.BLANK_SEPARATOR)
            skewX = skewSplit[SKEW_X_INDEX].toFloat()
            skewY = skewSplit[SKEW_Y_INDEX].toFloat()
        }

        if (splits.size > ROTATE_XY_INDEX) {
            val rotateXYSpilt = splits[ROTATE_XY_INDEX].split(KRCssConst.BLANK_SEPARATOR)
            rotateX = rotateXYSpilt[ROTATE_X_INDEX].toFloat()
            rotateY = rotateXYSpilt[ROTATE_Y_INDEX].toFloat()
        }

    }

    private fun applySkewTransform() {
        skewX?.let { sx -> skewY?.let { sy -> /* 在这里执行您需要的操作 */
            val horizontalSkewAngleInRadians = Math.toRadians(sx.toDouble()).toFloat()
            val verticalSkewAngleInRadians = Math.toRadians(sy.toDouble()).toFloat()
            target.viewDecorator?.matrix = Matrix().apply {
                setSkew(horizontalSkewAngleInRadians, verticalSkewAngleInRadians)
            }
        } }
    }

    private fun resetSkewTransform() {
        if (skewX != DEFAULT_SKEW_X || skewY != DEFAULT_SKEW_Y) {
            skewX = DEFAULT_SKEW_X
            skewY = DEFAULT_SKEW_Y
            target.viewDecorator?.matrix = null
        }
    }

    companion object {
        private const val ROTATE_INDEX = 0

        private const val SCALE_INDEX = 1
        private const val SCALE_X_INDEX = 0
        private const val SCALE_Y_INDEX = 1

        private const val TRANSLATE_INDEX = 2
        private const val TRANSLATE_X_INDEX = 0
        private const val TRANSLATE_Y_INDEX = 1

        private const val ANCHOR_INDEX = 3
        private const val ANCHOR_X_INDEX = 0
        private const val ANCHOR_Y_INDEX = 1

        private const val SKEW_INDEX = 4
        private const val SKEW_X_INDEX = 0
        private const val SKEW_Y_INDEX = 1

        private const val ROTATE_XY_INDEX = 5
        private const val ROTATE_X_INDEX = 0
        private const val ROTATE_Y_INDEX = 1
        private const val TRANSFORM_SEPARATOR = "|"

        private const val DEFAULT_ROTATE = 0f
        private const val DEFAULT_ROTATE_X = 0f
        private const val DEFAULT_ROTATE_Y = 0f
        private const val DEFAULT_SCALE_X = 1f
        private const val DEFAULT_SCALE_Y = 1f
        private const val DEFAULT_TRANSLATE_X = 0f
        private const val DEFAULT_TRANSLATE_Y = 0f
        private const val DEFAULT_PIVOT_X = 0f
        private const val DEFAULT_PIVOT_Y = 0f
        private val DEFAULT_SKEW_X = null
        private val DEFAULT_SKEW_Y = null
    }
}
