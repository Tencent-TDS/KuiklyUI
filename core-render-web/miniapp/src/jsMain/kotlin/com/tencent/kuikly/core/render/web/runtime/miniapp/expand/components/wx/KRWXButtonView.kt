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

package com.tencent.kuikly.core.render.web.runtime.miniapp.expand.components.wx

import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.EventHandler
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.wx.MiniWXButtonViewElement
import org.w3c.dom.Element

/**
 * Render view export for the WeChat mini-program native `button` component.
 *
 * Delegates property setting and event listening to [MiniWXButtonViewElement].
 * This class is registered as `KRWXButtonView` inside
 * [com.tencent.kuikly.core.render.web.runtime.miniapp.expand.KuiklyRenderViewDelegator].
 */
class KRWXButtonView : IKuiklyRenderViewExport {
    private val buttonElement = MiniWXButtonViewElement()

    override val ele: Element
        get() = buttonElement.unsafeCast<Element>()

    // Open-ability event callbacks
    private var getPhoneNumberCallback: KuiklyRenderCallback? = null
    private var getUserInfoCallback: KuiklyRenderCallback? = null
    private var contactCallback: KuiklyRenderCallback? = null
    private var openSettingCallback: KuiklyRenderCallback? = null
    private var launchAppCallback: KuiklyRenderCallback? = null
    private var chooseAvatarCallback: KuiklyRenderCallback? = null
    private var errorCallback: KuiklyRenderCallback? = null

    init {
        // Open-ability events exposed by mini-program button
        buttonElement.addEventListener(EVENT_GET_PHONE_NUMBER, createEventForwarder { getPhoneNumberCallback })
        buttonElement.addEventListener(EVENT_GET_USER_INFO, createEventForwarder { getUserInfoCallback })
        buttonElement.addEventListener(EVENT_CONTACT, createEventForwarder { contactCallback })
        buttonElement.addEventListener(EVENT_OPEN_SETTING, createEventForwarder { openSettingCallback })
        buttonElement.addEventListener(EVENT_LAUNCH_APP, createEventForwarder { launchAppCallback })
        buttonElement.addEventListener(EVENT_CHOOSE_AVATAR, createEventForwarder { chooseAvatarCallback })
        buttonElement.addEventListener(EVENT_ERROR, createEventForwarder { errorCallback })
    }

    /**
     * Build an [EventHandler] that forwards the mini-program native event `detail`
     * (as a JSON-serialized string) to the callback returned by [callbackSupplier].
     */
    private fun createEventForwarder(callbackSupplier: () -> KuiklyRenderCallback?): EventHandler {
        return { event: dynamic ->
            val data: dynamic = JSON.stringify(event.detail)
            callbackSupplier()?.invoke(mapOf<String, Any>(KEY_DATA to data.unsafeCast<String>()))
        }
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            PROP_SIZE -> {
                buttonElement.size = propValue as String
                true
            }
            PROP_TYPE -> {
                buttonElement.type = propValue as String
                true
            }
            PROP_PLAIN -> {
                buttonElement.plain = toBoolean(propValue)
                true
            }
            PROP_DISABLED -> {
                buttonElement.disabled = toBoolean(propValue)
                true
            }
            PROP_LOADING -> {
                buttonElement.loading = toBoolean(propValue)
                true
            }
            PROP_FORM_TYPE -> {
                buttonElement.formType = propValue as String
                true
            }
            PROP_OPEN_TYPE -> {
                buttonElement.openType = propValue as String
                true
            }
            PROP_NAME -> {
                buttonElement.name = propValue as String
                true
            }
            PROP_LANG -> {
                buttonElement.lang = propValue as String
                true
            }
            PROP_SESSION_FROM -> {
                buttonElement.sessionFrom = propValue as String
                true
            }
            PROP_APP_PARAMETER -> {
                buttonElement.appParameter = propValue as String
                true
            }
            PROP_SHOW_MESSAGE_CARD -> {
                buttonElement.showMessageCard = toBoolean(propValue)
                true
            }
            PROP_BUSINESS_ID -> {
                buttonElement.businessId = propValue.toString()
                true
            }
            CALLBACK_GET_PHONE_NUMBER -> {
                getPhoneNumberCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }
            CALLBACK_GET_USER_INFO -> {
                getUserInfoCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }
            CALLBACK_CONTACT -> {
                contactCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }
            CALLBACK_OPEN_SETTING -> {
                openSettingCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }
            CALLBACK_LAUNCH_APP -> {
                launchAppCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }
            CALLBACK_CHOOSE_AVATAR -> {
                chooseAvatarCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }
            CALLBACK_ERROR -> {
                errorCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }
            else -> super.setProp(propKey, propValue)
        }
    }

    private fun toBoolean(value: Any): Boolean = when (value) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> value == "1" || value.equals("true", ignoreCase = true)
        else -> false
    }

    companion object {
        const val VIEW_NAME = "KRWXButtonView"

        // Props
        const val PROP_SIZE = "size"
        const val PROP_TYPE = "type"
        const val PROP_PLAIN = "plain"
        const val PROP_DISABLED = "disabled"
        const val PROP_LOADING = "loading"
        const val PROP_FORM_TYPE = "formType"
        const val PROP_OPEN_TYPE = "openType"
        const val PROP_NAME = "name"
        const val PROP_LANG = "lang"
        const val PROP_SESSION_FROM = "sessionFrom"
        const val PROP_APP_PARAMETER = "appParameter"
        const val PROP_SHOW_MESSAGE_CARD = "showMessageCard"
        const val PROP_BUSINESS_ID = "businessId"

        // Callbacks
        const val CALLBACK_GET_PHONE_NUMBER = "getPhoneNumberCallback"
        const val CALLBACK_GET_USER_INFO = "getUserInfoCallback"
        const val CALLBACK_CONTACT = "contactCallback"
        const val CALLBACK_OPEN_SETTING = "openSettingCallback"
        const val CALLBACK_LAUNCH_APP = "launchAppCallback"
        const val CALLBACK_CHOOSE_AVATAR = "chooseAvatarCallback"
        const val CALLBACK_ERROR = "errorCallback"

        // Mini-program native event names (lowercase as dispatched by mini-program runtime)
        private const val EVENT_GET_PHONE_NUMBER = "getphonenumber"
        private const val EVENT_GET_USER_INFO = "getuserinfo"
        private const val EVENT_CONTACT = "contact"
        private const val EVENT_OPEN_SETTING = "opensetting"
        private const val EVENT_LAUNCH_APP = "launchapp"
        private const val EVENT_CHOOSE_AVATAR = "chooseavatar"
        private const val EVENT_ERROR = "error"

        private const val KEY_DATA = "data"
    }
}
