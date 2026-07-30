package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewContainer

internal class MyView: DeclarativeBaseView<MyViewAttr, MyViewEvent>() {
    
    override fun createEvent(): MyViewEvent {
        return MyViewEvent()
    }

    override fun createAttr(): MyViewAttr {
        return MyViewAttr().apply {
            overflow(true)
        }
    }

    override fun viewName(): String {
        return "KRMyView"
    }
}

internal class MyViewAttr : ComposeAttr() {
    fun message(msg: String): MyViewAttr {
        "message" with msg
        return this
    }

}

internal class MyViewEvent : ComposeEvent() {
    
}

internal fun ViewContainer<*, *>.My(init: MyView.() -> Unit) {
    addChild(MyView(), init)
}
