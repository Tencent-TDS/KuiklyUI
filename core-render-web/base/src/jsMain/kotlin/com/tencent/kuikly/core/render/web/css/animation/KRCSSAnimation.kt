package com.tencent.kuikly.core.render.web.css.animation

import com.tencent.kuikly.core.render.web.ktx.Frame
import com.tencent.kuikly.core.render.web.ktx.KRCssConst
import com.tencent.kuikly.core.render.web.ktx.getViewData
import com.tencent.kuikly.core.render.web.ktx.kuiklyAnimation
import com.tencent.kuikly.core.render.web.ktx.removeViewData
import com.tencent.kuikly.core.render.web.processor.AnimationOption
import com.tencent.kuikly.core.render.web.processor.AnimationTimingFunction
import com.tencent.kuikly.core.render.web.processor.KuiklyProcessor
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement

/**
 * Implementation class for Kuikly page animation module, supporting both plain and spring animation types
 *
 * @param animation Animation description passed from Kuikly
 * @param ele Element to which the animation is applied
 */
class KRCSSAnimation(animation: String, ele: Element) {
    // Reference to the View the animation is applied to, weakRef not recommended in JS, so direct reference
    private val view: HTMLElement = ele.unsafeCast<HTMLElement>()

    // Animation end callback
    var onAnimationEndBlock: ((
        hrAnimation: KRCSSAnimation,
        isCancel: Boolean,
        propKey: String,
        animationKey: String
    ) -> Unit)? = null

    // Animation duration, this field only works for plain animations
    var duration: Float = 0f

    // Animation interpolator type
    private var timingFuncType: Int = 0

    // Animation type
    private var animationType: Int = 0

    // Spring coefficient for spring animation
    private var damping = 0f

    // Initial velocity for spring animation
    private var velocity = 0f

    // Whether it is a spring animation
    private var isSpring = false

    // Animation delay time
    var delay = 0f

    // Loop animation
    private var repeatForever = false

    // Animation key
    private var animationKey = ""

    // Supported animation types
    private val supportAnimationHandlerCreator = HashMap<String, () -> KRCSSAnimationHandler>()

    // Animation operation map
    private val animationOperationMap = LinkedHashMap<String, KRCSSAnimationHandler>()

    // Animation type key
    private var propKey: String? = null

    // Start animation
    private var animationCommit = false

    // Current class handler animation count
    private var animationHandlerCount = 0

    init {
        // First format animation parameters
        parseAnimation(animation)
        // create animation instance
        createAnimationInstance()
        // Then according to the animation type, initialize different types of animation property handlers
        setupAnimationHandler()
    }

    /**
     * Determine if propKey supports animation,
     * Currently supports the following animation types
     *
     * 1.frame size and position animation
     * 2.transform transformation animation
     * 3.backgroundColor background color animation
     * 4.alpha transparency animation
     *
     * @param propKey Property key
     *
     * @return Whether this propKey supports animation
     */
    fun supportAnimation(propKey: String): Boolean = supportAnimationHandlerCreator.containsKey(propKey)

    /**
     * Record pending animations, the execution timing is [commitAnimation]
     *
     * @param animationType Animation type, currently supports the following animation types
     *  1.[KRCssConst.OPACITY]
     *  2.[KRCssConst.TRANSFORM]
     *  3.[KRCssConst.BACKGROUND_COLOR]
     *  4.[KRCssConst.FRAME]
     * @param finalValue Final value of animation
     */
    fun addAnimationOperation(animationType: String, finalValue: Any) {
        // First extract the corresponding property animation handler
        val handler = supportAnimationHandlerCreator[animationType]?.invoke() ?: return
        // Increment element animation count since a new animation was added
        animationHandlerCount++
        // Save the final value of the animation property for setting after animation is complete
        handler.finalValue = finalValue
        // Save animation type
        handler.propKey = animationType
        propKey = animationType
        // Save the property animation handler
        animationOperationMap[animationType] = handler
    }

    /**
     * Batch execute animations previously recorded with [addAnimationOperation] method
     */
    fun commitAnimation(forceCommit: Boolean = false) {
        if (animationCommit && !forceCommit) {
            return
        }
        animationCommit = true
        val targetView = view
        // Extract all animation lists added by addAnimationOperation
        val values = animationOperationMap.values
        if (values.isNotEmpty()) {
            for (value in values) {
                // Start animation
                value.start(targetView, forceCommit) { finished, propKey ->
                    animationHandlerCount--
                    if (animationHandlerCount == 0) {
                        // Animation execution completed, execute animation end callback
                        onAnimationEndBlock?.invoke(
                            this,
                            finished,
                            propKey,
                            animationKey
                        )
                    }
                }
            }
        }
    }

    /**
     * Get current animation key list
     */
    fun animationKeys(): List<String> = animationOperationMap.map { it.value.propKey }

    /**
     * Batch cancel running animations
     */
    fun cancelAnimation(cancelAnimationKeys: List<String>? = null) {
        val values = animationOperationMap.values
        for (value in values) {
            if (cancelAnimationKeys != null) {
                if (cancelAnimationKeys.contains(value.propKey)) {
                    // Align with iOS, when canceling a property animation with the same propKey,
                    // the finish field is true
                    value.forceNotCancel = true
                    value.cancel(value.propKey)
                }
            } else {
                value.cancel()
            }
        }
    }

    /**
     * Remove current animation from animation queue
     */
    fun removeFromAnimationQueue() {
        val animationQueue =
            view.getViewData<LinkedHashMap<Int, KRCSSAnimation>>(KRCssConst.ANIMATION_QUEUE) ?: return
        // Remove from animation queue
        animationQueue.remove(this.hashCode())
        if (animationQueue.isEmpty()) {
            // If the animation queue is empty, remove the element's animation queue
            view.removeViewData<LinkedHashMap<Int, KRCSSAnimation>>(KRCssConst.ANIMATION_QUEUE)
        }
    }

    /**
     * Whether there are animations in the queue
     */
    fun hasAnimations(): Boolean = animationOperationMap.values.isNotEmpty()

    /**
     * Whether it is a repeat animation
     */
    fun isRepeatAnimation(): Boolean = repeatForever

    /**
     * Handle animation end event
     */
    fun handleAnimationEnd(animationType: String? = null) {
        // Extract all animation lists added by addAnimationOperation
        val values = animationOperationMap.values
        if (values.isNotEmpty()) {
            values.forEach { value ->
                if (animationType == null || animationType.contains(value.propKey)) {
                    // Handle animation end event
                    value.end()
                }
            }
        }
    }

    /**
     * Clear all animations
     */
    fun clearAnimation() {
        val values = animationOperationMap.values
        if (values.isNotEmpty()) {
            values.forEach { value ->
                // cancel animation end event
                value.end(true)
            }
        }
    }

    /**
     * Get the web method name corresponding to the linear event method
     */
    fun getCssTimeFuncType(): String {
        return if (isSpring) {
            AnimationTimingFunction.SIMULATE_SPRING_ANIMATION.value
        } else {
            when (timingFuncType) {
                TIMING_FUNC_TYPE_ACCELERATE -> AnimationTimingFunction.EASE_IN.value
                TIMING_FUNC_TYPE_DECELERATE -> AnimationTimingFunction.EASE_OUT.value
                TIMING_FUNC_TYPE_ACCELERATE_DECELERATE -> AnimationTimingFunction.EASE_IN_OUT.value
                else -> AnimationTimingFunction.LINEAR.value
            }
        }
    }

    /**
     * Get the number of frames to be played in the animation
     */
    fun getFrameAnimationRemainCount(): Int {
        var count = 0
        val values = animationOperationMap.values
        if (values.isNotEmpty()) {
            values.forEach { value ->
                if (value.propKey == KRCssConst.FRAME) {
                    // calculate frame animation count
                    val frameValue = value.finalValue.unsafeCast<Frame>()
                    if (view.style.left != "${frameValue.x}px") {
                        count++
                    }
                    if (view.style.top != "${frameValue.y}px") {
                        count++
                    }
                    if (view.style.width != "${frameValue.width}px") {
                        count++
                    }
                    if (view.style.height != "${frameValue.height}px") {
                        count++
                    }
                }
            }
        }

        return count
    }

    /**
     * Format kuikly passed animation parameters
     */
    private fun parseAnimation(animation: String) {
        // Animation split by spaces
        val animationSpilt = animation.split(KRCssConst.BLANK_SEPARATOR)
        // Animation type, linear or spring
        animationType = animationSpilt[ANIMATION_TYPE_INDEX].toInt()
        // Animation time function type
        timingFuncType = animationSpilt[TIMING_FUNC_TYPE_INDEX].toInt()
        // Animation duration
        duration = animationSpilt[DURATION_INDEX].toFloat()
        if (duration <= 0) {
            duration = MINIMUM_DURATION
        }
        // Spring coefficient
        damping = animationSpilt[DAMPING_INDEX].toFloat()
        // Initial velocity
        velocity = animationSpilt[VELOCITY_INDEX].toFloat()
        // Compatible with dynamic code of old version
        if (animationSpilt.size > DELAY_INDEX) {
            delay = animationSpilt[DELAY_INDEX].toFloat()
        }
        if (animationSpilt.size > REPEAT_INDEX) {
            repeatForever = animationSpilt[REPEAT_INDEX].toInt() == 1
        }
        if (animationSpilt.size > ANIMATION_KEY_INDEX) {
            animationKey = animationSpilt[ANIMATION_KEY_INDEX]
        }
        if (animationType == SPRING_ANIMATION_TYPE) {
            isSpring = true
        }
    }

    /**
     * create animation instance
     */
    private fun createAnimationInstance() {
        // create animation instance by animation params
        if (view.kuiklyAnimation == null) {
            // one animation instance for one element
            view.kuiklyAnimation = KuiklyProcessor.animationProcessor.createAnimation(
                AnimationOption(
                    duration = duration,
                    timingFunction = getCssTimeFuncType(),
                    delay = delay,
                    transformOrigin = view.style.transformOrigin,
                )
            )
        }
    }

    /**
     * Set different types of animation processors
     */
    private fun setupAnimationHandler() {
        // Whether it's a spring animation, currently in Web implementation, spring animation is
        // simulated by a simple bezier curve, not yet independently implemented
        supportAnimationHandlerCreator[KRCssConst.OPACITY] = {
            KRCSSPlainAnimationHandler(KRCssConst.OPACITY)
        }
        supportAnimationHandlerCreator[KRCssConst.TRANSFORM] = {
            KRCSSPlainAnimationHandler(KRCssConst.TRANSFORM)
        }
        supportAnimationHandlerCreator[KRCssConst.BACKGROUND_COLOR] = {
            KRCSSPlainAnimationHandler(KRCssConst.BACKGROUND_COLOR)
        }
        supportAnimationHandlerCreator[KRCssConst.FRAME] = {
            KRCSSPlainAnimationHandler(KRCssConst.FRAME)
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

        private const val TIMING_FUNC_TYPE_ACCELERATE = 1
        private const val TIMING_FUNC_TYPE_DECELERATE = 2
        private const val TIMING_FUNC_TYPE_ACCELERATE_DECELERATE = 3

        // zero duration will not trigger transition end event, set to 10ms
        private const val MINIMUM_DURATION = 0.01f
    }
}
