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

    // Whether to prevent default text selection and image drag behavior.
    // When set to true, both text selection (selectstart) and image drag (dragstart) events
    // will be prevented. Default is true to maintain backward compatibility.
    //
    // NOTE: This is the legacy combined switch. It is kept as the default value source for
    // [preventDefaultSelect] and [preventDefaultDrag] so existing user code that only sets
    // this flag still works. For finer control, prefer setting [preventDefaultSelect] and
    // [preventDefaultDrag] independently.
    var preventDefaultDragAndSelect: Boolean = true
        set(value) {
            field = value
            // Sync the two fine-grained switches so legacy code keeps working as before.
            preventDefaultSelect = value
            preventDefaultDrag = value
        }

    // Whether to prevent the default text selection (selectstart) behavior.
    // Set this to false alone if you want users to be able to select / copy text on H5,
    // while still keeping native image drag prevented (which avoids the PageList drag
    // residue issue caused by HTML5 native drag swallowing mouseup).
    var preventDefaultSelect: Boolean = true

    // Whether to prevent the default image drag (dragstart) behavior.
    // Keep this as true (default) to avoid the browser entering native HTML5 drag mode,
    // because once a native drag starts the browser stops dispatching mousemove / mouseup
    // and the list scroll state machine will be stuck until the next click.
    var preventDefaultDrag: Boolean = true
}