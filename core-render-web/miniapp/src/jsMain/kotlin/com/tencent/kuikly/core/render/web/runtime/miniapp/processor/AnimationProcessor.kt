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
     * 创建实际底层动画数据
     */
    override fun export(ele: HTMLElement?): dynamic = getAnimationJson(ele)

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
            transforms.join(" ")
        else ""

        if (transformSequence != "") {
            // 有 transform 动画
            this.steps.push(Pair("transform", transformSequence))
            // transform origin
            this.steps.push(Pair("transformOrigin", "$transformOrigin"))
            // 插入动画类型
            this.transitionSteps.push("transform ${duration}s $timingFunction ${delay}s")
        }

        // 得到要执行的全部规则动画
        this.rules.forEach { rule ->
            // 插入要执行的规则动画的动画属性
            this.steps.push(rule)
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
     * 实际执行动画
     */
    private fun executeAnimation(ele: HTMLElement) {
        // dynamic 元素
        val dynamicElement = ele.asDynamic()
        // 动画数据
        val animationData = json()
        // 动画属性数据
        val rules = json()
        // dynamic 样式属性
        val dynamicStyle = ele.style.asDynamic()
        // 首先设置transition
        val execTransitionSteps = dynamicElement.execTransitionSteps.unsafeCast<JsArray<String>>()
        ele.style.transition = execTransitionSteps.join(",")
        // 保存 transition
        animationData["transition"] = ele.style.transition
        // 再设置动画属性
        val execAnimationRules = dynamicElement.execAnimationRules.unsafeCast<JsArray<Pair<String, String>>>()
        execAnimationRules.forEach { step ->
            val key = if (step.first == "background-color")  {
                "backgroundColor"
            } else {
                step.first
            }
            // 保存原值和新值
            rules[key] = json(
                "oldValue" to dynamicStyle[key],
                "newValue" to step.second
            )
            // 设置新值
            ele.style.setProperty(key, step.second)
            // rawLeft,rawTop 也需要更新，不然计算会有问题
            if (step.first == "left") {
                ele.asDynamic().rawLeft = step.second.pxToFloat()
            }
            if (step.first == "top") {
                ele.asDynamic().rawTop = step.second.pxToFloat()
            }
        }
        // 保存属性值
        animationData["rules"] = rules
        // 保存动画数据
        ele.asDynamic().animationData = animationData

        // 清除形变属性
        dynamicElement.execTransitionSteps = undefined
        // 清除动画属性
        dynamicElement.execAnimationRules = undefined
        // 清除动画延迟 id
        dynamicElement.executeAnimationId = 0
    }

    /**
     * 获取实际要给 dom 使用的数据
     */
    private fun getAnimationJson(ele: HTMLElement?): dynamic {
        // 有传入
        if (ele != null) {
            val dynamicElement = ele.asDynamic()

            // 如果当前有动画尚未执行，则先清除
            if (dynamicElement.executeAnimationId != 0) {
                MiniGlobal.clearTimeout(dynamicElement.executeAnimationId.unsafeCast<Int>())
            }

            // 真正要执行的过渡动画列表
            val execTransitionSteps: JsArray<String> =
                if (jsTypeOf(dynamicElement.execTransitionSteps) != "undefined") {
                // 如果当前元素已经有则使用已有的
                dynamicElement.execTransitionSteps.unsafeCast<JsArray<String>>()
            } else {
                JsArray()
            }
            // 真正要执行的属性列表
            val execAnimationRules: JsArray<Pair<String, String>> =
                if (jsTypeOf(dynamicElement.execAnimationRules) != "undefined") {
                // 如果当前元素已经有则使用已有的
                dynamicElement.execAnimationRules.unsafeCast<JsArray<Pair<String, String>>>()
            } else {
                JsArray()
            }

            // 保存当前所有形变
            this.transitionSteps.forEach { transition ->
                execTransitionSteps.push(transition)
            }
            dynamicElement.execTransitionSteps = execTransitionSteps
            // 保存当前所有动画属性
            this.steps.forEach { step ->
                execAnimationRules.push(step)
            }
            dynamicElement.execAnimationRules = execAnimationRules
            // 延迟执行动画
            dynamicElement.executeAnimationId = MiniGlobal.setTimeout({
                executeAnimation(ele)
            }, 10)
        }

        // 清空 steps
        this.steps.clear()
        this.transitionSteps.clear()
        // 返回动画数据
        return ""
    }
}
