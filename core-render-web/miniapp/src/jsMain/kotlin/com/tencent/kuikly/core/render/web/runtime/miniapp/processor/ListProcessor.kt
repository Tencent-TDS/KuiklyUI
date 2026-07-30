package com.tencent.kuikly.core.render.web.runtime.miniapp.processor

import com.tencent.kuikly.core.render.web.processor.IListProcessor
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import com.tencent.kuikly.core.render.web.runtime.dom.element.IListElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.MiniDocument

/**
 * h5 list processor
 */
object ListProcessor : IListProcessor {
    /**
     * create list element
     */
    override fun createListElement(): IListElement =
        MiniDocument.createElement(ElementType.LIST, null).unsafeCast<IListElement>()
}