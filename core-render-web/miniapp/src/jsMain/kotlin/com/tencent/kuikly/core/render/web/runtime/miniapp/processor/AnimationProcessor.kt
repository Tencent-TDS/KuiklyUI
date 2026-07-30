package com.tencent.kuikly.core.render.web.runtime.miniapp.processor

import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.clear
import com.tencent.kuikly.core.render.web.collection.array.isEmpty
import com.tencent.kuikly.core.render.web.ktx.pxToFloat
import com.tencent.kuikly.core.render.web.processor.AnimationOption
import com.tencent.kuikly.core.render.web.processor.IAnimation
import com.tencent.kuikly.core.render.web.processor.IAnimationProcessor
import com.tencent.kuikly.core.render.web.runtime.miniapp.MiniGlobal
import org.w3c.dom.HTMLElement
import kotlin.js.Json
import kotlin.js.json

/**
 * web render real animation generator
 */
object AnimationProcessor : IAnimationProcessor {
    /**
     * return web render real animation instance
     */
    override fun createAnimation(options: AnimationOption): IAnimation = MiniAppAnimation(options)
}

/**
 * Mini App render real animation
 */
class MiniAppAnimation(options: AnimationOption) : IAnimation {
    // 属性组合
    private val rules: JsArray<Pair<String, String>> = JsArray()
    // transform 对象
    private val transforms: JsArray<Pair<String, String>> = JsArray()
    // 组合动画
    private val steps: JsArray<Pair<String, String>> = JsArray()
    // 组合动画的过度类型
    private val transitionSteps: JsArray<String> = JsArray()

    override val delay = options.delay
    override val transformOrigin = options.transformOrigin
    override val duration = options.duration
    override val timingFunction = options.timingFunction

    /**
     * create real animation data
     */
    override fun export(ele: HTMLElement?): dynamic = getAnimationJson(ele)

    /**
     * generate animation keyframes
     */
    override fun step(options: Json): IAnimation {
        if (this.rules.isEmpty() && this.transforms.isEmpty()) {
            return this
        }

        // animation params
        val transformOrigin = options["transformOrigin"] ?: "50% 50% 0"
        val delay = options["delay"] ?: "0"
        val duration = options["duration"] ?: "0"
        val timingFunction = options["timingFunction"] ?: "linear"


        val transforms: JsArray<String> = JsArray()
        // generate transform list to execute
        this.transforms.forEach { transform ->
            // insert to transform list
            transforms.push(transform.second)
        }
        // execute by order
        val transformSequence = if (transforms.length > 0)
            transforms.join(" ")
        else ""

        if (transformSequence != "") {
            this.steps.push(Pair("transform", transformSequence))
            this.steps.push(Pair("transformOrigin", "$transformOrigin"))
            // insert transition
            this.transitionSteps.push("transform ${duration}s $timingFunction ${delay}s")
        }

        // generate all rule animation
        this.rules.forEach { rule ->
            this.steps.push(rule)
            this.transitionSteps.push("${rule.first} ${duration}s $timingFunction ${delay}s")
        }

        // clear rules & transforms
        this.rules.clear()
        this.transforms.clear()

        return this
    }

    override fun rotate(angle: String): IAnimation {
        this.transforms.push(Pair("rotate", "rotate(${angle}deg)"))
        return this
    }

    override fun skew(skewX: String, skewY: String): IAnimation {
        this.transforms.push(Pair("skew", "skew(${skewX}deg, ${skewY}deg)"))
        return this
    }

    override fun scale(scaleX: String, scaleY: String): IAnimation {
        this.transforms.push(Pair("scale", "scale(${scaleX}, ${scaleY})"))
        return this
    }

    override fun translate(translateX: String, translateY: String): IAnimation {
        this.transforms.push(
            Pair("translate", "translate(${translateX}px, ${translateY}px)")
        )
        return this
    }

    override fun opacity(opacity: String): IAnimation {
        // add opacity animation value
        this.rules.push(Pair("opacity", "opacity: $opacity"))
        return this
    }

    override fun backgroundColor(value: String): IAnimation {
        // add background color animation value
        this.rules.push(Pair("background-color", value))
        return this
    }

    override fun width(value: String): IAnimation {
        this.rules.push(Pair("width", "${value}px"))
        return this
    }

    override fun height(value: String): IAnimation {
        this.rules.push(Pair("height", "${value}px"))
        return this
    }

    override fun top(value: String): IAnimation {
        this.rules.push(Pair("top", "${value}px"))
        return this
    }

    override fun left(value: String): IAnimation {
        this.rules.push(Pair("left", "${value}px"))
        return this
    }

    override fun right(value: String): IAnimation {
        this.rules.push(Pair("right", "${value}px"))
        return this
    }

    override fun bottom(value: String): IAnimation {
        this.rules.push(Pair("bottom", "${value}px"))
        return this
    }

    /**
     * execute animation
     */
    private fun executeAnimation(ele: HTMLElement) {
        val dynamicElement = ele.asDynamic()
        val animationData = json()
        val rules = json()
        val dynamicStyle = ele.style.asDynamic()
        // first. set transition
        val execTransitionSteps = dynamicElement.execTransitionSteps.unsafeCast<JsArray<String>>()
        ele.style.transition = execTransitionSteps.join(",")
        // save transition
        animationData["transition"] = ele.style.transition
        // then. set animation attributes
        val execAnimationRules = dynamicElement.execAnimationRules.unsafeCast<JsArray<Pair<String, String>>>()
        execAnimationRules.forEach { step ->
            val key = if (step.first == "background-color")  {
                "backgroundColor"
            } else {
                step.first
            }
            // save old value and new value
            rules[key] = json(
                "oldValue" to dynamicStyle[key],
                "newValue" to step.second
            )
            // set new value
            ele.style.setProperty(key, step.second)
            // rawLeft,rawTop should update, otherwise the animation will not work
            if (step.first == "left") {
                ele.asDynamic().rawLeft = step.second.pxToFloat()
            }
            if (step.first == "top") {
                ele.asDynamic().rawTop = step.second.pxToFloat()
            }
        }
        // save animation rules
        animationData["rules"] = rules
        // save animation data
        ele.asDynamic().animationData = animationData

        // clear transition
        dynamicElement.execTransitionSteps = undefined
        // clear animation rules
        dynamicElement.execAnimationRules = undefined
        // clear animation timeout id
        dynamicElement.executeAnimationId = 0
    }

    /**
     * generate animation json data for dom element to execute
     */
    private fun getAnimationJson(ele: HTMLElement?): dynamic {
        if (ele != null) {
            val dynamicElement = ele.asDynamic()

            // clear animation un executed
            if (dynamicElement.executeAnimationId != 0) {
                MiniGlobal.clearTimeout(dynamicElement.executeAnimationId.unsafeCast<Int>())
            }

            // generate transition steps
            val execTransitionSteps: JsArray<String> =
                if (jsTypeOf(dynamicElement.execTransitionSteps) != "undefined") {
                // use existing steps if exists
                dynamicElement.execTransitionSteps.unsafeCast<JsArray<String>>()
            } else {
                JsArray()
            }
            // should execute rules list
            val execAnimationRules: JsArray<Pair<String, String>> = if (jsTypeOf(dynamicElement.execAnimationRules) != "undefined") {
                dynamicElement.execAnimationRules.unsafeCast<JsArray<Pair<String, String>>>()
            } else {
                JsArray()
            }

            // save transition steps list
            this.transitionSteps.forEach { transition ->
                execTransitionSteps.push(transition)
            }
            dynamicElement.execTransitionSteps = execTransitionSteps
            // save animation rules
            this.steps.forEach { step ->
                execAnimationRules.push(step)
            }
            dynamicElement.execAnimationRules = execAnimationRules
            // delay 10ms to execute animation to avoid flicker
            dynamicElement.executeAnimationId = MiniGlobal.setTimeout({
                executeAnimation(ele)
            }, 10)
        }

        // clear steps
        this.steps.clear()
        this.transitionSteps.clear()
        // mini app do not return animation index
        return ""
    }
}
