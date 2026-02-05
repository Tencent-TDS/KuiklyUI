package com.tencent.kuikly.core.render.web.runtime.miniapp

import com.tencent.kuikly.core.render.web.collection.FastMutableMap
import com.tencent.kuikly.core.render.web.collection.fastMutableMapOf
import com.tencent.kuikly.core.render.web.expand.components.KRScrollContentView
import com.tencent.kuikly.core.render.web.ktx.isAllDigits
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import com.tencent.kuikly.core.render.web.runtime.miniapp.const.RenderConst.PAGE_NAME
import com.tencent.kuikly.core.render.web.runtime.miniapp.const.RenderConst.PLATFORM
import com.tencent.kuikly.core.render.web.runtime.miniapp.const.RenderConst.STATUS_BAR_HEIGHT
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.Page
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniCanvasElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniDivElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniElementManage
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniElementUtil
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniImageElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniInputElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniListElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniParagraphElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniRootElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniScrollContentElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniSpanElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniTextAreaElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniVideoElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.page.MiniPageManage
import com.tencent.kuikly.core.render.web.utils.Log

/**
 * Mini program host document related operations
 */
object MiniDocument {
    private const val TAG = "MiniDocument"
    private const val SAFE_AREA_INSETS = "safeAreaInsets"
    private const val PARAM = "param"
    private const val IS_MINI_PROGRAM = "is_miniprogram"

    /**
     * Get MiniElement implementation by ID
     */
    private val getElementByIdImpl = { elementId: String ->
        val miniId = if (elementId.isAllDigits()) {
            // number id is root element, should transform to inner id
            "${MiniElementUtil.SER_ID_PREFIX}${elementId}"
        } else {
            elementId
        }
        MiniElementManage.getElement(miniId)
    }

    /**
     * Create mini program host element
     */
    @JsName("createElement")
    fun createElement(name: String, elementType: String?): MiniElement {
        // Return actual mini program host created element
        val element: MiniElement = when (name) {
            ElementType.SPAN -> {
                MiniSpanElement()
            }

            ElementType.CANVAS -> {
                MiniCanvasElement()
            }

            ElementType.IMAGE -> {
                MiniImageElement()
            }

            ElementType.INPUT -> {
                MiniInputElement()
            }

            ElementType.P -> {
                MiniParagraphElement()
            }

            ElementType.TEXT_AREA -> {
                MiniTextAreaElement()
            }

            ElementType.VIDEO -> {
                MiniVideoElement()
            }

            ElementType.PAG -> {
                // Pag tag not supported yet
                MiniDivElement()
            }

            ElementType.LIST -> {
                MiniListElement()
            }

            else -> {
                if (elementType == KRScrollContentView.VIEW_NAME) {
                    MiniScrollContentElement()
                } else {
                    // Return MiniDiv element by default
                    MiniDivElement()
                }
            }
        }
        return element
    }

    /**
     * Get mini program host element by element id
     */
    @JsName("getElementById")
    fun getElementById(elementId: String): MiniElement? = getElementByIdImpl(elementId)

    /**
     * Create text node for mini program
     */
    @JsName("createTextNode")
    fun createTextNode(text: String): MiniElement {
        return MiniSpanElement(text)
    }

    /**
     * Create mini app page root node
     */
    fun createRootContainer(pageId: Int) {
        val root = MiniRootElement()
        root.id = pageId.toString()
        val miniPage = MiniPageManage.getMiniPageByPageId(pageId)
        root.miniPageInstance = miniPage!!.mpInstance

        // Need to set transform and transition to ensure drop-shadow and other content settings take effect
        if (MiniGlobal.isIOS) {
            miniPage.lifeCycle.onReady {
                MiniGlobal.setTimeout({
                    root.firstElementChild?.style?.transform = "translate(0,0)"
                    root.firstElementChild?.style?.transition = "transform 250ms ease-out"
                }, 0)
            }
        } else {
            miniPage.lifeCycle.onReady {
                root.firstElementChild?.style?.transform = "translate(0, 0)"
            }
        }

        root.performUpdate()
    }

    /**
     * init mini app page instance
     */
    fun initPage(
        renderParams: FastMutableMap<String, Any>,
        onLoadCallback:
            (pageId: Int, pageName: String, paramsMap: FastMutableMap<String, Any>) -> Unit
    ) {
        var usedPageName = ""
        // Initialize mini program core configuration
        val miniPage = Page.initMiniPage()
        // Execute renderView initialization logic after mini program onLoad
        miniPage.lifeCycle.onLoad { miniPageName, params ->
            // Priority: 1. page_name from URL params, 2. pageName from renderParams
            usedPageName = if (miniPageName.isNotEmpty()) {
                miniPageName
            } else {
                renderParams[PAGE_NAME].unsafeCast<String?>() ?: ""
            }
            if (usedPageName == "") {
                throw IllegalArgumentException("pageName is empty")
            }
            // Business parameters
            val paramsMap = fastMutableMapOf<String, Any>().apply {
                putAll(FastMutableMap<String,Any>(params))
                set(SAFE_AREA_INSETS, "${MiniGlobal.statusBarHeight} 0 0 0")
                set(IS_MINI_PROGRAM, "1")
            }
            val usedParams: FastMutableMap<String, Any> = fastMutableMapOf()

            usedParams.apply {
                set(PLATFORM, "miniprogram")
                set(
                    STATUS_BAR_HEIGHT,
                    renderParams[STATUS_BAR_HEIGHT] ?: MiniGlobal.statusBarHeight
                )
                set(PARAM, paramsMap)
            }

            onLoadCallback(miniPage.pageId, usedPageName, usedParams)
        }

        miniPage.lifeCycle.onReady {
            Log.log(TAG, "page: $usedPageName on ready")
        }

        miniPage.lifeCycle.onShow {
            Log.log(TAG, "page: $usedPageName on show")
        }

        miniPage.lifeCycle.onHide {
            Log.log(TAG, "page: $usedPageName on hide")
        }
    }
}
