package com.tencent.kuikly.core.render.web.css.animation

import org.w3c.dom.HTMLElement

/**
 * Animation logic processor base class, currently animation is divided into two types
 * 1.Elastic animation, corresponding processor base class: [KRCSSSpringAnimationHandler
 * 2.Attribute animation, corresponding processor base class: [KRCSSPlainAnimationHandler]
 */
abstract class KRCSSAnimationHandler {
    var target: HTMLElement? = null
    var finalValue: Any? = null
    var delay: Float = 0f
    var propKey = ""
    var forceNotCancel = false

    /**
     * Start animation
     * @param target View to which the animation is applied
     * @param forceCommit Whether to force commit, if true, the animation props will be set to animation again
     * @param onAnimationEndBlock Animation end callback block
     */
    abstract fun start(
        target: HTMLElement,
        forceCommit: Boolean,
        onAnimationEndBlock: (finished: Boolean, propKey: String) -> Unit
    )

    /**
     * Cancel animation
     */
    abstract fun cancel(propKey: String? = null)

    /**
     * Animation end callback
     */
    abstract fun end(cancel: Boolean = false)

    /**
     * Remove animation listener
     */
    abstract fun removeListener()


    protected fun getFinishValue(isCancel: Boolean): Boolean {
        return if (forceNotCancel) {
            true
        } else {
            !isCancel
        }
    }
}