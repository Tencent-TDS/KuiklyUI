package com.tencent.kuikly.core.render.web.runtime.miniapp.core

import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.add
import com.tencent.kuikly.core.render.web.collection.map.get
import com.tencent.kuikly.core.render.web.runtime.miniapp.const.ShortCutsConst
import com.tencent.kuikly.core.render.web.runtime.miniapp.const.TransformConst
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniElement
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniElementUtil
import com.tencent.kuikly.core.render.web.runtime.miniapp.dom.MiniSpanElement
import kotlin.js.Json
import kotlin.js.json

typealias UploadCallBack = () -> Any?

/**
 * Update types
 */
enum class UpdateType {
    SELF, // Update self node
    CHILD, // Update child node
    STYLE, // Update own style
    ATTR // Update own attributes
}

/**
 * Information class carried by update task
 */
data class UpdatePayload(
    // Complete path for this update
    var path: String,
    // Value callback for this update, it's a function, returns the specific data for update
    val value: UploadCallBack,
    // Whether it's an update of customWrapper node
    val customWrapper: Boolean = false,
    val sid: String,
    // Type of this update
    val updateType: UpdateType,
    // RawPath for this update, except for updating self node, other update tasks end with *.[0].st, *.[0].p, *.[0].cn, etc.
    // Here will remove the last .st, .cn .p to calculate if this update task can be discarded
    val updateRawPath: String,
    // Callback marking that the update task has been consumed
    val onConsume: UploadCallBack? = null,
)

/**
 * Convert mini program DOM to setData data that can be used by mini program
 */
object Transform {
    // The Key to get node name on the template, through this Key, get the final template node type alias on usedComponentsAlias
    private const val TEMPLATE_NODE_NAME_KEY = "_num"

    // Mini program component aliases
    val componentsAlias = js(
        """
        {
          image: {_num: '1', lazyLoad: 'p0', mode: 'p1', showMenuByLongpress: 'p2', src: 'p3', webp: 'p4'},
          text: {_num: '6', decode: 'p0', maxLines: 'p1', overflow: 'p2', selectable: 'p3', space: 'p4', userSelect: 'p5'},
          'static-text': { _num: '4', decode: 'p0', maxLines: 'p1', overflow: 'p2', selectable: 'p3', space: 'p4', userSelect: 'p5'},
          'static-view': { _num: '5', class: 'cl', animation: 'p0', hoverClass: 'p1', hoverStartTime: 'p2', hoverStayTime: 'p3', hoverStopPropagation: 'p4' },
          view: {
            class: 'cl',
            _num: '7',
            animation: 'p0',
            hoverClass: 'p1',
            hoverStartTime: 'p2',
            hoverStayTime: 'p3',
            hoverStopPropagation: 'p4',
          },
          '#text': {_num: '8'},
          block: {_num: '12'},
          canvas: {_num: '15', canvasId: 'p0', disableScroll: 'p1', type: 'p2'},
          input: {
            _num: '29',
            adjustPosition: 'p0',
            alwaysEmbed: 'p1',
            autoFill: 'p2',
            confirmHold: 'p3',
            confirmType: 'p4',
            cursor: 'p5',
            cursorSpacing: 'p6',
            disabled: 'p7',
            holdKeyboard: 'p8',
            maxlength: 'p9',
            name: 'p10',
            password: 'p11',
            placeholder: 'p12',
            placeholderClass: 'p13',
            placeholderStyle: 'p14',
            safePasswordCertPath: 'p15',
            safePasswordCustomHash: 'p16',
            safePasswordLength: 'p17',
            safePasswordNonce: 'p18',
            safePasswordSalt: 'p19',
            safePasswordTimeStamp: 'p20',
            selectionEnd: 'p21',
            selectionStart: 'p22',
            type: 'p23',
            value: 'p24',
          },
          'movable-area': {_num: '38', scaleArea: 'p0'},
          'movable-view': {
            _num: '39',
            animation: 'p0',
            damping: 'p1',
            direction: 'p2',
            disabled: 'p3',
            friction: 'p4',
            height: 'p5',
            inertia: 'p6',
            outOfBounds: 'p7',
            scale: 'p8',
            scaleMax: 'p9',
            scaleMin: 'p10',
            scaleValue: 'p11',
            width: 'p12',
            x: 'p13',
            y: 'p14',
          },
          'rich-text': {_num: '56', nodes: 'p0', space: 'p1', userSelect: 'p2'},
          'static-rich-text': {_num: '57', nodes: 'p0', space: 'p1', userSelect: 'p2'},
          'scroll-view': {
            _num: '59',
            animation: 'p0',
            bounces: 'p1',
            cacheExtent: 'p2',
            clip: 'p3',
            enableBackToTop: 'p4',
            enableFlex: 'p5',
            enablePassive: 'p6',
            enhanced: 'p7',
            fastDeceleration: 'p8',
            lowerThreshold: 'p9',
            minDragDistance: 'p10',
            padding: 'p11',
            pagingEnabled: 'p12',
            refresherBackground: 'p13',
            refresherBallisticRefreshEnabled: 'p14',
            refresherDefaultStyle: 'p15',
            refresherEnabled: 'p16',
            refresherThreshold: 'p17',
            refresherTriggered: 'p18',
            refresherTwoLevelCloseThreshold: 'p19',
            refresherTwoLevelEnabled: 'p20',
            refresherTwoLevelPinned: 'p21',
            refresherTwoLevelScrollEnabled: 'p22',
            refresherTwoLevelThreshold: 'p23',
            refresherTwoLevelTriggered: 'p24',
            reverse: 'p25',
            scrollAnchoring: 'p26',
            scrollIntoView: 'p27',
            scrollIntoViewAlignment: 'p28',
            scrollIntoViewWithinExtent: 'p29',
            scrollLeft: 'p30',
            scrollTop: 'p31',
            scrollWithAnimation: 'p32',
            scrollX: 'p33',
            scrollY: 'p34',
            showScrollbar: 'p35',
            type: 'p36',
            upperThreshold: 'p37',
            usingSticky: 'p38',
          },
          textarea: {
            _num: '71',
            adjustPosition: 'p0',
            autoFocus: 'p1',
            autoHeight: 'p2',
            confirmHold: 'p3',
            confirmType: 'p4',
            cursor: 'p5',
            cursorSpacing: 'p6',
            disableDefaultPadding: 'p7',
            disabled: 'p8',
            fixed: 'p9',
            holdKeyboard: 'p10',
            maxLength: 'p11',
            name: 'p12',
            placeholder: 'p13',
            placeholderClass: 'p14',
            placeholderStyle: 'p15',
            selectionEnd: 'p16',
            selectionStart: 'p17',
            showConfirmBar: 'p18',
            value: 'p19',
          },
          video: {
            _num: '72',
            adUnitId: 'p0',
            animation: 'p1',
            autoPauseIfNavigate: 'p2',
            autoPauseIfOpenNative: 'p3',
            autoplay: 'p4',
            backgroundPoster: 'p5',
            certificateUrl: 'p6',
            controls: 'p7',
            danmuBtn: 'p8',
            danmuList: 'p9',
            direction: 'p10',
            duration: 'p11',
            enableAutoRotation: 'p12',
            enableDanmu: 'p13',
            enablePlayGesture: 'p14',
            enableProgressGesture: 'p15',
            initialTime: 'p16',
            isDrm: 'p17',
            isLive: 'p18',
            licenseUrl: 'p19',
            loop: 'p20',
            muted: 'p21',
            objectFit: 'p22',
            pageGesture: 'p23',
            pictureInPictureMode: 'p24',
            playBtnPosition: 'p25',
            poster: 'p26',
            posterForCrawler: 'p27',
            preferredPeakBitRate: 'p28',
            provisionUrl: 'p29',
            referrerPolicy: 'p30',
            showBackgroundPlaybackButton: 'p31',
            showBottomProgress: 'p32',
            showCastingButton: 'p33',
            showCenterPlayBtn: 'p34',
            showFullscreenBtn: 'p35',
            showMuteBtn: 'p36',
            showPlayBtn: 'p37',
            showProgress: 'p38',
            showScreenLockButton: 'p39',
            showSnapshotButton: 'p40',
            src: 'p41',
            title: 'p42',
            vslideGesture: 'p43',
            vslideGestureInFullscreen: 'p44',
          }
        }
    """
    )

    fun hydrate(element: MiniElement): Json {
        // For some nodes, node names are different in different situations, and some specific operations need to be done when converting data
        val nodeName = element.onTransformData()
        // Get mapping from node attributes to template attributes
        val usedComponentsAlias = componentsAlias[nodeName]
        // Pure text type, special handling, just return the content of the text
        if (isText(element)) {
            val textNode = element.unsafeCast<MiniSpanElement>()
            return json(
                ShortCutsConst.SID to element.innerId,
                ShortCutsConst.TEXT to textNode.textContent,
                ShortCutsConst.NODE_NAME to usedComponentsAlias[TEMPLATE_NODE_NAME_KEY]
            )
        }

        val data = json(
            ShortCutsConst.NODE_NAME to usedComponentsAlias[TEMPLATE_NODE_NAME_KEY],
            ShortCutsConst.SID to element.innerId,
        )

        val propsKeys = element.props

        propsKeys.forEach { value, prop ->
            val propInCamelCase = NativeApi.toCamelCase(prop.unsafeCast<String>())
            if (
                prop != TransformConst.STYLE &&
                prop != TransformConst.ID
            ) {
                val aliaName = usedComponentsAlias[propInCamelCase]
                if (aliaName != "") {
                    data[aliaName.unsafeCast<String>()] = value
                } else {
                    data[propInCamelCase] = value
                }
            }
        }

        val childNodesArray: JsArray<Json> = JsArray()
        element.childNodes.forEach { item ->
            childNodesArray.add(hydrate(item.unsafeCast<MiniElement>()))
        }

        data[ShortCutsConst.CHILD_NODE] = childNodesArray

        // todo kuikly's style setting method will result in a large amount of style content, and the amount of data set by setData each time will be very large, this can be optimized
        val cssText = element.style.cssText
        data[ShortCutsConst.STYLE] = cssText

        // If needCustomWrapper is true, it will be wrapped for local rendering to improve performance
        if (element.needCustomWrapper == true) {
            val tempCn: JsArray<Json> = JsArray<Json>().apply {
                add(data)
            }

            return json(
                ShortCutsConst.SID to "custom-${element.innerId}",
                ShortCutsConst.NODE_NAME to TransformConst.CUSTOM_WRAPPER,
                ShortCutsConst.CHILD_NODE to tempCn,
            )
        }

        return data
    }

    fun addComponentsAlias(key: String, value: dynamic) {
        if (!componentsAlias[key].unsafeCast<Boolean>()) {
            componentsAlias[key] = value
        }
    }

    /**
     * Determine if it's a pure text node
     */
    private fun isText(element: MiniElement): Boolean =
        element.nodeType == MiniElementUtil.TEXT_NODE
}
