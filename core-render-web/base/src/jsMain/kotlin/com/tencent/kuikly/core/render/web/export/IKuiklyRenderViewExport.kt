package com.tencent.kuikly.core.render.web.export

import com.tencent.kuikly.core.render.web.IKuiklyRenderContext
import com.tencent.kuikly.core.render.web.ktx.Frame
import com.tencent.kuikly.core.render.web.ktx.KRCssConst
import com.tencent.kuikly.core.render.web.ktx.setCommonProp
import org.w3c.dom.Element

/**
 * Rendering view component protocol, components expose themselves as kuikly UI components
 * by implementing [IKuiklyRenderViewExport].
 * Business custom UI components need to implement this protocol.
 */
interface IKuiklyRenderViewExport : IKuiklyRenderModuleExport {
    // All web render view objects are of type Element
    val ele: Element

    /**
     * Reset view for reuse (optional implementation), if this method returns true then the view can be reused
     */
    val reusable: Boolean
        get() = false

    /**
     * KuiklyRender context, not available in web currently
     */
    override var kuiklyRenderContext: IKuiklyRenderContext?
        get() = null
        set(_) {}

    /**
     * Update view properties
     *
     * @param propKey Property key
     * @param propValue Property value
     *
     * @return Whether property was handled, if true and component is [IKuiklyRenderViewExport.reusable],
     * [com.tencent.kuikly.core.render.web.layer.IKuiklyRenderLayerHandler] will record the property key,
     * and call [IKuiklyRenderViewExport.resetProp] and [IKuiklyRenderViewExport.resetShadow] methods
     * when component is reused to reset the view
     */
    fun setProp(propKey: String, propValue: Any): Boolean {
        val result = ele.setCommonProp(propKey, propValue)
        if (propKey == KRCssConst.FRAME) {
            // If setting size, notify that size information has changed
            onFrameChange(propValue.unsafeCast<Frame>())
        }

        return result
    }

    /**
     * Reset property, web currently does not support reusing element nodes
     *
     * @param propKey Property key to reset
     */
    fun resetProp(propKey: String): Boolean = true

    /**
     * Set shadow object corresponding to current renderView instance
     */
    fun setShadow(shadow: IKuiklyRenderShadowExport) {

    }

    /**
     * Reset shadow
     */
    fun resetShadow() {

    }

    /**
     * Called when view is added to parent
     *
     * @param parent Parent node of the view
     */
    fun onAddToParent(parent: Element) {

    }

    /**
     * Called when view is removed from parent
     *
     * @param parent Parent node of the view
     */
    fun onRemoveFromParent(parent: Element) {

    }

    /**
     * Remove element from parent node
     */
    fun removeFromParent() {
        val parent = ele.parentElement ?: return
        // Remove child node
        parent.removeChild(ele)
        // Notify child node removal
        onRemoveFromParent(parent)
    }

    /**
     * Callback when node size changes
     */
    fun onFrameChange(frame: Frame) {

    }
}
