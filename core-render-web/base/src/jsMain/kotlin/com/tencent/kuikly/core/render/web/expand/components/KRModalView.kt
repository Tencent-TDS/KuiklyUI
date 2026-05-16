package com.tencent.kuikly.core.render.web.expand.components

import com.tencent.kuikly.core.render.web.const.KRExtraConst
import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.kuiklyDocument
import com.tencent.kuikly.core.render.web.runtime.dom.element.ElementType
import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement

/**
 * KRModalView, corresponding to Kuikly Modal view.
 * In web, the modal element is moved from its original parent to document.body
 * to achieve full-screen overlay effect, similar to Android's addContentView.
 *
 * When nested KRModalViews are detected (e.g. Modal wrapping ActionSheet which
 * also uses Modal internally), the inner modal stays inside the outer one
 * instead of being moved to body again, preserving the correct layer hierarchy.
 */
class KRModalView : IKuiklyRenderViewExport {
    // div instance
    private val div = kuiklyDocument.createElement(ElementType.DIV)
    // Whether the view has been moved to body
    private var didMoveToWindow = false

    override val reusable: Boolean
        get() = false

    override val ele: HTMLDivElement
        get() = div.unsafeCast<HTMLDivElement>()

    override fun setProp(propKey: String, propValue: Any): Boolean {
        return super.setProp(propKey, propValue)
    }

    override fun onAddToParent(parent: Element) {
        super.onAddToParent(parent)
        // Move to document.body for full-screen display, similar to iOS Window / Android addContentView
        if (!didMoveToWindow) {
            didMoveToWindow = true
            // If the current modal is already nested inside another KRModalView
            // (which has already been moved to body), keep it in place to
            // preserve the correct parent-child relationship.
            if (isInsideModalView(parent)) {
                return
            }
            parent.removeChild(ele)
            kuiklyDocument.body?.appendChild(ele)
        }
    }

    /**
     * Walk up the ancestor chain from [parent] to check whether there is
     * already a KRModalView ancestor. If so, the current modal is nested
     * and should NOT be moved to body again.
     */
    private fun isInsideModalView(parent: Element): Boolean {
        var node: Element? = parent
        val body = kuiklyDocument.body
        while (node != null && node != body) {
            if (node.getAttribute(KRExtraConst.COMPONENT_IDENTIFIER_KEY) == VIEW_NAME) {
                return true
            }
            node = node.parentElement
        }
        return false
    }

    companion object {
        const val VIEW_NAME = "KRModalView"
        const val CONTAINER_SIZE_CHANGED = "containerSizeChanged"
    }
}
