package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.ClickParams
import com.tencent.kuikly.core.base.event.EventName
import com.tencent.kuikly.core.views.internal.GroupAttr
import com.tencent.kuikly.core.views.internal.GroupEvent
import com.tencent.kuikly.core.views.internal.GroupView

internal class MyDemoCustomView: GroupView<MyDemoCustomViewAttr, MyDemoCustomViewEvent>() {
    
    override fun createEvent(): MyDemoCustomViewEvent {
        return MyDemoCustomViewEvent()
    }

    override fun createAttr(): MyDemoCustomViewAttr {
        return MyDemoCustomViewAttr().apply {
            overflow(true)
        }
    }

    override fun viewName(): String {
        return "KRMyDemoCustomView"
    }
}

internal class MyDemoCustomViewAttr : GroupAttr() {
    fun message(msg: String): MyDemoCustomViewAttr {
        "message" with msg
        return this
    }

}

internal class MyDemoCustomViewEvent : GroupEvent() {
    fun onMyViewTapped(handler: (Any?) -> Unit) {
        this.register("onMyViewTapped") {
            handler(it)
        }
    }
}

internal fun ViewContainer<*, *>.MyDemoCustom(init: MyDemoCustomView.() -> Unit) {
    addChild(MyDemoCustomView(), init)
}
