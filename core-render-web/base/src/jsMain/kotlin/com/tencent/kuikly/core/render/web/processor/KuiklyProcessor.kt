package com.tencent.kuikly.core.render.web.processor

/**
 * kuikly processor
 */
object KuiklyProcessor {
    /**
     * web Animation processor
     */
    lateinit var animationProcessor: IAnimationProcessor

    // event process for different host, like web, mini app, electron. some event is not
    // supported by some host. so we need to process it by different host.
    lateinit var eventProcessor: IEventProcessor

    // image process for different host, like web, mini app, electron. some image props implement different.
    // so we need to process it by different host.
    lateinit var imageProcessor: IImageProcessor

    // list processor, used to create list view for different host
    lateinit var listProcessor: IListProcessor

    // real text process object, assigned in web render
    lateinit var richTextProcessor: IRichTextProcessor

    // isDev mode
    var isDev: Boolean = false
}