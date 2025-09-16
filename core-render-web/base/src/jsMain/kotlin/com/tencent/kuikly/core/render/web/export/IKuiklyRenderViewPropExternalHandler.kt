package com.tencent.kuikly.core.render.web.export

/**
 * [IKuiklyRenderViewExport] Custom Prop Handler
 */
interface IKuiklyRenderViewPropExternalHandler {

    /**
     * set custom prop
     *
     * @param renderViewExport
     * @param propKey
     * @param propValue
     *
     * @return should handle prop
     */
    fun setViewExternalProp(
        renderViewExport: IKuiklyRenderViewExport,
        propKey: String,
        propValue: Any
    ): Boolean

    /**
     * reset prop，only call when [IKuiklyRenderViewExport] is reusable
     *
     * @param renderViewExport
     * @param propKey
     *
     * @return  handle prop reset yes or no
     */
    fun resetViewExternalProp(renderViewExport: IKuiklyRenderViewExport, propKey: String): Boolean
}
