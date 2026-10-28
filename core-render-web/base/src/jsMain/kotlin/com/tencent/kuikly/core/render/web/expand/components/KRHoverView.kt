package com.tencent.kuikly.core.render.web.expand.components

import com.tencent.kuikly.core.render.web.const.KRCssConst
import com.tencent.kuikly.core.render.web.const.KRListConst
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.Frame
import com.tencent.kuikly.core.render.web.ktx.hrAnimation
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow
import com.tencent.kuikly.core.render.web.ktx.pxToFloat
import com.tencent.kuikly.core.render.web.ktx.toPxF
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import kotlin.js.json
import kotlin.math.max

/**
 * Hover top view
 */
class KRHoverView : IKuiklyRenderViewExport {
    // div instance
    private val hover = kuiklyDocument.createElement(ElementType.DIV)

    // Keep the layout top separate from the visual position while hovering.
    private var initialTop = 0f
    private var initialLeft = ""
    private var parentOffsetTop = 0f
    private var fixedLeftOffset = 0f
    private var hoverViewMarginTop = 0f
    private var scrollContainer: HTMLElement? = null
    private val scrollListener: (Event) -> Unit = { updateHoverPosition() }

    override val ele: HTMLDivElement
        get() = hover.unsafeCast<HTMLDivElement>()

    /**
     * Set hover view's display layer
     */
    private fun setBringIndex(index: Any): Boolean {
        // Start at layer 1 so bringIndex=0 paints above later list rows.
        ele.style.zIndex = (index.unsafeCast<Int>() + 1).toString()
        return true
    }

    /**
     * set hover margin top
     */
    private fun setHoverMarginTop(propValue: Any): Boolean {
        hoverViewMarginTop = propValue.unsafeCast<Float>()
        updateHoverPosition()
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
            // A Hover may attach before its Kuikly list overflows.
            val isScrollableY = (overflowY == "auto" || overflowY == "scroll") &&
                (node.classList.contains(KRListConst.IS_LIST) || node.scrollHeight > node.clientHeight)
            if (isScrollableY) {
                return node
            }
            node = node.parentElement?.unsafeCast<HTMLElement?>()
        }
        return null
    }

    /**
     * Layout coordinates ignore ancestor transforms, unlike getBoundingClientRect().
     * Include offset-parent borders because CSS top is relative to the padding edge.
     */
    private fun getLayoutTop(element: HTMLElement): Float {
        var top = 0f
        var node: HTMLElement? = element
        while (node != null) {
            val style = kuiklyWindow.getComputedStyle(node)
            // offsetTop rounds to integers; Kuikly's absolute frames can contain subpixels.
            top += if (style?.position == "absolute" && style.top.endsWith("px")) {
                style.top.pxToFloat() + style.marginTop.pxToFloat()
            } else {
                node.offsetTop.toFloat()
            }
            val parent = node.offsetParent.unsafeCast<HTMLElement?>()
            if (parent != null) {
                top += parent.clientTop.toFloat()
            }
            node = parent
        }
        return top
    }

    private fun hasTransformedAncestor(): Boolean {
        var node = ele.parentElement.unsafeCast<HTMLElement?>()
        while (node != null) {
            val transform = kuiklyWindow.getComputedStyle(node)?.transform ?: "none"
            if (transform != "none" && transform.isNotEmpty()) {
                return true
            }
            node = node.parentElement.unsafeCast<HTMLElement?>()
        }
        return false
    }

    private fun updateParentOffset() {
        val container = scrollContainer ?: return
        val parent = ele.offsetParent.unsafeCast<HTMLElement?>() ?: return
        parentOffsetTop = getLayoutTop(parent) + parent.clientTop -
            getLayoutTop(container) - container.clientTop
    }

    private fun restoreLayoutPosition() {
        ele.style.position = "absolute"
        ele.style.top = initialTop.toPxF()
        ele.style.left = initialLeft
    }

    private fun updateHoverPosition() {
        val container = scrollContainer ?: return
        if (hasTransformedAncestor()) {
            if (ele.style.position == "fixed") {
                restoreLayoutPosition()
            }
            updateParentOffset()
            val hoverTop = container.scrollTop.toFloat() + hoverViewMarginTop - parentOffsetTop
            // A transformed ancestor changes the fixed containing block, so stay in content coordinates.
            ele.style.position = "absolute"
            ele.style.top = max(initialTop, hoverTop).toPxF()
            return
        }

        val hoverTop = container.scrollTop.toFloat() + hoverViewMarginTop - parentOffsetTop
        if (hoverTop > initialTop) {
            val containerRect = container.getBoundingClientRect()
            if (ele.style.position != "fixed") {
                fixedLeftOffset = ele.getBoundingClientRect().left.toFloat() -
                    containerRect.left.toFloat() + container.scrollLeft.toFloat()
            }
            // Fixed positioning stays pinned even before the next main-thread scroll event.
            ele.style.position = "fixed"
            ele.style.top = (containerRect.top.toFloat() + container.clientTop + hoverViewMarginTop).toPxF()
            ele.style.left = (containerRect.left.toFloat() + fixedLeftOffset - container.scrollLeft).toPxF()
        } else {
            restoreLayoutPosition()
        }
    }

    override fun onAddToParent(parent: Element) {
        super.onAddToParent(parent)
        unbindScroll()
        initialTop = ele.style.top.pxToFloat()
        initialLeft = ele.style.left
        val parentElement = parent.unsafeCast<HTMLElement?>() ?: return
        val container = findNearestScrollContainer(parentElement) ?: return
        scrollContainer = container
        updateParentOffset()
        container.addEventListener("scroll", scrollListener, json("passive" to true))
        updateHoverPosition()
    }

    override fun onFrameChange(frame: Frame) {
        super.onFrameChange(frame)
        // Queued frames have not written layout styles; the current top/left may still be hover coordinates.
        if (ele.hrAnimation?.supportAnimation(KRCssConst.FRAME) == true) return
        val pendingAnimId: dynamic = ele.asDynamic().executeAnimationId
        if (jsTypeOf(pendingAnimId) == "number" && pendingAnimId.unsafeCast<Int>() != 0) return
        initialTop = ele.style.top.pxToFloat()
        initialLeft = ele.style.left
        updateParentOffset()
        updateHoverPosition()
    }

    override fun onRemoveFromParent(parent: Element) {
        unbindScroll()
        super.onRemoveFromParent(parent)
    }

    override fun onDestroy() {
        unbindScroll()
        super.onDestroy()
    }

    private fun unbindScroll() {
        scrollContainer?.let {
            it.removeEventListener("scroll", scrollListener)
            restoreLayoutPosition()
        }
        scrollContainer = null
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
