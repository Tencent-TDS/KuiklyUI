package com.tencent.kuikly.core.render.web.runtime.web.expand

import com.tencent.kuikly.core.render.web.const.KRCssConst
import com.tencent.kuikly.core.render.web.const.KRJsTypeConst
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewPropExternalHandler
import org.w3c.dom.HTMLElement

/**
 * Default Handler: external prop handler for custom props.
 * Handles the "cssClass" attribute set via `fun Attr.cssClass(value: String)` in common code.
 */
internal class KRCustomPropsHandler : IKuiklyRenderViewPropExternalHandler {

    override fun setViewExternalProp(
        renderViewExport: IKuiklyRenderViewExport,
        propKey: String,
        propValue: Any
    ): Boolean {
        when (propKey) {
            KRCssConst.CSS_CLASS -> {
                val ele = renderViewExport.ele.unsafeCast<HTMLElement>()
                if (jsTypeOf(ele.asDynamic().classList) == KRJsTypeConst.UNDEFINED) return false

                removeTrackedCssClasses(ele)

                val cssClassValue = propValue.unsafeCast<String>().trim()
                cssClassValue
                    .split("\\s+".toRegex())
                    .filter { it.isNotEmpty() }
                    .forEach { className ->
                        ele.classList.add(className)
                    }
                ele.setAttribute(TRACKED_CSS_CLASS_ATTR, cssClassValue)
                return true
            }
            else -> return false
        }
    }

    override fun resetViewExternalProp(
        renderViewExport: IKuiklyRenderViewExport,
        propKey: String
    ): Boolean {
        return when (propKey) {
            KRCssConst.CSS_CLASS -> {
                val ele = renderViewExport.ele.unsafeCast<HTMLElement>()
                if (jsTypeOf(ele.asDynamic().classList) == KRJsTypeConst.UNDEFINED) return false

                removeTrackedCssClasses(ele)
                ele.removeAttribute(TRACKED_CSS_CLASS_ATTR)
                true
            }
            else -> false
        }
    }

    private fun removeTrackedCssClasses(ele: HTMLElement) {
        ele.getAttribute(TRACKED_CSS_CLASS_ATTR)
            ?.trim()
            ?.split("\\s+".toRegex())
            ?.filter { it.isNotEmpty() }
            ?.forEach { className ->
                ele.classList.remove(className)
            }
    }

    companion object {
        private const val TRACKED_CSS_CLASS_ATTR = "data-kuikly-css-class"
    }
}
