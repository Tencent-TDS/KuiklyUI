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

    // Whether to prevent the default browser context menu (contextmenu event).
    //
    // On PC the `contextmenu` event is fired on right-click (or Ctrl+Click on macOS);
    // on mobile browsers it is fired after a long-press (which also shows the native
    // "copy / save image" callout).
    //
    // Semantics:
    //   - true  (default): Prevent the browser context menu on both PC and mobile.
    //                      This keeps the long-press / pan gestures from being
    //                      interrupted by the browser's callout menu on mobile,
    //                      and prevents the default right-click menu on PC — which
    //                      is the safe default for pages that treat right-click /
    //                      long-press as an app-level gesture.
    //   - false          : Allow the browser context menu on both PC and mobile.
    //                      Use this when the business wants users to be able to
    //                      right-click (PC) or long-press (mobile, e.g. save image)
    //                      through the browser's default UI.
    //
    // NOTE: This flag only controls whether the render layer calls `event.preventDefault()`
    // on the `contextmenu` event that is registered internally by long-press / pan
    // gesture handlers. It does NOT affect any `contextmenu` listener that the business
    // adds on its own (business is free to build a custom right-click menu on top).
    var preventDefaultContextMenu: Boolean = true

    // Whether the WebRender layer should automatically forward the browser's
    // container / window resize to the Kuikly Pager as a `rootViewSizeDidChanged`
    // event so that Kuikly pages relayout responsively (typical desktop use case:
    // wide-screen / split-pane / collapsible-sidebar layouts).
    //
    // Semantics:
    //   - false (default): WebRender never automatically forwards resize.
    //                      This is the default on both PC and mobile because
    //                      auto relayout on every resize can have wide-ranging
    //                      impact (e.g. mobile soft-keyboard-triggered resize
    //                      causing unwanted reflow, or desktop layouts that
    //                      do not want the Kuikly page to rescale). Business
    //                      code can still trigger a relayout imperatively via
    //                      [KuiklyView.updateRootViewSize] whenever needed.
    //   - true           : Enable auto forwarding on both PC and mobile.
    //                      Use with care on mobile: the soft keyboard will
    //                      also fire resize.
    //
    // Notes:
    // - When enabled, the WebRender observes both `ResizeObserver` on the root
    //   container (if available) and `window.resize` as fallback, throttled at
    //   100 ms; it dispatches `rootViewSizeDidChanged` only when the size
    //   actually changes.
    // - Business code that needs pixel-perfect control should keep this switch
    //   off (default) and call [KuiklyView.updateRootViewSize] itself.
    var autoUpdateRootViewSizeOnResize: Boolean = false
}