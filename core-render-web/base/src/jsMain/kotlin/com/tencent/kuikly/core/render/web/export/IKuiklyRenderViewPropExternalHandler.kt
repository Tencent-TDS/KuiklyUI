package com.tencent.kuikly.core.render.web.export

/**
 * [IKuiklyRenderViewExport]自定义属性Handler
 */
interface IKuiklyRenderViewPropExternalHandler {

    /**
     * 设置属性
     * @param renderViewExport
     * @param propKey
     * @param propValue
     * @return 是否处理该属性
     */
    fun setViewExternalProp(
        renderViewExport: IKuiklyRenderViewExport,
        propKey: String,
        propValue: Any
    ): Boolean

    /**
     * 重置属性，只有在[IKuiklyRenderViewExport]是可复用的情况下，才会被调用
     * @param renderViewExport
     * @param propKey
     * @return 是否处理了resetProp
     */
    fun resetViewExternalProp(renderViewExport: IKuiklyRenderViewExport, propKey: String): Boolean
}
