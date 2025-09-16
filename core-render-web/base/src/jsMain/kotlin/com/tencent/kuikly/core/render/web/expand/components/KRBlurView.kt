package com.tencent.kuikly.core.render.web.expand.components

import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import org.w3c.dom.HTMLElement

/**
 * Gaussian blur view
 */
class KRBlurView : IKuiklyRenderViewExport {
    private val blur = kuiklyDocument.createElement(ElementType.DIV)

    override val ele: HTMLElement
        get() = blur.unsafeCast<HTMLElement>()

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            PROP_BLUR_RADIUS -> blurRadius(propValue)
            else -> super.setProp(propKey, propValue)
        }
    }

    private fun blurRadius(propValue: Any): Boolean {
        val radius = propValue.unsafeCast<Float>()
        val blurRadius = if (radius <= 0) {
            1f
        } else if (radius > 25f) {
            25f
        } else {
            radius
        } * 5
        val style = ele.style.asDynamic()
        style.backdropFilter = "blur(${blurRadius}px)"
        style.webkitBackdropFilter = "blur(${blurRadius}px)"
        return true
    }

    companion object {
        const val VIEW_NAME = "KRBlurView"
        private const val PROP_BLUR_RADIUS = "blurRadius"
    }
}
