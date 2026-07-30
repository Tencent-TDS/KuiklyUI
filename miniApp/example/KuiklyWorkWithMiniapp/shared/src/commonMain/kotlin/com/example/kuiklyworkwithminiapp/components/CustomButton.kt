package com.example.kuiklyworkwithminiapp.components

import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TextAttr

internal class CustomButtonView: ComposeView<CustomButtonAttr, CustomButtonEvent>() {

    override fun createEvent(): CustomButtonEvent {
        return CustomButtonEvent()
    }

    override fun createAttr(): CustomButtonAttr {
        return CustomButtonAttr().apply {
            overflow(true)
        }
    }

    override fun viewName(): String {
        val pageData = getPager().pageData
        if (pageData.params.optString("is_miniprogram") == "1") {
            return "KRCustomButtonView"
        }
        return ViewConst.TYPE_VIEW
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                justifyContentCenter()
                alignItemsCenter()
            }
            // 文本
            ctx.attr.titleAttrInit?.also { textAttr ->
                Text {
                    attr (textAttr)
                }
            }
        }
    }
}
internal class CustomButtonAttr : ComposeAttr() {
    internal var titleAttrInit: (TextAttr.()->Unit)? = null
    fun titleAttr(init: TextAttr.()->Unit) {
        titleAttrInit = init
    }
    fun openType(type: String): CustomButtonAttr {
        "openType" with type
        return this
    }
}

internal class CustomButtonEvent : ComposeEvent() {
    fun onGetPhoneNumber(handler: (phoneNumberData: JSONObject) -> Unit) {
        this.register(GET_PHONE_NUMBER_CALLBACK) {
            handler(it as JSONObject)
        }
    }
    companion object {
        const val GET_PHONE_NUMBER_CALLBACK = "getPhoneNumberCallback"
    }
}

internal fun ViewContainer<*, *>.CustomButton(init: CustomButtonView.() -> Unit) {
    addChild(CustomButtonView(), init)
}