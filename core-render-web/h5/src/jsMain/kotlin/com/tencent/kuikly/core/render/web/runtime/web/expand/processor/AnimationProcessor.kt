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
 * H5 下的 styleSheet 操作类
 */
class StyleSheet {
    private var style: HTMLStyleElement? = null
    private var sheet: CSSStyleSheet? = null

    init {
        this.style = kuiklyDocument.createElement(ElementType.STYLE).unsafeCast<HTMLStyleElement>()
    }

    /**
     * 添加 style 标签
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

    // 添加样式命令
    fun add(cssText: String, index: Int = 0) {
        if (this.sheet == null) {
            // $style 未插入到 DOM，则先进行插入
            this.appendStyleSheet()
        }
        // 插入实际的动画样式内容
        this.sheet?.insertRule(cssText, index)
    }
}


/**
 * h5 style operator
 */
object H5StyleSheet {
    // 处理样式表样式的插入删除等
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
    // 属性组合
    private val rules: JsArray<Pair<String, String>> = JsArray()
    // transform 对象
    private val transforms: JsArray<Pair<String, String>> = JsArray()
    // 组合动画
    private val steps: JsArray<String> = JsArray()
    // 组合动画的过度类型
    private val transitionSteps: JsArray<String> = JsArray()
    // animationMap 的长度
    private var animationMapCount = 0
    // 动画id
    private var id = ++animationId

    override val delay = options.delay
    override val transformOrigin = options.transformOrigin
    override val duration = options.duration
    override val timingFunction = options.timingFunction

    /**
     * 创建实际底层动画数据
     */
    override fun export(ele: HTMLElement?): dynamic = getAnimationJson()

    /**
     * 动画关键帧载入
     */
    override fun step(options: Json): IAnimation {
        if (this.rules.isEmpty() && this.transforms.isEmpty()) {
            return this
        }

        // 动画类型参数
        val transformOrigin = options["transformOrigin"] ?: "50% 50% 0"
        val delay = options["delay"] ?: "0"
        val duration = options["duration"] ?: "0"
        val timingFunction = options["timingFunction"] ?: "linear"


        val transforms: JsArray<String> = JsArray()
        // 得到要执行的 transform 动画属性列表
        this.transforms.forEach { transform ->
            // 查找历史记录中是否已有当前类型的动画
            // 插入要执行的transform的动画属性
            transforms.push(transform.second)
        }
        // 按顺序执行
        val transformSequence = if (transforms.length > 0)
            "transform:${transforms.join(" ")}!important"
        else ""

        if (transformSequence != "") {
            // 有 transform 动画
            this.steps.push(transformSequence)
            this.steps.push(("transform-origin: $transformOrigin"))
            // 插入动画类型
            this.transitionSteps.push("transform ${duration}s $timingFunction ${delay}s")
        }

        // 得到要执行的全部规则动画
        this.rules.forEach { rule ->
            // 插入要执行的规则动画的动画属性
            this.steps.push("${rule.second}!important")
            // 插入动画类型
            this.transitionSteps.push("${rule.first} ${duration}s $timingFunction ${delay}s")
        }

        // 清空 rules 和 transforms
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
     * 获取实际要给 dom 使用的数据
     */
    private fun getAnimationJson(): dynamic {
        // 创建本次动画执行的索引
        val animIndex = "kuikly-animation_${this.id}_create-animation__${this.animationMapCount++}"
        val selector = "[animation=\"${animIndex}\"]"
        // 吐出 step，kuikly 单元素动画统一处理，不分步骤，先设置要指定的动作列表
        val stepList = "transition: ${this.transitionSteps.join(",")};"
        // 再设置要指定的动画属性列表
        val animationList = this.steps.join(";")
        // 往 stylesheet 中插入动画的选择器和动画的内容
        H5StyleSheet.styleSheet.add("$selector { $stepList$animationList }")
        // 清空 steps
        this.steps.clear()
        this.transitionSteps.clear()
        // 返回动画数据
        return animIndex
    }

    companion object {
        // 全局动画id
        private var animationId = 0
    }
}
