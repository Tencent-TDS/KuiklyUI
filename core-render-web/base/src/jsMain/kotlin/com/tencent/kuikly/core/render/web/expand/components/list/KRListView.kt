package com.tencent.kuikly.core.render.web.expand.components.list

import com.tencent.kuikly.core.render.web.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.web.ktx.KRCssConst
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.ktx.toPxF
import com.tencent.kuikly.core.render.web.processor.KuiklyProcessor
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement

/**
 * KRListView, corresponding to Kuikly's Scroller
 */
class KRListView : IKuiklyRenderViewExport {
    // Scroll container element
    private val listEle = KuiklyProcessor.listProcessor.createListElement()

    override val ele: HTMLElement
        get() = listEle.ele

    /**
     * Override special property write operations
     */
    override fun setProp(propKey: String, propValue: Any): Boolean {
        return when (propKey) {
            SCROLL -> {
                // Process scroll events, convert web event return values to kuikly event return value format
                listEle.scrollEventCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                // Bind scroll-related event handlers
                listEle.setScrollEvent()
                true
            }

            KRCssConst.OVERFLOW -> {
                // Handle overflow value compatibility, Web and mini program handle differently, each host handles it
                // For web, scrollbar needs to be shown by default, disable scrolling through
                // setScrollEnable, no handling for overflow
                // When overflow is set to visible in Web, content is not clipped and appears outside the element box.
                // In Android, this can be achieved by setting clipChildren to false.
                // When overflow is set to hidden in Web, content is clipped and the rest is invisible. In Android,
                // this can be achieved by setting clipChildren to true.
                // When overflow is set to scroll or auto in Web, content is clipped but browser shows scrollbar
                // to view the rest. In Android,
                // this effect may need to use scroll containers like ScrollView or RecyclerView, while clipChildren
                // property still needs to be set to true.
                true
            }

            DRAG_BEGIN -> {
                // Register drag begin event
                listEle.dragBeginEventCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }

            DRAG_END -> {
                // Register drag end event
                listEle.dragEndEventCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }

            WILL_DRAG_END -> {
                // Will end drag callback
                listEle.willDragEndEventCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                true
            }

            SCROLL_END -> {
                listEle.scrollEndEventCallback = propValue.unsafeCast<KuiklyRenderCallback>()
                // Bind scroll end event
                listEle.setScrollEndEvent()
                true
            }

            KRCssConst.BORDER_RADIUS -> {
                // not set overflow hidden
                val borderRadiusSpilt = propValue.unsafeCast<String>().asDynamic().split(",")
                val baseRadius = borderRadiusSpilt[0]
                if (baseRadius == borderRadiusSpilt[1] && baseRadius == borderRadiusSpilt[2] && baseRadius == borderRadiusSpilt[3]) {
                    ele.style.borderRadius = borderRadiusSpilt[0].unsafeCast<String>().toPxF()
                } else {
                    ele.style.borderTopLeftRadius = borderRadiusSpilt[0].unsafeCast<String>().toPxF()
                    ele.style.borderTopRightRadius = borderRadiusSpilt[1].unsafeCast<String>().toPxF()
                    ele.style.borderBottomLeftRadius = borderRadiusSpilt[2].unsafeCast<String>().toPxF()
                    ele.style.borderBottomRightRadius = borderRadiusSpilt[3].unsafeCast<String>().toPxF()
                }
                true
            }


            // Set whether scrolling is allowed
            SCROLL_ENABLE -> listEle.setScrollEnable(propValue)
            // Set whether to show scrollbar
            SHOW_SCROLL_INDICATOR -> listEle.setShowScrollIndicator(propValue)
            // Set scroll direction
            SET_SCROLL_DIRECTION -> listEle.setScrollDirection(propValue)
            // Set whether paging is needed
            PAGING_ENABLE -> listEle.setPagingEnable(propValue)
            // Set bounce effect is enable
            BOUNCES_ENABLE-> listEle.setBounceEnable(propValue)
            // Set nested scroll is enable
            NESTED_SCROLL -> listEle.setNestedScroll(propValue)
            else -> super.setProp(propKey, propValue)
        }
    }

    /**
     * Call listView related methods
     */
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_CONTENT_OFFSET -> listEle.setContentOffset(params)
            METHOD_CONTENT_INSET -> listEle.setContentInset(params)
            METHOD_CONTENT_INSET_WHEN_END_DRAG -> listEle.setContentInsetWhenEndDrag(params)
            else -> super.call(method, params, callback)
        }
    }

    /**
     * Component destruction
     */
    override fun onDestroy() {
        // First call element's destroy method
        listEle.destroy()
        // Then call parent class destroy method
        super.onDestroy()
    }

    companion object {
        const val VIEW_NAME = "KRListView"

        // scroll view and list view are the same thing
        const val VIEW_NAME_SCROLL_VIEW = "KRScrollView"

        // Set content offset, will scroll List to corresponding position
        private const val METHOD_CONTENT_OFFSET = "contentOffset"

        // ContentInset set when drag ends
        private const val METHOD_CONTENT_INSET_WHEN_END_DRAG =
            "contentInsetWhenEndDrag"

        // Set content margin
        private const val METHOD_CONTENT_INSET = "contentInset"
        private const val SCROLL_ENABLE = "scrollEnabled"
        // Whether to enable paging
        private const val PAGING_ENABLE = "pagingEnabled"
        // Whether to enable bounce
        private const val BOUNCES_ENABLE = "bouncesEnable"
        // Whether to enable nestedScroll
        private const val NESTED_SCROLL = "nestedScroll"

        // Whether to show scrollbar
        private const val SHOW_SCROLL_INDICATOR = "showScrollerIndicator"

        // Set scroll direction
        private const val SET_SCROLL_DIRECTION = "directionRow"

        // Scroll-related events
        private const val DRAG_BEGIN = "dragBegin"
        private const val WILL_DRAG_END = "willDragEnd"
        private const val DRAG_END = "dragEnd"
        private const val SCROLL = "scroll"
        private const val SCROLL_END = "scrollEnd"
    }
}
