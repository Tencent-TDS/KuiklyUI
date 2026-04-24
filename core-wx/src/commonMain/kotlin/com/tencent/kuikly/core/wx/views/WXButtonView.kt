/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.core.wx.views

import com.tencent.kuikly.core.base.ComposeAttr
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ComposeView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TextAttr

/**
 * WeChat mini-program native `button` component open-type values.
 */
object WXButtonOpenType {
    const val CONTACT = "contact"
    const val SHARE = "share"
    const val GET_PHONE_NUMBER = "getPhoneNumber"
    const val GET_USER_INFO = "getUserInfo"
    const val LAUNCH_APP = "launchApp"
    const val OPEN_SETTING = "openSetting"
    const val FEEDBACK = "feedback"
    const val CHOOSE_AVATAR = "chooseAvatar"
}

/**
 * WeChat mini-program native `button` component form-type values.
 */
object WXButtonFormType {
    const val SUBMIT = "submit"
    const val RESET = "reset"
}

/**
 * Cross-platform Compose-style wrapper of the WeChat mini-program native `button` component.
 *
 * On mini-program platform (when `pageData.params.is_miniprogram == "1"`), it renders the
 * native `<button/>` (via `KRWXButtonView`).
 * On other platforms it falls back to a plain view so open-type native capabilities are
 * silently ignored.
 */
class WXButtonView : ComposeView<WXButtonAttr, WXButtonEvent>() {

    override fun createEvent(): WXButtonEvent {
        return WXButtonEvent()
    }

    override fun createAttr(): WXButtonAttr {
        return WXButtonAttr().apply {
            overflow(true)
        }
    }

    override fun viewName(): String {
        val pageData = getPager().pageData
        if (pageData.params.optString(IS_MINI_PROGRAM) == "1") {
            return VIEW_NAME_MINI_PROGRAM
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
            // Title text rendered as a child Text view
            ctx.attr.titleAttrInit?.also { textAttr ->
                Text {
                    attr(textAttr)
                }
            }
        }
    }

    companion object {
        internal const val IS_MINI_PROGRAM = "is_miniprogram"
        internal const val VIEW_NAME_MINI_PROGRAM = "KRWXButtonView"
    }
}

/**
 * Attributes for [WXButtonView].
 *
 * Mirrors the native WeChat mini-program `button` component attributes.
 */
class WXButtonAttr : ComposeAttr() {

    internal var titleAttrInit: (TextAttr.() -> Unit)? = null

    /**
     * Config the inner title text style.
     */
    fun titleAttr(init: TextAttr.() -> Unit) {
        titleAttrInit = init
    }

    /**
     * Button size, `default` / `mini`.
     */
    fun size(value: String): WXButtonAttr {
        PROP_SIZE with value
        return this
    }

    /**
     * Button visual type, `primary` / `default` / `warn`.
     */
    fun type(value: String): WXButtonAttr {
        PROP_TYPE with value
        return this
    }

    /**
     * Whether the button is hollow.
     */
    fun plain(value: Boolean): WXButtonAttr {
        PROP_PLAIN with value
        return this
    }

    /**
     * Whether the button is disabled.
     */
    fun disabled(value: Boolean): WXButtonAttr {
        PROP_DISABLED with value
        return this
    }

    /**
     * Whether to show the loading indicator before the button text.
     */
    fun loading(value: Boolean): WXButtonAttr {
        PROP_LOADING with value
        return this
    }

    /**
     * Form type used when the button is placed inside a `form`.
     * See [WXButtonFormType] for candidate values.
     */
    fun formType(value: String): WXButtonAttr {
        PROP_FORM_TYPE with value
        return this
    }

    /**
     * Mini-program open-ability. See [WXButtonOpenType] for candidate values.
     */
    fun openType(value: String): WXButtonAttr {
        PROP_OPEN_TYPE with value
        return this
    }

    /**
     * Button name used by the form.
     */
    fun name(value: String): WXButtonAttr {
        PROP_NAME with value
        return this
    }

    /**
     * Button language, `en` / `zh_CN` / `zh_TW`.
     */
    fun lang(value: String): WXButtonAttr {
        PROP_LANG with value
        return this
    }

    /**
     * Session source when `openType = "contact"`.
     */
    fun sessionFrom(value: String): WXButtonAttr {
        PROP_SESSION_FROM with value
        return this
    }

    /**
     * Parameters passed to the app when `openType = "launchApp"`.
     */
    fun appParameter(value: String): WXButtonAttr {
        PROP_APP_PARAMETER with value
        return this
    }

    /**
     * Whether to display the in-session message card when `openType = "contact"`.
     */
    fun showMessageCard(value: Boolean): WXButtonAttr {
        PROP_SHOW_MESSAGE_CARD with value
        return this
    }

    /**
     * Customer service message business id when `openType = "contact"`.
     */
    fun businessId(value: String): WXButtonAttr {
        PROP_BUSINESS_ID with value
        return this
    }

    companion object {
        internal const val PROP_SIZE = "size"
        internal const val PROP_TYPE = "type"
        internal const val PROP_PLAIN = "plain"
        internal const val PROP_DISABLED = "disabled"
        internal const val PROP_LOADING = "loading"
        internal const val PROP_FORM_TYPE = "formType"
        internal const val PROP_OPEN_TYPE = "openType"
        internal const val PROP_NAME = "name"
        internal const val PROP_LANG = "lang"
        internal const val PROP_SESSION_FROM = "sessionFrom"
        internal const val PROP_APP_PARAMETER = "appParameter"
        internal const val PROP_SHOW_MESSAGE_CARD = "showMessageCard"
        internal const val PROP_BUSINESS_ID = "businessId"
    }
}

/**
 * Events for [WXButtonView].
 *
 * The callback parameter is a [JSONObject] with key `data` which carries the
 * JSON-stringified `detail` payload from the mini-program native event.
 */
class WXButtonEvent : ComposeEvent() {

    /**
     * Called when user authorizes phone number, triggered by `openType = "getPhoneNumber"`.
     */
    fun onGetPhoneNumber(handler: (JSONObject) -> Unit) {
        registerJsonCallback(CALLBACK_GET_PHONE_NUMBER, handler)
    }

    /**
     * Called when user authorizes user-info, triggered by `openType = "getUserInfo"`.
     */
    fun onGetUserInfo(handler: (JSONObject) -> Unit) {
        registerJsonCallback(CALLBACK_GET_USER_INFO, handler)
    }

    /**
     * Called when entering customer-service conversation, triggered by `openType = "contact"`.
     */
    fun onContact(handler: (JSONObject) -> Unit) {
        registerJsonCallback(CALLBACK_CONTACT, handler)
    }

    /**
     * Called when the authorize setting page is returned, triggered by `openType = "openSetting"`.
     */
    fun onOpenSetting(handler: (JSONObject) -> Unit) {
        registerJsonCallback(CALLBACK_OPEN_SETTING, handler)
    }

    /**
     * Called when the host app is launched, triggered by `openType = "launchApp"`.
     */
    fun onLaunchApp(handler: (JSONObject) -> Unit) {
        registerJsonCallback(CALLBACK_LAUNCH_APP, handler)
    }

    /**
     * Called when user picks an avatar, triggered by `openType = "chooseAvatar"`.
     */
    fun onChooseAvatar(handler: (JSONObject) -> Unit) {
        registerJsonCallback(CALLBACK_CHOOSE_AVATAR, handler)
    }

    /**
     * Called when the open-ability triggers an error.
     */
    fun onError(handler: (JSONObject) -> Unit) {
        registerJsonCallback(CALLBACK_ERROR, handler)
    }

    private fun registerJsonCallback(eventName: String, handler: (JSONObject) -> Unit) {
        register(eventName) {
            handler(it as JSONObject)
        }
    }

    companion object {
        internal const val CALLBACK_GET_PHONE_NUMBER = "getPhoneNumberCallback"
        internal const val CALLBACK_GET_USER_INFO = "getUserInfoCallback"
        internal const val CALLBACK_CONTACT = "contactCallback"
        internal const val CALLBACK_OPEN_SETTING = "openSettingCallback"
        internal const val CALLBACK_LAUNCH_APP = "launchAppCallback"
        internal const val CALLBACK_CHOOSE_AVATAR = "chooseAvatarCallback"
        internal const val CALLBACK_ERROR = "errorCallback"
    }
}

/**
 * DSL builder for [WXButtonView].
 */
fun ViewContainer<*, *>.WXButton(init: WXButtonView.() -> Unit) {
    addChild(WXButtonView(), init)
}
