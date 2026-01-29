package com.tencent.kuikly.core.render.web.ktx

import com.tencent.kuikly.core.render.web.const.KRAnimationConst
import com.tencent.kuikly.core.render.web.const.KRAttrConst
import com.tencent.kuikly.core.render.web.const.KRCssConst
import com.tencent.kuikly.core.render.web.const.KREventConst
import com.tencent.kuikly.core.render.web.const.KRExtConst
import com.tencent.kuikly.core.render.web.const.KRJsTypeConst
import com.tencent.kuikly.core.render.web.const.KRParamConst
import com.tencent.kuikly.core.render.web.const.KRPlaceholderConst
import com.tencent.kuikly.core.render.web.const.KRStyleConst
import com.tencent.kuikly.core.render.web.const.KRTagConst
import com.tencent.kuikly.core.render.web.const.KRViewConst
import com.tencent.kuikly.core.render.web.css.animation.KRCSSAnimation
import com.tencent.kuikly.core.render.web.processor.IAnimation
import com.tencent.kuikly.core.render.web.processor.IEvent
import com.tencent.kuikly.core.render.web.processor.KuiklyProcessor
import com.tencent.kuikly.core.render.web.processor.state
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.css.CSSStyleDeclaration
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.get
import kotlin.js.Json
import kotlin.js.Promise
import kotlin.js.json
import kotlin.math.abs

fun String.toPercentage(): String = (toFloat() * 100).toString() + "%"

/**
 * Adapt background value, convert from Kotlin format to web CSS format
 */
fun getCSSBackgroundImage(value: String): String {
    val startIndex = value.indexOf("(")
    val spilt = value.substring(startIndex + 1, value.length - 1).split(",")
    var bgImage = "linear-gradient("
    val direction = when (spilt[0]) {
        "0" -> "to top"
        "1" -> "to bottom"
        "2" -> "to left"
        "3" -> "to right"
        "4" -> "to top left"
        "5" -> "to top right"
        "6" -> "to bottom left"
        "7" -> "to bottom right"
        else -> "to top"
    }
    bgImage += "$direction,"
    for (i in 1 until spilt.size) {
        val colorStopSpilt = spilt[i].split(" ")
        val color = if (colorStopSpilt.size == 2) {
            colorStopSpilt[0].toRgbColor() + " " + colorStopSpilt[1].toFloat() * 100f + "%"
        } else {
            colorStopSpilt[0].toRgbColor()
        }
        bgImage += color
        if (i != spilt.size - 1) {
            bgImage += ","
        }
    }
    return "$bgImage)"
}

/**
 * Get shadow style Web format
 */
fun getShadowString(shadowList: List<String>): String {
    if (shadowList.size < 4) {
        return ""
    }
    val offsetX = shadowList[0].toPxF()
    val offsetY = shadowList[1].toPxF()
    val radius = shadowList[2].toPxF()
    val color = shadowList[3].toRgbColor()
    // Processing successful, return shadow style Web format
    return "$offsetX $offsetY $radius $color"
}

/**
 * Get data associated with Element by key
 * @param T Data type associated with key
 * @param key Key for associated data
 * @return Associated data
 */
fun <T> Element.getViewData(key: String): T? = this.asDynamic()[key].unsafeCast<T?>()

/**
 * Associate key with value and save to View
 * @param key Key to associate
 * @param value Data to associate
 */
fun Element.putViewData(key: String, value: Any) {
    this.asDynamic()[key] = value
}

/**
 * Remove data associated with key
 * @param T Data type
 * @param key Key to remove
 * @return Removed data
 */
fun <T> Element.removeViewData(key: String): T? {
    this.asDynamic()[key] = null

    return null
}

/**
 * Get web transform content for element, terminal provides values in format:
 * "$rotate|$scale|$translateOffset|$anchor|$skew"
 */
fun getCSSTransform(value: Any): Array<String> {
    val transformSpilt = value.unsafeCast<String>().split("|")

    val anchorSpilt = transformSpilt[3].split(" ")
    val anchorX = anchorSpilt[0].toPercentage()
    val anchorY = anchorSpilt[1].toPercentage()
    val transformOrigin = "$anchorX $anchorY"

    val translateSpilt = transformSpilt[2].split(" ")
    val translateX = translateSpilt[0].toPercentage()
    val translateY = translateSpilt[1].toPercentage()

    val rotate = "${transformSpilt[0]}deg"

    val scaleSpilt = transformSpilt[1].split(" ")
    val scaleX = scaleSpilt[0]
    val scaleY = scaleSpilt[1]

    val skewSplit = transformSpilt[4].split(" ")
    val skewX = "${skewSplit[0]}deg"
    val skewY = "${skewSplit[1]}deg"

    return arrayOf(
        transformOrigin,
        "translate($translateX, $translateY) rotate(${rotate}) scale($scaleX, $scaleY) skew($skewX, $skewY)"
    )
}

fun convertGradientStringToCssMask(gradientStr: String): String {
    val directionMap = mapOf(
        0 to "to top",
        1 to "to bottom",
        2 to "to left",
        3 to "to right",
        4 to "to top left",
        5 to "to top right",
        6 to "to bottom left",
        7 to "to bottom right"
    )

    val pattern = """linear-gradient\((\d+),(.+)\)""".toRegex()
    val match = pattern.find(gradientStr) ?: return gradientStr

    val (dirNum, stops) = match.destructured
    val direction = directionMap[dirNum.toInt()] ?: "to bottom"

    // Improved color stop parsing
    val colorStopPattern = """rgba\((\d+),(\d+),(\d+),([\d.]+)\)\s*([\d.]+)?""".toRegex()
    val processedStops = stops.split(",(?![^()]*\\))".toRegex()).joinToString(", ") { stop ->
        colorStopPattern.find(stop)?.let { mr ->
            val (r, g, b, a, pos) = mr.destructured
            val position = pos.takeIf { it.isNotEmpty() }?.let {
                "${(it.toFloat() * 100).toInt()}%"
            } ?: ""
            "rgba($r,$g,$b,${a.toFloat()})${if (position.isNotEmpty()) " $position" else ""}"
        } ?: stop
    }

    return "linear-gradient($direction, $processedStops)"
}

/**
 * Extend element animation completion method
 */
var Element.animationCompletionBlock: KuiklyRenderCallback?
    get() = getViewData(KRCssConst.ANIMATION_COMPLETION_BLOCK)
    set(value) {
        if (value != null) {
            putViewData(KRCssConst.ANIMATION_COMPLETION_BLOCK, value)
        } else {
            removeViewData<KuiklyRenderCallback>(KRCssConst.ANIMATION_COMPLETION_BLOCK)
        }
    }

/**
 * Check and update position for H5 with border offset
 */
private fun Element.checkAndUpdatePositionForH5(frame: Frame, style: CSSStyleDeclaration) {
    // update style for h5
    if (style.borderWidth.endsWith(KRStyleConst.PX_SUFFIX)) {
        val borderWidth = style.borderWidth.pxToDouble()
        Promise.resolve(null).then {
            // if element has border, then element is border-box, then adjust the children's offset
            for (i in 0 until this.children.length) {
                val child = this.children[i].unsafeCast<HTMLElement>()
                val dynamicChild = child.asDynamic()
                child.style.left = (dynamicChild.rawLeft.unsafeCast<Double>() - borderWidth).toPxF()
                child.style.top = (dynamicChild.rawTop.unsafeCast<Double>() - borderWidth).toPxF()
                // add sign
                dynamicChild.isAdujustedOffset = true
            }
        }
        
        // Only apply border adjustment for span or p elements
        val tagName = this.tagName.lowercase()
        if (tagName == KRTagConst.SPAN || tagName == KRTagConst.P) {
            frame.width += 2 * borderWidth
            frame.height += 2 * borderWidth
            frame.x -= borderWidth
            frame.y -= borderWidth
        }
    }

    // handle offset for dynamic child
    parentElement.unsafeCast<HTMLElement?>()?.let { parent ->
        val dynamicChild = this.asDynamic()
        if (parent.style.borderWidth.endsWith(KRStyleConst.PX_SUFFIX) && jsTypeOf(dynamicChild.isAdujustedOffset) == KRJsTypeConst.UNDEFINED) {
            // adjust offset for non adjusted child
            val borderWidth = parent.style.borderWidth.pxToDouble()
            style.left = (frame.x - borderWidth).toPxF()
            style.top = (frame.y - borderWidth).toPxF()
        }
    }
}

/**
 * Set element frame
 */
fun Element.setFrame(frame: Frame, style: CSSStyleDeclaration) {
    // dynamic this
    val dynamicElement = this.asDynamic()
    // Because terminal position data is relative to parent element, web sets element position to absolute, then
    // Exactly relative to parent element position offset
    style.position = KRStyleConst.POSITION_ABSOLUTE
    // Left offset
    val left = frame.x
    // Top offset
    val top = frame.y
    // save raw left and top value to element props
    dynamicElement.rawLeft = left
    dynamicElement.rawTop = top

    // adjust element offset for border element
    if (style.asDynamic().checkAndUpdatePosition != null) {
        // update style for miniapp
        style.asDynamic().checkAndUpdatePosition()
    } else {
        checkAndUpdatePositionForH5(frame, style)
    }

    // set element frame
    style.left = frame.x.toPxF()
    style.top = frame.y.toPxF()
    style.width = frame.width.toPxF()
    style.height = frame.height.toPxF()
}

/**
 * get element index in parent
 */
fun indexOfChild(node: Element?): Int {
    val parent = node?.parentElement
    if (parent != null) {
        val childNodes = parent.childNodes
        val length = parent.childElementCount
        for (i in 0 until length) {
            if (node === childNodes[i]) {
                return i
            }
        }
    }
    return -1
}

/**
 * Extend CSSAnimation property for View
 * Represents the animation currently being set
 */
var Element.hrAnimation: KRCSSAnimation?
    get() = getViewData(KRCssConst.ANIMATION)
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
 * Extend CSSAnimation property for View
 * Represents the animation currently being set
 */
var Element.kuiklyAnimation: IAnimation?
    get() = getViewData(KRCssConst.KUIKLY_ANIMATION)
    set(value) {
        val animation = kuiklyAnimation
        if (value == animation) {
            return
        }
        if (value == null) {
            removeViewData<dynamic>(KRCssConst.KUIKLY_ANIMATION) ?: return
        } else {
            putViewData(KRCssConst.KUIKLY_ANIMATION, value)
        }
    }

/**
 * Try to record animation operation
 * @param key Attribute key
 * @param value Attribute value
 *
 * @return Returns true if the key attribute is supported by animation and records a pending animation to CSSAnimation
 * Returns false if the key attribute is not supported by animation
 */
fun Element.tryAddAnimationOperation(key: String, value: Any): Boolean {
    val animation = hrAnimation
    // When the first attribute is set, the animation object is null, and the attribute value is set
    // normally at this time. When the animation is set, hrAnimation is not null
    // , subsequent attribute settings need to add animation operations and save animation keyFrames
    if (animation != null && animation.supportAnimation(key)) {
        animation.addAnimationOperation(key, value)
        return true
    }
    return false
}

/**
 * Extend View with [kuiklyAnimationGroup] property
 * Represents the animation queue created after wxAnimation.export
 */
var Element.kuiklyAnimationGroup: dynamic
    get() = getViewData(KRAnimationConst.KUIKLY_ANIMATION_GROUP)
    set(value) {
        putViewData(KRAnimationConst.KUIKLY_ANIMATION_GROUP, value as Any)
    }

/**
 * Extend View with [isBindAnimationEndEvent] property
 */
var Element.isBindAnimationEndEvent: Boolean
    get() {
        val isBindEvent = getViewData<dynamic>(KRAnimationConst.IS_BIND_ANIMATION_END_EVENT) ?: return false
        return isBindEvent.unsafeCast<Boolean>()
    }
    set(value) {
        this.asDynamic().isBindAnimationEndEvent = value
    }

/**
 * Extend View with [isRepeatAnimation] property
 */
var Element.isRepeatAnimation: Boolean
    get() {
        val isRepeatAnimation = getViewData<dynamic>(KRAnimationConst.IS_REPEAT_ANIMATION) ?: return false
        return isRepeatAnimation.unsafeCast<Boolean>()
    }
    set(value) {
        this.asDynamic().isRepeatAnimation = value
    }

/**
 * Extend View with [frameAnimationEndCount] property
 */
var Element.frameAnimationEndCount: Int
    get() {
        return getViewData<dynamic>(KRAnimationConst.FRAME_ANIMATION_END_COUNT) ?: 0
    }
    set(value) {
        this.asDynamic().frameAnimationEndCount = value
    }

/**
 * Extend View with [frameAnimationRemainCount] property
 */
var Element.frameAnimationRemainCount: Int
    get() {
        return getViewData<dynamic>(KRAnimationConst.FRAME_ANIMATION_REMAIN_COUNT) ?: 0
    }
    set(value) {
        this.asDynamic().frameAnimationRemainCount = value
    }

/**
 * Extend View with [exportAnimationTimeoutId] property
 */
var Element.exportAnimationTimeoutId: Int
    get() {
        return getViewData<dynamic>(KRAnimationConst.EXPORT_ANIMATION_TIMEOUT_ID) ?: 0
    }
    set(value) {
        this.asDynamic().exportAnimationTimeoutId = value
    }

/**
 * set common prop for element
 */
fun Element.setCommonProp(key: String, value: Any): Boolean {
    val ele = this.unsafeCast<HTMLElement>()
    val dynamicElement = ele.asDynamic()
    if (this.tryAddAnimationOperation(key, value)) {
        // If it is animation operation setting, set successfully,
        // animation operation attribute value has been set, no need
        // Set here, directly return
        return true
    }

    // Otherwise use unified setting method
    val result = propHandlers[key]?.invoke(ele.style, value, ele) ?: false

    if (ele.style.borderWidth.isNotEmpty()) {
        Promise.resolve(null).then{
            // re calculate element size for mini app
            if (jsTypeOf(dynamicElement.forceUpdateChildrenStyle) == KRJsTypeConst.FUNCTION) {
                dynamicElement.forceUpdateChildrenStyle()
            }
        }
    }

    return result
}

/**
 * Get current element animation queue
 */
private fun Element.getAnimationQueue(): LinkedHashMap<Int, KRCSSAnimation>? =
    getViewData<LinkedHashMap<Int, KRCSSAnimation>>(KRCssConst.ANIMATION_QUEUE)

/**
 * Add animation to animation queue
 * @param hrAnimation Animation to be added
 */
private fun Element.addKRAnimation(hrAnimation: KRCSSAnimation) {
    val animationQueue = getAnimationQueue()
        ?: LinkedHashMap<Int, KRCSSAnimation>().apply {
            putViewData(KRCssConst.ANIMATION_QUEUE, this)
        }
    animationQueue[hrAnimation.hashCode()] = hrAnimation
}

/**
 * export animation group and real execute animation
 *
 * @param group Animation group to export, string for h5 and object for mini app
 */
fun Element.exportAnimation(group: dynamic) {
    this.setAttribute(KRAttrConst.ANIMATION, group)
}

/**
 * Get animation step option
 */
fun Element.getAnimationStepOption(animation: KRCSSAnimation?): Json {
    return json(
        KRAnimationConst.DURATION to animation?.duration,
        KRAnimationConst.DELAY to animation?.delay,
        KRAnimationConst.TIMING_FUNCTION to animation?.getCssTimeFuncType(),
        KRAnimationConst.TRANSFORM_ORIGIN to this.unsafeCast<HTMLElement>().style.transformOrigin
    )
}

/**
 * 重复执行 style 类型的动画
 */
fun Element.repeatStyleAnimation() {
    val dynamicElement = this.asDynamic()
    val ele = this.unsafeCast<HTMLElement>()
    val dynamicStyle = ele.style.asDynamic()
    val hasAnimationData = jsTypeOf(dynamicElement.animationData)
    if (hasAnimationData == KRJsTypeConst.OBJECT) {
        // 处理 json 动画数据
        val data = dynamicElement.animationData.unsafeCast<Json>()
        // 首先移除已有 transition
        ele.style.transition = KRCssConst.EMPTY_STRING
        // 然后还原属性旧值
        val rules = data[KRAnimationConst.RULES].unsafeCast<Json?>()
        rules?.let {
            val ruleList = js("Object.keys(rules)") as Array<String>
            for (key in ruleList) {
                val value = rules[key].unsafeCast<Json>()
                // 还原旧值
                dynamicStyle[key] = value[KRAnimationConst.OLD_VALUE]
            }

            // 然后再设置新值
            kuiklyWindow.setTimeout({
                // 先设置 transition
                ele.style.transition = data[KRAnimationConst.TRANSITION].unsafeCast<String>()
                // 然后再设置新值
                for (key in ruleList) {
                    val value = rules[key].unsafeCast<Json>()
                    // 还原旧值
                    dynamicStyle[key] = value[KRAnimationConst.NEW_VALUE]
                }
            }, AnimationTimingConst.STYLE_ANIMATION_RESTART_DELAY_MS)
        }
    }
}

/**
 * handle transition end event for prop animation for h5
 */
fun Element.handlePropTransitionEnd(name: String) {
    var propertyName = name
    // 此处为 H5 动画结束处理
    if ((KRCssConst.FRAME_ATTRS.indexOf(propertyName) >= 0)) {
        // transition end event，多个属性会触发多次，需要进行处理，其中 width, height, left, top
        // 都属于 frame 动画，按同一类型处理
        propertyName = KRCssConst.FRAME
        // frame 动画结束次数加1
        frameAnimationEndCount++
        // 计算还剩余的 frame 变化动画次数
    } else if (propertyName == KRCssConst.BACKGROUND_COLOR_ATTR) {
        propertyName = KRCssConst.BACKGROUND_COLOR
    }
    // Animation queue - 使用副本遍历避免并发修改
    val animationQueue = getAnimationQueue()
    if (animationQueue != null) {
        // 创建队列的副本用于安全遍历
        val animationQueueCopy = LinkedHashMap(animationQueue)
        animationQueueCopy.forEach { animation ->
            val animationKeys = animation.value.animationKeys()
            if (animationKeys.isNotEmpty() && animationKeys.contains(propertyName)) {
                if (isRepeatAnimation || animation.value.isRepeatAnimation()) {
                    // repeat animation should repeat
                    isRepeatAnimation = true
                    return@forEach
                }
                if (propertyName == KRCssConst.FRAME) {
                    // 计算 frame 动画结束次数
                    frameAnimationRemainCount = animation.value.getFrameAnimationRemainCount()
                }
                // 如果是其他类型的动画，则直接调用动画结束事件。如果是 frame 动画，则需要变动的属性都完成
                // 才调用动画结束事件
                if (
                    propertyName != KRCssConst.FRAME ||
                    (frameAnimationRemainCount in 1..frameAnimationEndCount)
                ) {
                    // Animation ends, notify all animation processors, if animation type matches,
                    // handle animation end event
                    animation.value.handleAnimationEnd(propertyName)
                }
            } else if (animationKeys.isEmpty()) {
                // empty animation instance, remove from animation queue
                animation.value.removeFromAnimationQueue()
            }
        }
    }
    // remove animation attribute and set animation again whether is repeat animation
    if (isRepeatAnimation) {
        exportAnimation("")
        // execute animation again
        kuiklyWindow.setTimeout({
            exportAnimation(kuiklyAnimationGroup)
        }, AnimationTimingConst.REPEAT_ANIMATION_DELAY_MS)
    } else if (getAnimationQueue() == null) {
        // remove animation attribute
        exportAnimation("")
    }
}

/**
 * handle transition end event for style animation for mini app
 */
fun Element.handleStyleTransitionEnd() {
    // 小程序的 transitionend 一个动画只会触发一次，且没有propertyName，因此有回调就移除一个动画
    // 不像 h5 对于 frame 动画
    // 首先移除空动画 - 使用副本遍历避免并发修改
    val animationQueue1 = getAnimationQueue()
    if (animationQueue1 != null) {
        // 创建队列的副本用于安全遍历
        val animationQueueCopy1 = LinkedHashMap(animationQueue1)
        animationQueueCopy1.forEach { animation ->
            val animationKeys = animation.value.animationKeys()
            if (animationKeys.isEmpty()) {
                // empty animation instance, remove from animation queue
                animation.value.removeFromAnimationQueue()
            }
        }
    }
    // 接着移除第一个动画即可
    val animationQueue = getAnimationQueue()
    if (animationQueue != null) {
        val size = animationQueue.size
        if (size > 0) {
            val animation = animationQueue.entries.first().value
            // remove first animation
            if (isRepeatAnimation || animation.isRepeatAnimation()) {
                // repeat animation should repeat，重复动画不处理结束逻辑
                isRepeatAnimation = true
            } else {
                // 调用动画结束处理方法
                animation.handleAnimationEnd()
            }
        }
        if (animationQueue.size == 0) {
            // animation end, remove transition attribute
            this.unsafeCast<HTMLElement>().style.transition = ""
        }
    }

    if (isRepeatAnimation) {
        // 如果是重复动画，则要重新处理
        repeatStyleAnimation()
    }
}

/**
 * bind animation end event
 */
fun Element.bindAnimationEndEvent() {
    if (!isBindAnimationEndEvent) {
        // bind animation end event, only once
        isBindAnimationEndEvent = true
        // bind animation end event
        this.addEventListener(KREventConst.TRANSITION_END, { event: dynamic ->
            val propertyName = event.propertyName.unsafeCast<String>()
            if (jsTypeOf(propertyName) == KRJsTypeConst.STRING) {
                handlePropTransitionEnd(propertyName)
            } else {
                handleStyleTransitionEnd()
            }
        })
    }
}

/**
 * clear animation timeout record
 */
fun Element.clearAnimationTimeout() {
    val animationQueue = getAnimationQueue()
    if (animationQueue == null && exportAnimationTimeoutId > 0) {
        // all animation is finished or canceled, then clear animation timeout
        kuiklyWindow.clearTimeout(exportAnimationTimeoutId)
        // reset timeout id
        exportAnimationTimeoutId = 0
    }
}

/**
 * Set animation description class, for kuikly
 * 1. set animation props begin value
 * 2. add animation to animation queue
 * 3. set animation props end value, and really start animation
 *
 * @param animation Description animation string
 */
fun Element.setKRAnimation(animation: String?) {
    // bind animation end event first
    bindAnimationEndEvent()

    when {
        animation == null -> {
            // Clear animation - 使用副本遍历避免并发修改
            val animationQueue = getAnimationQueue()
            if (animationQueue != null) {
                // 创建队列的副本用于安全遍历
                val animationQueueCopy = LinkedHashMap(animationQueue)
                animationQueueCopy.forEach { (_, v) ->
                    v.cancelAnimation()
                    v.removeFromAnimationQueue()
                }
            }
        }

        animation.isEmpty() -> {
            // 某些情况下，kuikly会分两次提交动画，我们需要将其合并，因此此时需要再次提交之前的动画
            var forceCommit = false
            if (exportAnimationTimeoutId > 0) {
                // 某些情况下，kuikly会分两次提交动画，我们需要将其合并
                kuiklyWindow.clearTimeout(exportAnimationTimeoutId)
                forceCommit = true
            }

            // Set animation end value, and really start animation
            val commitAnimation = hrAnimation
            // Animation content is empty
            hrAnimation = null
            val animationQueue = getAnimationQueue()
            if (animationQueue != null) {
                // commit last animation
                animationQueue.forEach { (_, v) ->
                    // commit new animation, old animation has been committed
                    v.commitAnimation(forceCommit)
                }
                // generate last step animation string
                kuiklyAnimation?.step(getAnimationStepOption(commitAnimation))
                // execute animation
                kuiklyAnimationGroup = kuiklyAnimation?.export(this.unsafeCast<HTMLElement>())
                if (kuiklyAnimationGroup != null && kuiklyAnimationGroup != "") {
                    // 小程序的动画此处返回为空，因此不做 animation 属性的设置
                    // execute animation
                    exportAnimationTimeoutId = kuiklyWindow.setTimeout({
                        exportAnimation(kuiklyAnimationGroup)
                    }, AnimationTimingConst.ANIMATION_EXPORT_DELAY_MS)
                }
            }
        }

        else -> {
            if (isRepeatAnimation) {
                // clear repeat animation status
                // Animation queue - 使用副本遍历避免并发修改
                val animationQueue = getAnimationQueue()
                if (animationQueue != null) {
                    // 创建队列的副本用于安全遍历
                    val animationQueueCopy = LinkedHashMap(animationQueue)
                    animationQueueCopy.forEach { animate ->
                        // handle animation end event
                        animate.value.clearAnimation()
                    }
                }
                isRepeatAnimation = false
            }

            // Set animation props begin value and ini animation instance
            val newAnimation = KRCSSAnimation(animation, this)
            newAnimation.onAnimationEndBlock =
                { hrAnimation: KRCSSAnimation, finished, propKey, animationKey ->
                    animationCompletionBlock?.invoke(
                        mapOf(
                            KRAnimationConst.FINISH to if (finished) 1 else 0,
                            KRAnimationConst.ATTR to propKey,
                            KRAnimationConst.ANIMATION_KEY to animationKey
                        )
                    )
                    hrAnimation.removeFromAnimationQueue()
                    // clear animation timeout when finished all animations all cancelled
                    clearAnimationTimeout()
                }
            if (hrAnimation != null && hrAnimation?.hasAnimations() == true) {
                // commit animation when has animations
                hrAnimation?.commitAnimation()
                // generate current step animation string
                kuiklyAnimation?.step(getAnimationStepOption(hrAnimation))
            }
            hrAnimation = newAnimation
            // Add animation to queue
            addKRAnimation(newAnimation)
        }
    }
}

/**
 * Unified handling logic for common properties
 */
private val propHandlers = mapOf<String, (CSSStyleDeclaration, Any, HTMLElement) -> Boolean>(
    // Set element properties
    KRCssConst.OPACITY to { cssStyle, value, _ ->
        cssStyle.opacity = value.unsafeCast<Number>().toString()
        true
    },
    KRCssConst.VISIBILITY to { cssStyle, value, _ ->
        cssStyle.visibility = if (value.unsafeCast<Int>() == 0) "hidden" else "visible"
        true
    },
    KRCssConst.OVERFLOW to { cssStyle, value, _ ->
        // When overflow is set to visible in Web, content is not clipped and will be rendered
        // outside the element box. In Android, this can be achieved by setting clipChildren to
        // false.When overflow is set to hidden in Web, content is clipped and the rest is invisible.
        // In Android, this can be achieved by setting clipChildren to true. When overflow is set to
        // scroll or auto in Web, content is clipped but the browser displays scrollbars to view
        // the rest of the content. In Android, this effect may require using scroll containers
        // like ScrollView or RecyclerView, and the clipChildren property still needs to be set to true.
        val overflow = if (value.unsafeCast<Int>() == 1) "hidden" else "visible"
        cssStyle.overflowX = overflow
        cssStyle.overflowY = overflow
        true
    },
    KRCssConst.BACKGROUND_COLOR to { cssStyle, value, _ ->
        cssStyle.backgroundColor = value.unsafeCast<String>().toRgbColor()
        true
    },
    KRCssConst.TOUCH_ENABLE to { cssStyle, value, ele ->
        ele.onclick = null
        ele.onprogress = null
        ele.ondblclick = null
        // pointerEvents is w3c standard, it can control whether the element can be clicked.
        // but it is not supported by kotlin/JS, so use asDynamic() to set it.
        cssStyle.asDynamic().pointerEvents = if (value.unsafeCast<Int>() == 0) "none" else "auto"
        true
    },
    KRCssConst.TRANSFORM to { cssStyle, value, _ ->
        // Get and set transform value
        val transformContent = getCSSTransform(value)
        cssStyle.transformOrigin = transformContent[0]
        cssStyle.transform = transformContent[1]
        true
    },
    KRCssConst.BACKGROUND_IMAGE to { cssStyle, value, _ ->
        val stringValue = value.unsafeCast<String>()
        // If it's RichTextView, do not set backgroundImage, Web currently doesn't support
        // background text changing color together
        val backgroundImagePrefix = "background-image: "
        if (stringValue.startsWith(backgroundImagePrefix)) {
            cssStyle.backgroundImage = stringValue.substring(
                backgroundImagePrefix.length,
                stringValue.length - 1
            )
        } else {
            cssStyle.backgroundImage = getCSSBackgroundImage(stringValue)
        }
        true
    },
    KRCssConst.BOX_SHADOW to { cssStyle, value, _ ->
        val boxShadowSpilt = (value as String).split(" ")
        cssStyle.boxShadow = getShadowString(boxShadowSpilt)
        true
    },
    KRCssConst.TEXT_SHADOW to { cssStyle, value, _ ->
        val textShadowSpilt = (value as String).split(" ")
        cssStyle.textShadow = getShadowString(textShadowSpilt)
        true
    },
    KRCssConst.STROKE_WIDTH to { cssStyle, value, _ ->
        val usedWidth = value.asDynamic() / 4
        val dynamicCssStyle = cssStyle.asDynamic()
        dynamicCssStyle.webkitTextStroke = "${usedWidth}px ${dynamicCssStyle.webkitTextStroke}"
        true
    },
    KRCssConst.STROKE_COLOR to { cssStyle, value, _ ->
        cssStyle.asDynamic().webkitTextStroke = "$value"
        true
    },
    KRCssConst.BORDER_RADIUS to { cssStyle, value, _ ->
        with(cssStyle) {
            val borderRadiusSpilt = value.unsafeCast<String>().asDynamic().split(",")
            val baseRadius = borderRadiusSpilt[0]
            if (baseRadius == borderRadiusSpilt[1] && baseRadius == borderRadiusSpilt[2] && baseRadius == borderRadiusSpilt[3]) {
                borderRadius = borderRadiusSpilt[0].unsafeCast<String>().toPxF()
            } else {
                borderTopLeftRadius = borderRadiusSpilt[0].unsafeCast<String>().toPxF()
                borderTopRightRadius = borderRadiusSpilt[1].unsafeCast<String>().toPxF()
                borderBottomLeftRadius = borderRadiusSpilt[2].unsafeCast<String>().toPxF()
                borderBottomRightRadius = borderRadiusSpilt[3].unsafeCast<String>().toPxF()
            }
            this.asDynamic().overflow = "hidden"
        }
        true
    },
    KRCssConst.BORDER to { cssStyle, value, _ ->
        val borders = value.unsafeCast<String>().split(" ")
        cssStyle.borderWidth = borders[0].toFloat().toPxF()
        cssStyle.borderStyle = borders[1]
        cssStyle.borderColor = borders[2].toRgbColor()
        // set box-sizing to border-box for border item
        cssStyle.boxSizing = "border-box"
        true
    },
    KRCssConst.CLICK to { _, value, ele ->
        // Check if it is a PC device (precise pointing device like mouse)
        val isPCDevice = kuiklyWindow.matchMedia(ClickDetectionConst.POINTER_FINE_QUERY).matches

        // Record mousedown position for PC drag/selection detection (only for PC)
        if (isPCDevice) {
            ele.addEventListener("mousedown", { event ->
                val mouseEvent = event.unsafeCast<MouseEvent>()
                ele.asDynamic().clickStartX = mouseEvent.clientX
                ele.asDynamic().clickStartY = mouseEvent.clientY
            })
        }

        ele.addEventListener("click", { event ->
            // If a pan or long-press has been triggered, ignore the click.
            val panOrLongPressTriggered = ele.asDynamic().panOrLongPressTriggered == true
            if (panOrLongPressTriggered) {
                return@addEventListener
            }

            // PC optimization: filter out drag/selection operations (only for PC)
            if (isPCDevice) {
                // Check if text is selected (user is selecting text, not clicking)
                val selection = kuiklyWindow.asDynamic().getSelection()
                val selectedText = selection?.toString() ?: ""
                if (selectedText.unsafeCast<String>().isNotEmpty()) {
                    return@addEventListener
                }

                // Check if mouse moved significantly (drag operation, not click)
                val startX = ele.asDynamic().clickStartX
                val startY = ele.asDynamic().clickStartY
                if (startX != null && startY != null) {
                    val mouseEvent = event.unsafeCast<MouseEvent>()
                    val deltaX = abs(mouseEvent.clientX - startX.unsafeCast<Int>())
                    val deltaY = abs(mouseEvent.clientY - startY.unsafeCast<Int>())
                    if (deltaX > ClickDetectionConst.MOVE_TOLERANCE || deltaY > ClickDetectionConst.MOVE_TOLERANCE) {
                        return@addEventListener
                    }
                }
            }

            // Check whether the current element is marked as having a double click handler registered
            val hasBindDoubleClick = ele.asDynamic().hasDoubleClickListener == true
            val clickEvent = event.asDynamic()
            clickEvent.stopPropagation()
            // If no double click handler is registered, invoke the click callback
            if (!hasBindDoubleClick) {
                value.unsafeCast<KuiklyRenderCallback>().invoke(
                    mapOf(
                        "x" to clickEvent.offsetX.unsafeCast<Double>().toFloat(),
                        "y" to clickEvent.offsetY.unsafeCast<Double>().toFloat()
                    )
                )
            } else {
                // If a double click handler is registered
                // If the timer exists , clear it (reset the timing)
                val prevTimer = ele.asDynamic().clickTimer
                if (prevTimer != null) kuiklyWindow.clearTimeout(prevTimer)
                // Start a new timer and save it
                ele.asDynamic().clickTimer = kuiklyWindow.setTimeout({
                    // If the double click callback is not triggered within 200ms, invoke the click callback
                    value.unsafeCast<KuiklyRenderCallback>().invoke(
                        mapOf(
                            "x" to clickEvent.offsetX.unsafeCast<Double>().toFloat(),
                            "y" to clickEvent.offsetY.unsafeCast<Double>().toFloat()
                        )
                    )
                    // clear the timer
                    ele.asDynamic().clickTimer = null
                }, AnimationTimingConst.DOUBLE_CLICK_TIMEOUT_MS)
            }

        })
        true
    },
    KRCssConst.DOUBLE_CLICK to { _, value, ele ->
        // Mark the element as having a double click handler registered
        ele.asDynamic().hasDoubleClickListener = true
        // When the double click event is triggered, invoke the double click callback
        KuiklyProcessor.eventProcessor.doubleClick(ele) { event: IEvent? ->
            event?.let {
                value.unsafeCast<KuiklyRenderCallback>().invoke(
                    mapOf(
                        "x" to it.offsetX.toFloat(),
                        "y" to it.offsetY.toFloat()
                    )
                )
                // Clear the timer to prevent the click callback from being invoked afterward
                val timer = ele.asDynamic().clickTimer
                if (timer != null) {
                    kuiklyWindow.clearTimeout(timer)
                    ele.asDynamic().clickTimer = null
                }
            }
        }
        true
    },
    KRCssConst.LONG_PRESS to { _, value, ele ->
        KuiklyProcessor.eventProcessor.longPress(ele) { event ->
            event?.let {
                value.unsafeCast<KuiklyRenderCallback>().invoke(
                    mapOf(
                        "x" to it.clientX.toFloat(),
                        "y" to it.clientY.toFloat(),
                        "state" to it.state
                    )
                )
            }
        }
        true
    },
    KRCssConst.PAN to { _, value, ele ->
        KuiklyProcessor.eventProcessor.pan(ele) { event ->
            event?.let {
                value.unsafeCast<KuiklyRenderCallback>().invoke(
                    mapOf(
                        "x" to it.clientX.toFloat() - ele.getBoundingClientRect().left,
                        "y" to it.clientY.toFloat() - ele.getBoundingClientRect().top,
                        "state" to it.state,
                        "pageX" to it.pageX.toFloat(),
                        "pageY" to it.pageY.toFloat(),
                    )
                )
            }
        }
        true
    },
    KRCssConst.ANIMATION to { _, value, ele ->
        ele.setKRAnimation(value.unsafeCast<String>())
        true
    },
    KRCssConst.MASK_LINEAR_GRADIENT to { cssStyle, value, _ ->
        val linearValue = convertGradientStringToCssMask(value.unsafeCast<String>())
        cssStyle.asDynamic().webkitMask = linearValue
        true
    },
    KRCssConst.FRAME to { cssStyle, value, ele ->
        ele.setFrame(value.unsafeCast<Frame>(), cssStyle)
        true
    },
    KRCssConst.ANIMATION_COMPLETION_BLOCK to { _, value, ele ->
        ele.animationCompletionBlock = value.unsafeCast<KuiklyRenderCallback>()
        true
    },
    KRCssConst.COLOR to { cssStyle, value, _ ->
        // Convert numeric color value to rgba format
        cssStyle.color = value.unsafeCast<String>().toRgbColor()
        true
    },
    KRCssConst.Z_INDEX to { cssStyle, value, _ ->
        cssStyle.zIndex = value.unsafeCast<Int>().toString()
        true
    },
    KRCssConst.ACCESSIBILITY to { _, value, ele ->
        // Set accessibility text
        ele.setAttribute("aria-label", value.unsafeCast<String>())
        true
    },
)

/**
 * Set placeholder color
 */
fun setPlaceholderColor(el: HTMLElement, color: String) {
    // 生成唯一 class
    val uniqueClass = "phcolor_" + kotlin.random.Random.nextInt(1_000_000)
    el.classList.add(uniqueClass)

    // 构造标准 CSS 规则
    val css = """
        .${uniqueClass}::placeholder { color: $color; opacity: 1; }
    """.trimIndent()
    // 插入 style 标签
    val style = kuiklyDocument.createElement("style")
    style.setAttribute("type", "text/css")
    style.appendChild(kuiklyDocument.createTextNode(css))
    kuiklyDocument.head?.appendChild(style)
}

/**
 * Animation timing constants
 */
private object AnimationTimingConst {
    /** Delay for style animation restart in milliseconds */
    const val STYLE_ANIMATION_RESTART_DELAY_MS = 50
    /** Delay for animation export in milliseconds */
    const val ANIMATION_EXPORT_DELAY_MS = 10
    /** Delay for repeat animation in milliseconds */
    const val REPEAT_ANIMATION_DELAY_MS = 10
    /** Double click detection timeout in milliseconds */
    const val DOUBLE_CLICK_TIMEOUT_MS = 200
}

/**
 * Click detection constants for PC optimization
 */
private object ClickDetectionConst {
    /** Movement tolerance in pixels - if mouse moves more than this, it's considered a drag, not a click */
    const val MOVE_TOLERANCE = 5
    /** Media query for precise pointing device (PC mouse) */
    const val POINTER_FINE_QUERY = "(pointer: fine)"
}