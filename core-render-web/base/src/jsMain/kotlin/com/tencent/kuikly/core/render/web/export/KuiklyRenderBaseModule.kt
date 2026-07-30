package com.tencent.kuikly.core.render.web.export

import com.tencent.kuikly.core.render.web.IKuiklyRenderContext
import org.w3c.dom.Element

/**
 * Base module class exposed to Kuikly from Native side
 */
open class KuiklyRenderBaseModule : IKuiklyRenderModuleExport {

    private var _kuiklyRenderContext: IKuiklyRenderContext? = null

    /**
     * Get KuiklyRenderContext
     */
    override var kuiklyRenderContext: IKuiklyRenderContext?
        get() = _kuiklyRenderContext
        set(value) {
            _kuiklyRenderContext = value
        }

    /**
     * Get [Element] by tag
     *
     * @param tag Tag corresponding to [Element]
     */
    fun viewWithTag(tag: Int): Element? = kuiklyRenderContext?.getView(tag)
}
