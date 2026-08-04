package com.tencent.kuikly.core.render.web.expand.components

import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
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

    // Component's original top value in normal flow
    private var initialTop = 0f
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
     * set hover margin top
     */
    private fun setHoverMarginTop(propValue: Any): Boolean {
        hoverViewMarginTop = propValue.unsafeCast<Float>()
        return true
    }

    /**
     * Find nearest scrollable ancestor to avoid relying on rigid parent/grandparent hierarchy.
     */
    private fun findNearestScrollContainer(start: HTMLElement?): HTMLElement? {
        var node = start
        while (node != null) {
            val style = kuiklyWindow.getComputedStyle(node)
            val overflowY = style?.overflowY ?: ""
            val isScrollableY = (overflowY == "auto" || overflowY == "scroll") && node.scrollHeight > node.clientHeight
            if (isScrollableY) {
                return node
            }
            node = node.parentElement?.unsafeCast<HTMLElement?>()
        }
        return null
    }

    /**
     * Measure element top relative to a specific container.
     */
    private fun getTopRelativeToContainer(target: HTMLElement, container: HTMLElement): Float {
        val targetRect = target.getBoundingClientRect()
        val containerRect = container.getBoundingClientRect()
        return (targetRect.top - containerRect.top + container.scrollTop).toFloat()
    }

    /**
     * Compute the fixed top position in viewport coordinates.
     */
    private fun getFixedTopInViewport(container: HTMLElement): Float {
        return container.getBoundingClientRect().top.toFloat() + hoverViewMarginTop
    }

    /**
     * When node is inserted into parent node, bind nearest scroll container event and pass scroll parameters.
     */
    override fun onAddToParent(parent: Element) {
        super.onAddToParent(parent)
        val parentElement = parent.unsafeCast<HTMLElement?>() ?: return
        val scrollContainer = findNearestScrollContainer(parentElement) ?: return

        val updateInitialTop = {
            initialTop = getTopRelativeToContainer(ele, scrollContainer)
        }

        updateInitialTop()

        // Listen to scroll changes, handle hover state.
        scrollContainer.addEventListener("scroll", {
            if (initialTop == 0f) {
                updateInitialTop()
            }
            val contentOffsetTop = scrollContainer.scrollTop.toFloat()
            if (contentOffsetTop > initialTop - hoverViewMarginTop) {
                ele.style.position = "fixed"
                ele.style.top = "${getFixedTopInViewport(scrollContainer)}px"
            } else {
                ele.style.position = "absolute"
                ele.style.top = initialTop.toPxF()
            }
        }, json("passive" to true))
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
