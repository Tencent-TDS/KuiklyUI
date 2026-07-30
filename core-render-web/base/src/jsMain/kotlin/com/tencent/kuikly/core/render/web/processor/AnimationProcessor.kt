package com.tencent.kuikly.core.render.web.processor

import org.w3c.dom.HTMLElement
import kotlin.js.Json

/**
 * web animation timing function, for spring animation, use cubic-bezier to simulate
 */
enum class AnimationTimingFunction(val value: String) {
    LINEAR("linear"),
    EASE_IN("ease-in"),
    EASE_OUT("ease-out"),
    EASE_IN_OUT("ease-in-out"),
    SIMULATE_SPRING_ANIMATION("cubic-bezier(0.68, -0.55, 0.27, 1.55)"),
}

/**
 * web animation interface
 */
interface IAnimation {
    // animation duration
    val duration: Number
    // animation timing function
    val timingFunction: String
    // animation delay time
    val delay: Number
    // animation transform origin position
    val transformOrigin: String

    /**
     * Export animation queue, before export, all animation will be cleared
     */
    fun export(ele: HTMLElement? = null): dynamic

    /**
     * step animation
     */
    fun step(options: Json): IAnimation

    /**
     * set rotate value
     */
    fun rotate(angle: String): IAnimation

    /**
     * set skew value
     */
    fun skew(skewX: String, skewY: String): IAnimation

    /**
     * set scale value
     */
    fun scale(scaleX: String, scaleY: String): IAnimation

    /**
     * set translate value
     */
    fun translate(translateX: String, translateY: String): IAnimation

    /**
     * set opacity value
     */
    fun opacity(opacity: String): IAnimation

    /**
     * set background color value
     */
    fun backgroundColor(value: String): IAnimation

    /**
     * set width value
     */
    fun width(value: String): IAnimation

    /**
     * set height value
     */
    fun height(value: String): IAnimation

    /**
     * set top value
     */
    fun top(value: String): IAnimation

    /**
     * set left value
     */
    fun left(value: String): IAnimation

    /**
     * set right value
     */
    fun right(value: String): IAnimation

    /**
     * set bottom value
     */
    fun bottom(value: String): IAnimation
}

/**
 * animation options
 */
data class AnimationOption(
    val duration: Number = 400,
    val timingFunction: String = AnimationTimingFunction.LINEAR.value,
    val delay: Number = 0,
    val transformOrigin: String = "50% 50% 0px",
)

/**
 * web animation creator interface
 */
interface IAnimationProcessor {
    /**
     * Create animation
     */
    fun createAnimation(options: AnimationOption): IAnimation
}