package com.tencent.kuikly.core.render.web.export

import com.tencent.kuikly.core.render.web.ktx.SizeF

/**
 * Shadow view protocol, generally used for custom layout implementation
 */
interface IKuiklyRenderShadowExport {
    /**
     * Called when updating shadow object properties
     *
     * @param propKey Property key
     * @param propValue Property value
     */
    fun setShadowProp(propKey: String, propValue: Any)

    /**
     * Call shadow object methods
     *
     * @param methodName Method name
     * @param params Parameters
     */
    fun call(methodName: String, params: String): Any? = null

    /**
     * Calculate and return actual size of RenderView based on layout constraint size
     *
     * @param constraintSize Constraint size
     *
     * @return Calculated actual size
     */
    fun calculateRenderViewSize(constraintSize: SizeF): SizeF
}
