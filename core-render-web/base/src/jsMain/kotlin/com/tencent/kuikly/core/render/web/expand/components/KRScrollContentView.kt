package com.tencent.kuikly.core.render.web.expand.components

import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import org.w3c.dom.ElementCreationOptions
import org.w3c.dom.HTMLElement

/**
 * KRScrollContentView Content area for ListView list
 */
class KRScrollContentView : IKuiklyRenderViewExport {
    // Scroll container element
    private val div = kuiklyDocument.createElement(
        ElementType.DIV, VIEW_NAME.unsafeCast<ElementCreationOptions>()
    )

    override val ele: HTMLElement
        get() = div.unsafeCast<HTMLElement>()

    companion object {
        const val VIEW_NAME = "KRScrollContentView"
    }
}
