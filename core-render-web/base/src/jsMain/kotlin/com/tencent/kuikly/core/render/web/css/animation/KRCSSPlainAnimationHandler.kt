package com.tencent.kuikly.core.render.web.css.animation

import com.tencent.kuikly.core.render.web.collection.map.JsMap
import com.tencent.kuikly.core.render.web.collection.map.get
import com.tencent.kuikly.core.render.web.collection.map.set
import com.tencent.kuikly.core.render.web.ktx.Frame
import com.tencent.kuikly.core.render.web.ktx.KRCssConst
import com.tencent.kuikly.core.render.web.ktx.kuiklyAnimation
import com.tencent.kuikly.core.render.web.ktx.toPercentage
import com.tencent.kuikly.core.render.web.ktx.toRgbColor
import org.w3c.dom.HTMLElement


/**
 * Property animation handler
 *
 * @param property Animation property value to be set
 */
class KRCSSPlainAnimationHandler(
    private val property: String,
) : KRCSSAnimationHandler() {
    // Animation completion callback
    private var animationEndBlock: ((finished: Boolean, propKey: String) -> Unit)? = null

    /**
     * Start animation
     */
    override fun start(
        target: HTMLElement,
        forceCommit: Boolean,
        onAnimationEndBlock: (finished: Boolean, propKey: String) -> Unit
    ) {
        // Save current element reference
        this.target = target
        // Save animation end callback
        animationEndBlock = onAnimationEndBlock
        finalValue?.let {
            // Get actual API and parameters to execute
            val operateValueMap = getAPIAnimationValue(property, it)
            // apply animation
            applyAnimationHandler(target, property, operateValueMap)
        }
    }

    /**
     * Cancel animation
     */
    override fun cancel(propKey: String?) {
        // new animation set, old animation canceled
    }

    /**
     * Remove animation end event listener
     */
    override fun removeListener() {
        // transitionend event has been listened at element, no need to handle here
    }

    /**
     * Animation end callback, events are uniformly bound and handled at the element layer
     */
    override fun end(cancel: Boolean) {
        // no use now
        animationEndBlock?.invoke(getFinishValue(cancel), property)
        // set animation finalValue
        applyFinalValue()
    }

    /**
     * apply animation final value for element
     */
    private fun applyFinalValue() {
        // After each animation ends, assign the animation final value to the corresponding property of the element
        finalValue?.let { value ->
            when (property) {
                KRCssConst.TRANSFORM -> {
                    getCSSTransform(value).let {
                        target?.style?.transform = it["transform"]!!
                    }
                }

                KRCssConst.OPACITY -> {
                    target?.style?.opacity = value.unsafeCast<Number>().toString()
                }

                KRCssConst.BACKGROUND_COLOR -> {
                    target?.style?.backgroundColor = value.unsafeCast<String>().toRgbColor()
                }

                KRCssConst.FRAME -> {
                    val frameValue = value.unsafeCast<Frame>()
                    target?.style?.left = "${frameValue.x}px"
                    target?.style?.top = "${frameValue.y}px"
                    target?.style?.width = "${frameValue.width}px"
                    target?.style?.height = "${frameValue.height}px"
                }

                else -> {}
            }
        }
    }

    /**
     * Get web transform content for element, return as map
     */
    private fun getCSSTransform(value: Any): JsMap<String, String> {
        val transformSpilt = value.unsafeCast<String>().split("|")
        // Get element's own width and height (if parameter is %, convert to px)
        val width = target?.style?.width?.removeSuffix("px")?.toDouble() ?: 0.0
        val height = target?.style?.height?.removeSuffix("px")?.toDouble() ?: 0.0

        val anchorSpilt = transformSpilt[3].split(" ")
        val anchorX = anchorSpilt[0].toPercentage()
        val anchorY = anchorSpilt[1].toPercentage()
        val transformOrigin = "$anchorX $anchorY 0"

        val translateSpilt = transformSpilt[2].split(" ")
        val translateX = translateSpilt[0].toFloat() * width
        val translateY = translateSpilt[1].toFloat() * height

        val rotate = transformSpilt[0]

        val scaleSpilt = transformSpilt[1].split(" ")
        val scaleX = scaleSpilt[0]
        val scaleY = scaleSpilt[1]

        val skewSplit = transformSpilt[4].split(" ")
        val skewX = skewSplit[0]
        val skewY = skewSplit[1]

        return JsMap<String, String>().apply {
            this["transformOrigin"] = transformOrigin

            this["translateX"] = translateX.toString()
            this["translateY"] = translateY.toString()

            this["rotate"] = rotate

            this["scaleX"] = scaleX
            this["scaleY"] = scaleY

            this["skewX"] = skewX
            this["skewY"] = skewY

            this["transform"] =
                "translate(${
                    translateX
                }px, ${
                    translateY
                }px) rotate(${
                    rotate
                }deg) scale($scaleX, $scaleY) skew(${
                    skewX
                }deg, ${
                    skewY
                }deg)"
        }
    }

    /**
     * Get mini program API and parameter values corresponding to current animationValue
     */
    private fun getAPIAnimationValue(
        animationType: String,
        newAnimationValue: Any
    ): MutableMap<String, String> {
        val operatesMap = mutableMapOf<String, String>()
        when (animationType) {
            KRCssConst.TRANSFORM -> {
                // Get each parameter
                val transformValues = getCSSTransform(newAnimationValue)
                transformValues.let {
                    operatesMap["translateX"] = it["translateX"] ?: "0"
                    operatesMap["translateY"] = it["translateY"] ?: "0"
                    operatesMap["rotate"] = it["rotate"] ?: "0"
                    operatesMap["scaleX"] = it["scaleX"] ?: "1.0"
                    operatesMap["scaleY"] = it["scaleY"] ?: "1.0"

                    // current step animation transformOrigin
                    operatesMap["transformOrigin"] = it["transformOrigin"] ?: "50% 50% 0"
                }
            }

            KRCssConst.OPACITY -> {
                operatesMap["opacity"] = newAnimationValue.unsafeCast<Number>().toString()
            }

            KRCssConst.BACKGROUND_COLOR -> {
                operatesMap["backgroundColor"] = newAnimationValue.unsafeCast<String>().toRgbColor()
            }

            KRCssConst.FRAME -> {
                val frame = newAnimationValue.unsafeCast<Frame>()
                operatesMap["left"] = frame.x.toString()
                operatesMap["top"] = frame.y.toString()
                operatesMap["width"] = frame.width.toString()
                operatesMap["height"] = frame.height.toString()
            }
        }
        return operatesMap
    }


    /**
     * Run each handler of the animation
     */
    private fun applyAnimationHandler(
        target: HTMLElement,
        property: String,
        operateValueMap: Map<String, String>
    ) {
        when (property) {
            KRCssConst.TRANSFORM -> {
                val translateX = operateValueMap["translateX"] ?: "0"
                val translateY = operateValueMap["translateY"] ?: "0"
                val rotate = operateValueMap["rotate"] ?: "0"
                val scaleX = operateValueMap["scaleX"] ?: "1.0"
                val scaleY = operateValueMap["scaleY"] ?: "1.0"
                val skewX = operateValueMap["skewX"] ?: "0"
                val skewY = operateValueMap["skewY"] ?: "0"

                // set transform values
                target.kuiklyAnimation?.translate(translateX, translateY)
                target.kuiklyAnimation?.rotate(rotate)
                target.kuiklyAnimation?.scale(scaleX, scaleY)
                target.kuiklyAnimation?.skew(skewX, skewY)
            }

            KRCssConst.OPACITY -> {
                target.kuiklyAnimation?.opacity(operateValueMap["opacity"]!!)
            }

            KRCssConst.BACKGROUND_COLOR -> {
                target.kuiklyAnimation?.backgroundColor(operateValueMap["backgroundColor"]!!)
            }

            KRCssConst.FRAME -> {
                target.kuiklyAnimation?.left(operateValueMap["left"]!!)
                target.kuiklyAnimation?.top(operateValueMap["top"]!!)
                target.kuiklyAnimation?.width(operateValueMap["width"]!!)
                target.kuiklyAnimation?.height(operateValueMap["height"]!!)
            }
        }
    }
}
