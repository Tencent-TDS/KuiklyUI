package com.tencent.kuikly.core.render.web.processor

import com.tencent.kuikly.core.render.web.runtime.dom.element.IListElement

/**
 * list view processor
 */
interface IListProcessor {
    /**
     * create list view
     */
    fun createListElement(): IListElement
}