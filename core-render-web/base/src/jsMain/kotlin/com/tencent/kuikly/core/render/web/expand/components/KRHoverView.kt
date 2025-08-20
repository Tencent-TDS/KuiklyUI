package com.tencent.kuikly.core.render.web.expand.components

import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.ktx.pxToFloat
import com.tencent.kuikly.core.render.web.ktx.toPxF

import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import org.w3c.dom.Element

import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import kotlin.js.json

/**
 * Hover top view
 */
class KRHoverView : IKuiklyRenderViewExport {
    // div instance
    private val hover = kuiklyDocument.createElement(ElementType.DIV)

    // Component's original top value
    private var top = 0f
    private var hoverViewMarginTop = 0f

    override val ele: HTMLDivElement
        get() = hover.unsafeCast<HTMLDivElement>()

    /**
     * Set hover view's display layer
     */
    private fun setBringIndex(index: Any): Boolean {
        // Set display layer
        ele.style.zIndex = index.unsafeCast<Int>().toString()
        return true
    }

    /**
     * Get total top value
     */
    private fun getTotalTop(element: HTMLElement?): Float {
        var totalTop = 0f
        if (element === null) {
            return totalTop
        }
        // calculate current top value
        totalTop += element.style.top.pxToFloat()

        // plus all parent's top value
        var parent = element.parentElement
        while (parent !== null) {
            totalTop += parent.unsafeCast<HTMLElement>().style.top.pxToFloat()
            parent = parent.parentElement
        }

        return totalTop
    }

    /**
     * set hover margin top
     */
    private fun setHoverMarginTop(propValue: Any): Boolean {
        hoverViewMarginTop = propValue.unsafeCast<Float>()
        return true
    }

    /**
     * When node is inserted into parent node, bind parent's scroll event and pass scroll parameters
     */
    override fun onAddToParent(parent: Element) {
        super.onAddToParent(parent)
        // Current node's parent element is listView's scroll content area scrollContentView.
        // Actual scroll view needs to get grandparent node
        if (parent.parentElement !== null) {
            // Save scroll grandparent node
            val grandParent = parent.parentElement.unsafeCast<HTMLElement?>()
            val totalTop = getTotalTop(grandParent)
            // Save current component's top value
            top = ele.style.top.pxToFloat()
            // Listen to grandparent scroll node change event, handle hover state,
            // if scroll distance is greater than top value, set to fixed, otherwise restore
            grandParent?.addEventListener("scroll", {
                val contentOffsetTop = grandParent.scrollTop
                if (contentOffsetTop > top - hoverViewMarginTop) {
                    // Scroll distance is greater than hover component's top value, set to scroll top value
                    // and position set to fixed
                    ele.style.position = "fixed"
                    ele.style.top = "${totalTop + hoverViewMarginTop}px"
                } else {
                    ele.style.position = "absolute"
                    ele.style.top = top.toPxF()
                }
            }, json("passive" to true))
        }
    }

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            MARGIN_TOP -> setHoverMarginTop(propValue)
            BRING_INDEX -> setBringIndex(propValue)
            else -> super.setProp(propKey, propValue)
        }
    }

    companion object {
        const val VIEW_NAME = "KRHoverView"
        private const val BRING_INDEX = "bringIndex"
        private const val MARGIN_TOP = "hoverMarginTop"
    }
}
