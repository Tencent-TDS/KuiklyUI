package com.tencent.kuikly.core.render.web.runtime.web.expand.processor

import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.clear
import com.tencent.kuikly.core.render.web.collection.array.isEmpty
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.processor.AnimationOption
import com.tencent.kuikly.core.render.web.processor.IAnimation
import com.tencent.kuikly.core.render.web.processor.IAnimationProcessor
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLStyleElement
import org.w3c.dom.css.CSSStyleSheet
import org.w3c.dom.get
import kotlin.js.Json

/**
 * h5 style sheet class
 */
class StyleSheet {
    private var style: HTMLStyleElement? = null
    private var sheet: CSSStyleSheet? = null

    init {
        this.style = kuiklyDocument.createElement(ElementType.STYLE).unsafeCast<HTMLStyleElement>()
    }

    /**
     * append style sheet to dom
     */
    private fun appendStyleSheet() {
        val style = this.style
        if (style != null) {
            val head = kuiklyDocument.getElementsByTagName("head")[0]
            style.setAttribute("type", "text/css")
            style.setAttribute("data-type", "kuikly")
            head?.appendChild(style)
            this.sheet = style.sheet.unsafeCast<CSSStyleSheet>()
        }
    }

    // append css text to style sheet
    fun add(cssText: String, index: Int = 0) {
        if (this.sheet == null) {
            // insert style sheet first when not inserted
            this.appendStyleSheet()
        }
        // insert real animation css text
        this.sheet?.insertRule(cssText, index)
    }
}


/**
 * h5 style operator
 */
object H5StyleSheet {
    // style sheet
    val styleSheet = StyleSheet()
}

/**
 * web render real animation generator
 */
object AnimationProcessor : IAnimationProcessor {
    /**
     * return web render real animation instance
     */
    override fun createAnimation(options: AnimationOption): IAnimation = H5Animation(options)
}

/**
 * H5 render real animation
 */
class H5Animation(options: AnimationOption) : IAnimation {
    // css rules list
    private val rules: JsArray<Pair<String, String>> = JsArray()
    // transform list
    private val transforms: JsArray<Pair<String, String>> = JsArray()
    // animation steps list
    private val steps: JsArray<String> = JsArray()
    // animation transition steps list
    private val transitionSteps: JsArray<String> = JsArray()
    // animationMap count
    private var animationMapCount = 0
    // animation id
    private var id = ++animationId

    override val delay = options.delay
    override val transformOrigin = options.transformOrigin
    override val duration = options.duration
    override val timingFunction = options.timingFunction

    /**
     * create real animation json
     */
    override fun export(ele: HTMLElement?): dynamic = getAnimationJson()

    /**
     * animation keyframe generate
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
        // insert transform animation properties list
        this.transforms.forEach { transform ->
            transforms.push(transform.second)
        }
        // execute transform animation by order
        val transformSequence = if (transforms.length > 0)
            "transform:${transforms.join(" ")}!important"
        else ""

        if (transformSequence != "") {
            // insert transform properties
            this.steps.push(transformSequence)
            this.steps.push(("transform-origin: $transformOrigin"))
            // insert transition properties
            this.transitionSteps.push("transform ${duration}s $timingFunction ${delay}s")
        }

        // generate all animation rules
        this.rules.forEach { rule ->
            // insert rules
            this.steps.push("${rule.second}!important")
            // insert transitions
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
        this.rules.push(Pair("background-color", "background-color: $value"))
        return this
    }

    override fun width(value: String): IAnimation {
        this.rules.push(Pair("width", "width: ${value}px"))
        return this
    }

    override fun height(value: String): IAnimation {
        this.rules.push(Pair("height", "height: ${value}px"))
        return this
    }

    override fun top(value: String): IAnimation {
        this.rules.push(Pair("top", "top: ${value}px"))
        return this
    }

    override fun left(value: String): IAnimation {
        this.rules.push(Pair("left", "left: ${value}px"))
        return this
    }

    override fun right(value: String): IAnimation {
        this.rules.push(Pair("right", "right: ${value}px"))
        return this
    }

    override fun bottom(value: String): IAnimation {
        this.rules.push(Pair("bottom", "bottom: ${value}px"))
        return this
    }

    /**
     * get real animation json
     */
    private fun getAnimationJson(): dynamic {
        // create animation index
        val animIndex = "kuikly-animation_${this.id}_create-animation__${this.animationMapCount++}"
        val selector = "[animation=\"${animIndex}\"]"
        // generate steps, kuikly single element animation unified processing, no steps
        val stepList = "transition: ${this.transitionSteps.join(",")};"
        // set animation properties list
        val animationList = this.steps.join(";")
        // insert animation selector and animation content to stylesheet
        H5StyleSheet.styleSheet.add("$selector { $stepList$animationList }")
        // clear steps & transition steps
        this.steps.clear()
        this.transitionSteps.clear()
        // return animation index
        return animIndex
    }

    companion object {
        // global animation id
        private var animationId = 0
    }
}
