package com.tencent.kuikly.core.render.web.runtime.miniapp.dom

import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.add
import com.tencent.kuikly.core.render.web.collection.map.JsMap
import com.tencent.kuikly.core.render.web.collection.map.get
import com.tencent.kuikly.core.render.web.collection.map.set
import com.tencent.kuikly.core.render.web.collection.set.JsSet
import com.tencent.kuikly.core.render.web.ktx.pxToDouble
import com.tencent.kuikly.core.render.web.ktx.toPxF
import com.tencent.kuikly.core.render.web.runtime.miniapp.const.ShortCutsConst
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.UpdatePayload
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.UpdateType
import kotlin.math.round
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

typealias OnStyleSet = (styleName: String, value: Any)-> Boolean
typealias OnStyleGet = (styleName: String, defaultValue: Any)-> Any

/**
 * Mini program host style definition implementation
 */
class MiniCssStyleDeclaration(private val miniElement: MiniElement) {
    // Map for storing style data
    private val styleMap = JsMap<String, dynamic>()

    // Style property delegate instance
    private val cssPropertyDelegate = CssPropertyDelegate()

    // Set of actually used style names, for later serialization, only serialize these used styles
    private val usedStyleProp: JsSet<String> = JsSet()

    // Custom delegate logic class
    private class CssPropertyDelegate :
        ReadWriteProperty<MiniCssStyleDeclaration, String> {
        override fun getValue(thisRef: MiniCssStyleDeclaration, property: KProperty<*>): String {
            val defaultValue = thisRef.getPropertyValue(property.name).unsafeCast<String?>() ?: ""
            var retValue = defaultValue

            thisRef.onStyleGet?.let {
                retValue =  it(property.name, defaultValue).unsafeCast<String?>() ?: ""
            }

            return retValue
        }

        override fun setValue(
            thisRef: MiniCssStyleDeclaration,
            property: KProperty<*>,
            value: String
        ) {
            var needDefaultSet = true

            thisRef.onStyleSet?.let {
                needDefaultSet = it(property.name, value)
            }

            if (needDefaultSet) {
                thisRef.setProperty(property.name, value)
            }
        }
    }

    var onStyleSet: OnStyleSet? = null
    var onStyleGet: OnStyleGet? = null

    private var cachedCssText: String? = null

    @JsName("cssText")
    val cssText: String
        get() = cachedCssText ?: buildCssText().also { cachedCssText = it }
    @JsName("cssFloat")
    var cssFloat: String by cssPropertyDelegate
    @JsName("alignContent")
    var alignContent: String by cssPropertyDelegate
    @JsName("alignItems")
    var alignItems: String by cssPropertyDelegate
    @JsName("alignSelf")
    var alignSelf: String by cssPropertyDelegate
    @JsName("animation")
    var animation: String by cssPropertyDelegate
    @JsName("animationDelay")
    var animationDelay: String by cssPropertyDelegate
    @JsName("animationDirection")
    var animationDirection: String by cssPropertyDelegate
    @JsName("animationDuration")
    var animationDuration: String by cssPropertyDelegate
    @JsName("animationFillMode")
    var animationFillMode: String by cssPropertyDelegate
    @JsName("animationIterationCount")
    var animationIterationCount: String by cssPropertyDelegate
    @JsName("animationName")
    var animationName: String by cssPropertyDelegate
    @JsName("animationPlayState")
    var animationPlayState: String by cssPropertyDelegate
    @JsName("animationTimingFunction")
    var animationTimingFunction: String by cssPropertyDelegate
    @JsName("background")
    var background: String by cssPropertyDelegate
    @JsName("backgroundAttachment")
    var backgroundAttachment: String by cssPropertyDelegate
    @JsName("backgroundClip")
    var backgroundClip: String by cssPropertyDelegate
    @JsName("webkitBackgroundClip")
    var webkitBackgroundClip: String by cssPropertyDelegate
    @JsName("webkitBackdropFilter")
    var webkitBackdropFilter: String by cssPropertyDelegate
    @JsName("backdropFilter")
    var backdropFilter: String by cssPropertyDelegate
    @JsName("backgroundColor")
    var backgroundColor: String by cssPropertyDelegate
    @JsName("backgroundImage")
    var backgroundImage: String by cssPropertyDelegate
    @JsName("backgroundOrigin")
    var backgroundOrigin: String by cssPropertyDelegate
    @JsName("backgroundPosition")
    var backgroundPosition: String by cssPropertyDelegate
    @JsName("backgroundRepeat")
    var backgroundRepeat: String by cssPropertyDelegate
    @JsName("backgroundSize")
    var backgroundSize: String by cssPropertyDelegate
    @JsName("border")
    var border: String by cssPropertyDelegate
    @JsName("borderBottom")
    var borderBottom: String by cssPropertyDelegate
    @JsName("borderBottomColor")
    var borderBottomColor: String by cssPropertyDelegate
    @JsName("borderBottomLeftRadius")
    var borderBottomLeftRadius: String by cssPropertyDelegate
    @JsName("borderBottomRightRadius")
    var borderBottomRightRadius: String by cssPropertyDelegate
    @JsName("borderBottomStyle")
    var borderBottomStyle: String by cssPropertyDelegate
    @JsName("borderBottomWidth")
    var borderBottomWidth: String by cssPropertyDelegate
    @JsName("borderCollapse")
    var borderCollapse: String by cssPropertyDelegate
    @JsName("borderColor")
    var borderColor: String = ""
        get() = field
        set(value) {
            field = value
            setProperty("border", "$borderWidth $borderStyle $value")
        }
    @JsName("borderLeft")
    var borderLeft: String by cssPropertyDelegate
    @JsName("borderLeftColor")
    var borderLeftColor: String by cssPropertyDelegate
    @JsName("borderLeftStyle")
    var borderLeftStyle: String by cssPropertyDelegate
    @JsName("borderLeftWidth")
    var borderLeftWidth: String by cssPropertyDelegate
    @JsName("borderRadius")
    var borderRadius: String by cssPropertyDelegate
    @JsName("borderRight")
    var borderRight: String by cssPropertyDelegate
    @JsName("borderRightColor")
    var borderRightColor: String by cssPropertyDelegate
    @JsName("borderRightStyle")
    var borderRightStyle: String by cssPropertyDelegate
    @JsName("borderRightWidth")
    var borderRightWidth: String by cssPropertyDelegate
    @JsName("borderSpacing")
    var borderSpacing: String by cssPropertyDelegate
    @JsName("borderStyle")
    var borderStyle: String = ""
    @JsName("borderTop")
    var borderTop: String by cssPropertyDelegate
    @JsName("borderTopColor")
    var borderTopColor: String by cssPropertyDelegate
    @JsName("borderTopLeftRadius")
    var borderTopLeftRadius: String by cssPropertyDelegate
    @JsName("borderTopRightRadius")
    var borderTopRightRadius: String by cssPropertyDelegate
    @JsName("borderTopStyle")
    var borderTopStyle: String by cssPropertyDelegate
    @JsName("borderTopWidth")
    var borderTopWidth: String by cssPropertyDelegate
    @JsName("borderWidth")
    var borderWidth: String = ""
    @JsName("bottom")
    var bottom: String by cssPropertyDelegate
    @JsName("boxShadow")
    var boxShadow: String by cssPropertyDelegate
    @JsName("color")
    var color: String by cssPropertyDelegate
    @JsName("cursor")
    var cursor: String by cssPropertyDelegate
    @JsName("direction")
    var direction: String by cssPropertyDelegate
    @JsName("display")
    var display: String by cssPropertyDelegate
    @JsName("filter")
    var filter: String by cssPropertyDelegate
    @JsName("flex")
    var flex: String by cssPropertyDelegate
    @JsName("flexBasis")
    var flexBasis: String by cssPropertyDelegate
    @JsName("flexDirection")
    var flexDirection: String by cssPropertyDelegate
    @JsName("flexFlow")
    var flexFlow: String by cssPropertyDelegate
    @JsName("flexGrow")
    var flexGrow: String by cssPropertyDelegate
    @JsName("flexShrink")
    var flexShrink: String by cssPropertyDelegate
    @JsName("flexWrap")
    var flexWrap: String by cssPropertyDelegate
    @JsName("font")
    var font: String by cssPropertyDelegate
    @JsName("fontFamily")
    var fontFamily: String by cssPropertyDelegate
    @JsName("fontFeatureSettings")
    var fontFeatureSettings: String by cssPropertyDelegate
    @JsName("fontKerning")
    var fontKerning: String by cssPropertyDelegate
    @JsName("fontLanguageOverride")
    var fontLanguageOverride: String by cssPropertyDelegate
    @JsName("fontSize")
    var fontSize: String
        get() = getPropertyValue("fontSize").unsafeCast<String>()
        set(value) {
            // Round font size, because the font size is rounded in the core,
            // so we need to be consistent
            setProperty("fontSize", "${round(value.pxToDouble())}px")
        }
    @JsName("fontSizeAdjust")
    var fontSizeAdjust: String by cssPropertyDelegate
    @JsName("fontStretch")
    var fontStretch: String by cssPropertyDelegate
    @JsName("fontStyle")
    var fontStyle: String by cssPropertyDelegate
    @JsName("fontSynthesis")
    var fontSynthesis: String by cssPropertyDelegate
    @JsName("fontVariant")
    var fontVariant: String by cssPropertyDelegate
    @JsName("fontVariantAlternates")
    var fontVariantAlternates: String by cssPropertyDelegate
    @JsName("fontVariantCaps")
    var fontVariantCaps: String by cssPropertyDelegate
    @JsName("fontVariantEastAsian")
    var fontVariantEastAsian: String by cssPropertyDelegate
    @JsName("fontVariantLigatures")
    var fontVariantLigatures: String by cssPropertyDelegate
    @JsName("fontVariantNumeric")
    var fontVariantNumeric: String by cssPropertyDelegate
    @JsName("fontVariantPosition")
    var fontVariantPosition: String by cssPropertyDelegate
    @JsName("fontWeight")
    var fontWeight: String by cssPropertyDelegate
    @JsName("height")
    var height: String by cssPropertyDelegate
    @JsName("hyphens")
    var hyphens: String by cssPropertyDelegate
    @JsName("justifyContent")
    var justifyContent: String by cssPropertyDelegate
    @JsName("left")
    var left: String by cssPropertyDelegate
    @JsName("letterSpacing")
    var letterSpacing: String by cssPropertyDelegate
    @JsName("lineBreak")
    var lineBreak: String by cssPropertyDelegate
    @JsName("lineHeight")
    var lineHeight: String by cssPropertyDelegate
    @JsName("margin")
    var margin: String by cssPropertyDelegate
    @JsName("marginBottom")
    var marginBottom: String by cssPropertyDelegate
    @JsName("marginLeft")
    var marginLeft: String by cssPropertyDelegate
    @JsName("marginRight")
    var marginRight: String by cssPropertyDelegate
    @JsName("marginTop")
    var marginTop: String by cssPropertyDelegate
    @JsName("mask")
    var mask: String by cssPropertyDelegate
    @JsName("maskType")
    var maskType: String by cssPropertyDelegate
    @JsName("maxHeight")
    var maxHeight: String by cssPropertyDelegate
    @JsName("maxWidth")
    var maxWidth: String by cssPropertyDelegate
    @JsName("minHeight")
    var minHeight: String by cssPropertyDelegate
    @JsName("minWidth")
    var minWidth: String by cssPropertyDelegate
    @JsName("objectFit")
    var objectFit: String by cssPropertyDelegate
    @JsName("objectPosition")
    var objectPosition: String by cssPropertyDelegate
    @JsName("opacity")
    var opacity: String by cssPropertyDelegate
    @JsName("order")
    var order: String by cssPropertyDelegate
    @JsName("overflow")
    var overflow: String by cssPropertyDelegate
    @JsName("overflowX")
    var overflowX: String by cssPropertyDelegate
    @JsName("overflowY")
    var overflowY: String by cssPropertyDelegate
    @JsName("padding")
    var padding: String by cssPropertyDelegate
    @JsName("paddingBottom")
    var paddingBottom: String by cssPropertyDelegate
    @JsName("paddingLeft")
    var paddingLeft: String by cssPropertyDelegate
    @JsName("paddingRight")
    var paddingRight: String by cssPropertyDelegate
    @JsName("paddingTop")
    var paddingTop: String by cssPropertyDelegate
    @JsName("pointerEvents")
    var pointerEvents: String by cssPropertyDelegate
    @JsName("position")
    var position: String by cssPropertyDelegate
    @JsName("quotes")
    var quotes: String by cssPropertyDelegate
    @JsName("resize")
    var resize: String by cssPropertyDelegate
    @JsName("right")
    var right: String by cssPropertyDelegate
    @JsName("textAlign")
    var textAlign: String by cssPropertyDelegate
    @JsName("textAlignLast")
    var textAlignLast: String by cssPropertyDelegate
    @JsName("textCombineUpright")
    var textCombineUpright: String by cssPropertyDelegate
    @JsName("textDecoration")
    var textDecoration: String by cssPropertyDelegate
    @JsName("textDecorationColor")
    var textDecorationColor: String by cssPropertyDelegate
    @JsName("textDecorationLine")
    var textDecorationLine: String by cssPropertyDelegate
    @JsName("textDecorationStyle")
    var textDecorationStyle: String by cssPropertyDelegate
    @JsName("webkitTextStroke")
    var webkitTextStroke: String by cssPropertyDelegate
    @JsName("textIndent")
    var textIndent: String by cssPropertyDelegate
    @JsName("textJustify")
    var textJustify: String by cssPropertyDelegate
    @JsName("textOrientation")
    var textOrientation: String by cssPropertyDelegate
    @JsName("textOverflow")
    var textOverflow: String by cssPropertyDelegate
    @JsName("textShadow")
    var textShadow: String by cssPropertyDelegate
    @JsName("textTransform")
    var textTransform: String by cssPropertyDelegate
    @JsName("textUnderlinePosition")
    var textUnderlinePosition: String by cssPropertyDelegate
    @JsName("top")
    var top: String by cssPropertyDelegate
    @JsName("transform")
    var transform: String by cssPropertyDelegate
    @JsName("transformOrigin")
    var transformOrigin: String by cssPropertyDelegate
    @JsName("transformStyle")
    var transformStyle: String by cssPropertyDelegate
    @JsName("transition")
    var transition: String by cssPropertyDelegate
    @JsName("transitionDelay")
    var transitionDelay: String by cssPropertyDelegate
    @JsName("transitionDuration")
    var transitionDuration: String by cssPropertyDelegate
    @JsName("transitionProperty")
    var transitionProperty: String by cssPropertyDelegate
    @JsName("transitionTimingFunction")
    var transitionTimingFunction: String by cssPropertyDelegate
    @JsName("verticalAlign")
    var verticalAlign: String by cssPropertyDelegate
    @JsName("visibility")
    var visibility: String by cssPropertyDelegate
    @JsName("webkitMask")
    var webkitMask: String by cssPropertyDelegate
    @JsName("webkitLineClamp")
    var webkitLineClamp: String by cssPropertyDelegate
    @JsName("webkitBoxOrient")
    var webkitBoxOrient: String by cssPropertyDelegate
    @JsName("whiteSpace")
    var whiteSpace: String by cssPropertyDelegate
    @JsName("width")
    var width: String by cssPropertyDelegate
    @JsName("wordBreak")
    var wordBreak: String by cssPropertyDelegate
    @JsName("wordSpacing")
    var wordSpacing: String by cssPropertyDelegate
    @JsName("wordWrap")
    var wordWrap: String by cssPropertyDelegate
    @JsName("writingMode")
    var writingMode: String by cssPropertyDelegate
    @JsName("zIndex")
    var zIndex: String by cssPropertyDelegate
    @JsName("backfaceVisibility")
    var backfaceVisibility: String by cssPropertyDelegate
    @JsName("borderImage")
    var borderImage: String by cssPropertyDelegate
    @JsName("borderImageOutset")
    var borderImageOutset: String by cssPropertyDelegate
    @JsName("borderImageRepeat")
    var borderImageRepeat: String by cssPropertyDelegate
    @JsName("borderImageSlice")
    var borderImageSlice: String by cssPropertyDelegate
    @JsName("borderImageSource")
    var borderImageSource: String by cssPropertyDelegate
    @JsName("borderImageWidth")
    var borderImageWidth: String by cssPropertyDelegate
    @JsName("boxDecorationBreak")
    var boxDecorationBreak: String by cssPropertyDelegate
    @JsName("boxSizing")
    var boxSizing: String by cssPropertyDelegate
    @JsName("breakAfter")
    var breakAfter: String by cssPropertyDelegate
    @JsName("breakBefore")
    var breakBefore: String by cssPropertyDelegate
    @JsName("breakInside")
    var breakInside: String by cssPropertyDelegate
    @JsName("captionSide")
    var captionSide: String by cssPropertyDelegate
    @JsName("clear")
    var clear: String by cssPropertyDelegate
    @JsName("clip")
    var clip: String by cssPropertyDelegate
    @JsName("columnCount")
    var columnCount: String by cssPropertyDelegate
    @JsName("columnFill")
    var columnFill: String by cssPropertyDelegate
    @JsName("columnGap")
    var columnGap: String by cssPropertyDelegate
    @JsName("columnRule")
    var columnRule: String by cssPropertyDelegate
    @JsName("columnRuleColor")
    var columnRuleColor: String by cssPropertyDelegate
    @JsName("columnRuleStyle")
    var columnRuleStyle: String by cssPropertyDelegate
    @JsName("columnRuleWidth")
    var columnRuleWidth: String by cssPropertyDelegate
    @JsName("columnSpan")
    var columnSpan: String by cssPropertyDelegate
    @JsName("columnWidth")
    var columnWidth: String by cssPropertyDelegate
    @JsName("columns")
    var columns: String by cssPropertyDelegate
    @JsName("content")
    var content: String by cssPropertyDelegate
    @JsName("counterIncrement")
    var counterIncrement: String by cssPropertyDelegate
    @JsName("counterReset")
    var counterReset: String by cssPropertyDelegate
    @JsName("emptyCells")
    var emptyCells: String by cssPropertyDelegate
    @JsName("hangingPunctuation")
    var hangingPunctuation: String by cssPropertyDelegate
    @JsName("imageOrientation")
    var imageOrientation: String by cssPropertyDelegate
    @JsName("imageRendering")
    var imageRendering: String by cssPropertyDelegate
    @JsName("imageResolution")
    var imageResolution: String by cssPropertyDelegate
    @JsName("imeMode")
    var imeMode: String by cssPropertyDelegate
    @JsName("listStyle")
    var listStyle: String by cssPropertyDelegate
    @JsName("listStyleImage")
    var listStyleImage: String by cssPropertyDelegate
    @JsName("listStylePosition")
    var listStylePosition: String by cssPropertyDelegate
    @JsName("listStyleType")
    var listStyleType: String by cssPropertyDelegate
    @JsName("mark")
    var mark: String by cssPropertyDelegate
    @JsName("markAfter")
    var markAfter: String by cssPropertyDelegate
    @JsName("markBefore")
    var markBefore: String by cssPropertyDelegate
    @JsName("marks")
    var marks: String by cssPropertyDelegate
    @JsName("marqueeDirection")
    var marqueeDirection: String by cssPropertyDelegate
    @JsName("marqueePlayCount")
    var marqueePlayCount: String by cssPropertyDelegate
    @JsName("marqueeSpeed")
    var marqueeSpeed: String by cssPropertyDelegate
    @JsName("marqueeStyle")
    var marqueeStyle: String by cssPropertyDelegate
    @JsName("navDown")
    var navDown: String by cssPropertyDelegate
    @JsName("navIndex")
    var navIndex: String by cssPropertyDelegate
    @JsName("navLeft")
    var navLeft: String by cssPropertyDelegate
    @JsName("navRight")
    var navRight: String by cssPropertyDelegate
    @JsName("navUp")
    var navUp: String by cssPropertyDelegate
    @JsName("orphans")
    var orphans: String by cssPropertyDelegate
    @JsName("outline")
    var outline: String by cssPropertyDelegate
    @JsName("outlineColor")
    var outlineColor: String by cssPropertyDelegate
    @JsName("outlineOffset")
    var outlineOffset: String by cssPropertyDelegate
    @JsName("outlineStyle")
    var outlineStyle: String by cssPropertyDelegate
    @JsName("outlineWidth")
    var outlineWidth: String by cssPropertyDelegate
    @JsName("overflowWrap")
    var overflowWrap: String by cssPropertyDelegate
    @JsName("pageBreakAfter")
    var pageBreakAfter: String by cssPropertyDelegate
    @JsName("pageBreakBefore")
    var pageBreakBefore: String by cssPropertyDelegate
    @JsName("pageBreakInside")
    var pageBreakInside: String by cssPropertyDelegate
    @JsName("perspective")
    var perspective: String by cssPropertyDelegate
    @JsName("perspectiveOrigin")
    var perspectiveOrigin: String by cssPropertyDelegate
    @JsName("phonemes")
    var phonemes: String by cssPropertyDelegate
    @JsName("rest")
    var rest: String by cssPropertyDelegate
    @JsName("restAfter")
    var restAfter: String by cssPropertyDelegate
    @JsName("restBefore")
    var restBefore: String by cssPropertyDelegate
    @JsName("tabSize")
    var tabSize: String by cssPropertyDelegate
    @JsName("tableLayout")
    var tableLayout: String by cssPropertyDelegate
    @JsName("unicodeBidi")
    var unicodeBidi: String by cssPropertyDelegate
    @JsName("voiceBalance")
    var voiceBalance: String by cssPropertyDelegate
    @JsName("voiceDuration")
    var voiceDuration: String by cssPropertyDelegate
    @JsName("voicePitch")
    var voicePitch: String by cssPropertyDelegate
    @JsName("voicePitchRange")
    var voicePitchRange: String by cssPropertyDelegate
    @JsName("voiceRate")
    var voiceRate: String by cssPropertyDelegate
    @JsName("voiceStress")
    var voiceStress: String by cssPropertyDelegate
    @JsName("voiceVolume")
    var voiceVolume: String by cssPropertyDelegate
    @JsName("widows")
    var widows: String by cssPropertyDelegate

    /**
     * Set style property
     */
    @JsName("setProperty")
    fun setProperty(property: String, value: String?, silence: Boolean = false) {
        var propName = property
        val oldValue = styleMap[property]
        if (oldValue == value) {
            return
        }
        if (value == null) {
            propName = cssNameMap[property].unsafeCast<String>()
            removeProperty(propName)
            return
        } else {
            usedStyleProp.add(propName)
            styleMap[property] = value
        }

        if (silence) {
            return
        }

        enqueueUpdate()
    }

    /**
     * Get style property
     */
    fun getPropertyValue(property: String): dynamic {
        return styleMap[property] ?: return ""
    }

    /**
     * Enter update queue
     */
    private fun enqueueUpdate() {
        // Clear cache
        cachedCssText = null
        // Determine if the current update task needs to enter the update queue
        if (!miniElement.checkNeedUpdate() || (miniElement.parentElement == null && miniElement !is MiniRootElement)) {
            return
        }
        // Get the path needed for the update
        forceUpdate()
    }

    private fun checkIfNeedResetPosition(element: MiniElement): Boolean {
        return element.rawLeft != null && element.rawTop != null &&
                element.parentElement?.style?.borderWidth != "" &&
                element.parentElement?.style?.borderWidth?.endsWith("px") == true
    }

    private fun resetElementPosition(element: MiniElement) {
        val borderWidth = element.parentElement!!.style.borderWidth.pxToDouble()
        if (element.rawLeft != null && element.rawTop != null) {
            val newLeft = element.rawLeft!! - borderWidth
            val newTop = element.rawTop!! - borderWidth
            element.style.left = newLeft.toPxF()
            element.style.top = newTop.toPxF()
        }
    }

    // provide a method to check if need reset position
    @JsName("checkAndUpdatePosition")
    fun checkAndUpdatePosition() {
        if (checkIfNeedResetPosition(miniElement)) {
            resetElementPosition((miniElement))
        }
    }

    fun forceUpdate() {
        checkAndUpdatePosition()
        // Get the path needed for the update
        val rawElementPath = miniElement.elementPath
        miniElement.enqueueUpdate(
            UpdatePayload(
                path = "${rawElementPath}.${ShortCutsConst.STYLE}",
                value = {
                    this.cssText
                },
                updateType = UpdateType.STYLE,
                updateRawPath = rawElementPath,
                customWrapper = miniElement.needCustomWrapper == true,
                sid = miniElement.innerId
            )
        )
    }

    private fun buildCssText(): String {
        val textList: JsArray<String> = JsArray()
        usedStyleProp.forEach { cssName ->
            val cssValue = styleMap[cssName]
            if (cssValue != null && cssValue != "") {
                textList.add("${cssNameMap[cssName]}: ${cssValue};")
            }
        }
        return textList.join(" ")
    }

    /**
     * Delete style property
     */
    private fun removeProperty(property: String): dynamic {
        styleMap[property] ?: return ""
        val propName = cssNameMap[property].unsafeCast<String>()
        if (!usedStyleProp.has(propName)) {
            return ""
        }
        val propValue = styleMap[propName]
        styleMap[propName] = null
        usedStyleProp.delete(propName)
        enqueueUpdate()
        return propValue
    }

    // Currently only some styles are in cssNameMap, most styles are not supported by mini programs,
    // use object dictionary, reduce camel case to hyphenated naming calculation
    companion object {
        val cssNameMap = js(
            """
            {
              animation: 'animation',
              whiteSpace: 'white-space',
              alignItems: 'align-items',
              alignSelf: 'align-self',
              backdropFilter: 'backdrop-filter',
              webkitBackdropFilter: '-webkit-backdrop-filter',
              background: 'background',
              backgroundImage: 'background-image',
              backgroundColor: 'background-color',
              backgroundSize: 'background-size',
              backgroundClip: 'background-clip',
              webkitBackgroundClip: '-webkit-background-clip',
              border: 'border',
              borderBottom: 'border-bottom',
              borderBottomColor: 'border-bottom-color',
              wordBreak: 'word-break',
              borderBottomLeftRadius: 'border-bottom-left-radius',
              borderBottomRightRadius: 'border-bottom-right-radius',
              borderBottomStyle: 'border-bottom-style',
              borderBottomWidth: 'border-bottom-width',
              borderCollapse: 'border-collapse',
              borderColor: 'border-color',
              borderLeft: 'border-left',
              borderLeftColor: 'border-left-color',
              borderLeftStyle: 'border-left-style',
              borderLeftWidth: 'border-left-width',
              borderRadius: 'border-radius',
              borderRight: 'border-right',
              borderRightColor: 'border-right-color',
              borderRightStyle: 'border-right-style',
              borderRightWidth: 'border-right-width',
              borderSpacing: 'border-spacing',
              borderStyle: 'border-style',
              borderTop: 'border-top',
              borderTopColor: 'border-top-color',
              borderTopLeftRadius: 'border-top-left-radius',
              borderTopRightRadius: 'border-top-right-radius',
              borderTopStyle: 'border-top-style',
              borderTopWidth: 'border-top-width',
              borderWidth: 'border-width',
              bottom: 'bottom',
              boxShadow: 'box-shadow',
              boxSizing: 'box-sizing',
              clip: 'clip',
              color: 'color',
              content: 'content',
              direction: 'direction',
              display: 'display',
              filter: '-webkit-filter',
              flex: 'flex',
              flexBasis: 'flex-basis',
              flexDirection: 'flex-direction',
              flexFlow: 'flex-flow',
              flexGrow: 'flex-grow',
              flexShrink: 'flex-shrink',
              fontStyle: 'font-style',
              flexWrap: 'flex-wrap',
              font: 'font',
              fontFamily: 'font-family',
              fontSize: 'font-size',
              fontWeight: 'font-weight',
              height: 'height',
              justifyContent: 'justify-content',
              left: 'left',
              lineHeight: 'line-height',
              letterSpacing: 'letter-spacing',
              margin: 'margin',
              marginBottom: 'margin-bottom',
              marginLeft: 'margin-left',
              marginRight: 'margin-right',
              marginTop: 'margin-top',
              maxHeight: 'max-height',
              maxWidth: 'max-width',
              minHeight: 'min-height',
              minWidth: 'min-width',
              opacity: 'opacity',
              objectFit: 'object-fit',
              overflow: 'overflow',
              overflowX: 'overflow-x',
              overflowY: 'overflow-y',
              padding: 'padding',
              fontVariant: 'font-variant',
              verticalAlign: 'vertical-align',
              textIndent: 'text-indent',
              paddingBottom: 'padding-bottom',
              paddingLeft: 'padding-left',
              paddingRight: 'padding-right',
              paddingTop: 'padding-top',
              position: 'position',
              pointerEvents: 'pointer-events',
              right: 'right',
              textAlign: 'text-align',
              textOverflow: 'text-overflow',
              textShadow: 'text-shadow',
              webkitTextStroke: '-webkit-text-stroke',
              textDecoration: 'text-decoration',
              top: 'top',
              transform: 'transform',
              transformOrigin: 'transform-origin',
              transition: 'transition',
              visibility: 'visibility',
              width: 'width',
              webkitMask: '-webkit-mask',
              webkitLineClamp: '-webkit-line-clamp',
              webkitBoxOrient: '-webkit-box-orient',
              wordWrap: 'word-wrap',
              zIndex: 'z-index',
            }
        """
        )
    }
}
