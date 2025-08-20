package com.tencent.kuikly.core.render.web.expand.components

import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import org.w3c.dom.HTMLDivElement

/**
 * Mask hollow view
 */
class KRMaskView : IKuiklyRenderViewExport {
    private val mask = kuiklyDocument.createElement(ElementType.DIV)

    override val ele: HTMLDivElement
        get() = mask.unsafeCast<HTMLDivElement>()

    companion object {
        const val VIEW_NAME = "KRMaskView"
    }
}
