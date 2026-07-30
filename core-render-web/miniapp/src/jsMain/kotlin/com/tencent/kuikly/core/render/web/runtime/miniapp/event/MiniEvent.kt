package com.tencent.kuikly.core.render.web.runtime.miniapp.event

class MiniEvent(private val mpEvent: dynamic) {
    @JsName("type")
    val type: String
        get() = mpEvent.type.toLocaleLowerCase().unsafeCast<String>()

    @JsName("offsetX")
    val offsetX: Double
        get() = mpEvent.detail.x.unsafeCast<Double>()

    @JsName("offsetY")
    val offsetY: Double
        get() = mpEvent.detail.y.unsafeCast<Double>()

    @JsName("touches")
    val touches: dynamic
        get() = mpEvent.touches

    @JsName("changedTouches")
    val changedTouches: dynamic
        get() = mpEvent.changedTouches

    @JsName("currentTarget")
    val currentTarget: dynamic
        get() = mpEvent.currentTarget

    @JsName("target")
    val target: dynamic
        get() = mpEvent.detail

    @JsName("detail")
    val detail: dynamic
        get() = mpEvent.detail

    @JsName("stopPropagation")
    fun stopPropagation() {

    }

    @JsName("timeStamp")
    val timeStamp: dynamic
        get() = mpEvent.timeStamp
}