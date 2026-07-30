package com.tencent.kuikly.core.render.web.runtime.web.expand.processor

import com.tencent.kuikly.core.render.web.processor.IListProcessor
import com.tencent.kuikly.core.render.web.runtime.dom.element.IListElement
import com.tencent.kuikly.core.render.web.runtime.web.expand.components.list.H5ListView

/**
 * h5 list processor
 */
object ListProcessor : IListProcessor {
    /**
     * create list element
     */
    override fun createListElement(): IListElement = H5ListView()
}