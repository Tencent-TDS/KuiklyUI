package com.tencent.kuikly.core.render.web.runtime.miniapp.dom

import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.add
import com.tencent.kuikly.core.render.web.collection.array.get
import com.tencent.kuikly.core.render.web.collection.array.remove
import com.tencent.kuikly.core.render.web.collection.map.JsMap
import com.tencent.kuikly.core.render.web.collection.map.get
import com.tencent.kuikly.core.render.web.collection.map.set
import com.tencent.kuikly.core.render.web.expand.components.KRRichTextView
import com.tencent.kuikly.core.render.web.ktx.Frame
import com.tencent.kuikly.core.render.web.ktx.pxToFloat
import com.tencent.kuikly.core.render.web.ktx.toPxF
import com.tencent.kuikly.core.render.web.runtime.miniapp.MiniGlobal
import com.tencent.kuikly.core.render.web.runtime.miniapp.const.ShortCutsConst
import com.tencent.kuikly.core.render.web.runtime.miniapp.const.TransformConst
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.NativeApi
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.Transform
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.UpdatePayload
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.UpdateType
import com.tencent.kuikly.core.render.web.runtime.miniapp.processor.RichTextProcessor
import com.tencent.kuikly.core.render.web.utils.Log
import kotlin.js.Json
import kotlin.js.Promise

/**
 * Utility operations for mini program nodes
 */
object MiniElementUtil {
    private const val INNER_ID_PREFIX = "inner"

    // Must add prefix, pure numeric ids will cause issues in mini program
    const val SER_ID_PREFIX = "mini-"
    const val ELEMENT_NODE = 1
    const val TEXT_NODE = 3

    init {
        MiniGlobal.globalThis.incrementId = 0
    }

    /**
     * Internally maintained auto-incrementing id
     */
    fun generatorId(): String {
        MiniGlobal.globalThis.incrementId = MiniGlobal.globalThis.incrementId + 1
        return INNER_ID_PREFIX + MiniGlobal.globalThis.incrementId.unsafeCast<Int>()
    }
}

/**
 * Mini program base element
 */
open class MiniElement(var nodeName: String, val nodeType: Int) {
    // User set id
    private var uid = ""

    // Placeholder ImageView list
    private var imageSpanViewList: JsArray<MiniElement> = JsArray()

    // border width ratio with width and height, too close means used as border
    private val borderWithSizeRatio = 5

    // offset size
    private var innerOffsetWidth: Float? = null
    private var innerOffsetHeight: Float? = null

    // Event handler type
    private val miniEvent: MiniEvent by lazy {
        MiniEvent()
    }

    private var cacheCurrentContext: dynamic = null

    // Implement node properties
    val props: JsMap<String, dynamic> = JsMap()

    // Internally generated id
    var innerId = MiniElementUtil.generatorId()

    // Whether to use customWrapper, if true will wrap with customWrapper for subsequent partial refresh
    // This value cannot be changed after setting, otherwise currentContext will be abnormal
    var needCustomWrapper: Boolean? = false

    // When current node is updating, child node update tasks can be ignored
    var isLockChildUpdate: Boolean = false

    // Check if events have been added
    var hasAddEventListener: Boolean = false

    // element raw left value
    @JsName("rawLeft")
    var rawLeft: Double? = null
    // element raw top value
    @JsName("rawTop")
    var rawTop: Double? = null

    // Get node's path, used as json key for setData
    open val elementPath: String
        get() {
            val parentNode = this.parentElement
            var path = ""
            if (parentNode != null) {
                val list = parentNode.childNodes
                val indexOfNode = list.indexOf(this)
                path = parentNode.unsafeCast<MiniElement>().elementPath.let {
                    "$it.${ShortCutsConst.CHILD_NODE}.[$indexOfNode]"
                }
            }
            return path
        }

    open val rootElement: MiniRootElement?
        get() = parentElement.unsafeCast<MiniElement?>()?.rootElement

    init {
        MiniElementManage.setElement(innerId, this.unsafeCast<MiniElement>())
    }

    @JsName("id")
    var id: String
        get() = uid
        set(value) {
            val newId = MiniElementUtil.SER_ID_PREFIX + value
            uid = newId
            MiniElementManage.deleteElement(innerId)
            innerId = newId
            MiniElementManage.setElement(uid, this)
        }

    // Style properties
    @JsName("style")
    open val style: MiniCssStyleDeclaration by lazy {
        MiniCssStyleDeclaration(this)
    }

    @JsName("classList")
    val classList: MiniClassList = MiniClassList(this)

    @JsName("scrollTop")
    var scrollTop: Double = 0.0

    @JsName("scrollLeft")
    var scrollLeft: Double = 0.0

    @JsName("offsetWidth")
    var offsetWidth: Float
        get() {
            if (innerOffsetWidth != null) {
                return innerOffsetWidth!!
            }
            return style.width.pxToFloat()
        }
        set(value) {
            innerOffsetWidth = value
        }

    @JsName("offsetHeight")
    var offsetHeight: Float
        get() {
            if (innerOffsetHeight != null) {
                return innerOffsetHeight!!
            }
            return style.height.pxToFloat()
        }
        set(value) {
            innerOffsetHeight = value
        }

    @JsName("innerHTML")
    open var innerHTML: String = ""

    @JsName("innerText")
    open var innerText: String = ""

    // Parent node
    @JsName("parentNode")
    var parentNode: MiniElement? = null

    @JsName("parentElement")
    val parentElement: MiniElement?
        get() = parentNode

    @JsName("childNodes")
    val childNodes: JsArray<MiniElement> = JsArray()

    @JsName("childElementCount")
    val childElementCount: Int
        get() = childNodes.length

    @JsName("firstElementChild")
    val firstElementChild: MiniElement?
        get() {
            return if (childNodes.length > 0) {
                childNodes[0]
            } else {
                null
            }
        }

    /**
     * If parent node is in update queue, child node updates can be ignored
     */
    fun checkNeedUpdate(): Boolean {
        var currentElement = this.parentElement.unsafeCast<MiniElement?>()
        var needUpload = true
        while (currentElement != null) {
            if (currentElement.isLockChildUpdate) {
                needUpload = false
                break
            }
            currentElement = currentElement.parentElement.unsafeCast<MiniElement?>()
        }
        return needUpload
    }

    /**
     * Handle node attribute updates
     */
    fun updateAttribute(qualifiedName: String, value: dynamic) {
        rootElement?.let {
            val usedNodeName = this.onTransformData()
            val usedComponentsAlias = Transform.componentsAlias[usedNodeName]
            val qualifiedNameInCamelCase = NativeApi.toCamelCase(qualifiedName)
            val aliaName = usedComponentsAlias[qualifiedNameInCamelCase]
            if (aliaName != null) {
                val usedPath = elementPath
                val path = "${usedPath}.${aliaName}"
                val updatePayload = UpdatePayload(
                    path = path,
                    value = { value },
                    updateType = UpdateType.ATTR,
                    updateRawPath = usedPath,
                    customWrapper = needCustomWrapper == true,
                    sid = innerId
                )
                enqueueUpdate(updatePayload)
            }
        }
    }

    /**
     * Read attribute
     */
    @JsName("getAttribute")
    fun getAttribute(qualifiedName: String): Any? {
        if (qualifiedName == TransformConst.STYLE) {
            return style.cssText
        }
        return props[qualifiedName]
    }

    /**
     * Set attribute
     */
    @JsName("setAttribute")
    fun setAttribute(qualifiedName: String, value: Any) {
        if (props[qualifiedName] == value) {
            return
        }
        props[qualifiedName] = value
        updateAttribute(qualifiedName, value)
    }

    /**
     * For attributes like scroll position that need to be set even if memory value is the same,
     * use this interface instead of setAttribute
     */
    fun setAttributeForce(qualifiedName: String, value: Any) {
        props[qualifiedName] = value
        updateAttribute(qualifiedName, value)
    }

    /**
     * Remove attribute
     */
    @JsName("removeAttribute")
    fun removeAttribute(qualifiedName: String) {
        if (this.props.has(qualifiedName)) {
            this.props.delete(qualifiedName)
            updateAttribute(qualifiedName, "")
        }
    }

    @JsName("insertBefore")
    fun insertBefore(node: MiniElement, child: MiniElement?): MiniElement {
        var index = 0

        val ele = node.unsafeCast<MiniElement>()
        ele.parentNode = this

        if (child != null) {
            index = childNodes.indexOf(child)
            childNodes.add(index, ele)
        } else {
            childNodes.add(ele)
        }

        val childNodesLength = childNodes.length

        if (this.rootElement != null) {
            if (child == null) {
                val isOnlyChild = childNodesLength == 1
                if (isOnlyChild) {
                    this.updateChildNodes()
                } else {
                    val newChildElement = node.unsafeCast<MiniElement>()
                    val usedPath = newChildElement.elementPath
                    newChildElement.isLockChildUpdate = true
                    enqueueUpdate(UpdatePayload(
                        path = usedPath,
                        value = hydrate(newChildElement),
                        customWrapper = newChildElement.needCustomWrapper == true,
                        sid = innerId,
                        updateType = UpdateType.SELF,
                        updateRawPath = usedPath,
                        onConsume = {
                            newChildElement.isLockChildUpdate = false
                            true
                        }
                    ))
                }
            } else {
                val mark = childNodesLength * 2 / 3
                if (mark > index) {
                    this.updateChildNodes(false)
                } else {
                    this.updateSingleChild(index)
                }
            }
        }

        // Handle special case of rich text placeholder image insertion
        handleRichTextImageSpanInsert(ele)

        return ele
    }

    @JsName("appendChild")
    fun appendChild(node: MiniElement): MiniElement = insertBefore(node, null)

    @JsName("removeChild")
    fun removeChild(child: MiniElement): MiniElement {
        val index = childNodes.indexOf(child)
        if (index != -1) {
            this.childNodes.remove(child)
            child.unsafeCast<MiniElement>().parentNode = null
        }
        MiniElementManage.removeNodeTree(child)
        if (this.rootElement != null) {
            this.updateChildNodes(false)
        }
        return child
    }

    /**
     * Update child nodes
     */
    fun updateChildNodes(isClean: Boolean = false) {
        if (!checkNeedUpdate()) {
            return
        }
        val childNodesArray: JsArray<dynamic> = JsArray()
        val cleanChildNodes = {
            isLockChildUpdate = false
            childNodesArray
        }
        val renderChildNode = {
            isLockChildUpdate = false
            childNodes.forEach { item ->
                val element = item.unsafeCast<MiniElement>()
                childNodesArray.add(hydrate(element)())
            }
            childNodesArray
        }
        isLockChildUpdate = true
        val usedPath = elementPath
        enqueueUpdate(UpdatePayload(
            path = "${usedPath}.${ShortCutsConst.CHILD_NODE}",
            value = if (isClean) {
                cleanChildNodes
            } else {
                renderChildNode
            },
            customWrapper = this.unsafeCast<MiniElement>().needCustomWrapper == true,
            sid = innerId,
            updateType = UpdateType.CHILD,
            updateRawPath = usedPath,
            onConsume = {
                this.isLockChildUpdate = false
                true
            }
        ))
    }

    /**
     * Push to update queue
     * @param payload Update task object
     */
    open fun enqueueUpdate(payload: UpdatePayload) {
        rootElement?.enqueueUpdate(
            payload
        )
    }

    private fun getEventRealType(type: String): String {
        return when (type) {
            "click" -> {
                "tap"
            }
            else -> {
                type
            }
        }
    }

    /**
     * Add event listener
     */
    @JsName("addEventListener")
    open fun addEventListener(type: String, callback: EventHandler, options: dynamic = null) {
        hasAddEventListener = true
        miniEvent.addEventListener(getEventRealType(type), callback, options)
    }

    /**
     * Remove event listener
     */
    @JsName("removeEventListener")
    fun removeEventListener(type: String, callback: EventHandler?) {
        miniEvent.removeEventListener(getEventRealType(type), callback)
    }

    /**
     * Dispatch event
     */
    fun dispatchEvent(event: dynamic): Boolean = miniEvent.dispatchEvent(event)

    open fun onTransformData(): String = nodeName

    open fun setFrame(frame: Frame) {
        // Since terminal position data's offset is relative to parent element,
        // setting element's position to absolute in web
        // will make it offset relative to parent element
        style.position = "absolute"
        // Left offset
        val left = frame.x
        // Top offset
        val top = frame.y
        // Since kuikly's layout engine calculates element's top and left using element's width and height
        // When element has border, in actual rendering, border will occupy content space
        // This causes child elements' top/left settings to make actual display position offset right or down
        // For this case, need to subtract border width from top/left values
        style.left = left.toPxF()
        style.top = top.toPxF()
        style.width = frame.width.toPxF()
        style.height = frame.height.toPxF()
    }

    /**
     * Mini program can't get getBoundingClientRect synchronously
     */
    fun getBoundingClientRectPromise(): Promise<dynamic> {
        return Promise { resolve, _ ->
            val query = getCurrentContext().createSelectorQuery()
            query.select("#" + this.id).boundingClientRect { res ->
                resolve(res)
            }.exec().unsafeCast<Unit>()
        }
    }

    /**
     * force update child style
     */
    @JsName("forceUpdateChildrenStyle")
    fun forceUpdateChildrenStyle() {
        childNodes.forEach { childNode ->
            childNode.style.unsafeCast<MiniCssStyleDeclaration>().forceUpdate()
        }
    }

    /**
     * Get current component's context, if in custom component, need to return custom component reference
     * Otherwise return global namespace like wx or qq
     */
    private fun getCurrentContext(): dynamic {
        if (parentElement == null) {
            Log.warn("getCurrentContext call when parentElement is null")
            return NativeApi.plat
        }

        if (cacheCurrentContext != null) {
            return cacheCurrentContext
        }
        var pElement = parentElement.unsafeCast<MiniElement?>()
        var cusWrapperElement: MiniElement? = null
        while (pElement != null) {
            if (pElement.needCustomWrapper == true) {
                cusWrapperElement = pElement
                break
            }
            pElement = pElement.parentElement.unsafeCast<MiniElement?>()
        }

        val ctx = if (cusWrapperElement != null) {
            MiniGlobal.globalThis.customWrapperCache.get("custom-${cusWrapperElement.innerId}")
        } else {
            NativeApi.plat
        }
        cacheCurrentContext = ctx

        return ctx
    }

    private fun hydrate(element: MiniElement): () -> Json {
        return {
            Transform.hydrate(element)
        }
    }

    private fun updateSingleChild(index: Int) {
        if (!checkNeedUpdate()) {
            return
        }
        childNodes.forEach { childNode, childIndex ->
            if (!(index > 0 && childIndex < index)) {
                val currentElement = childNode.unsafeCast<MiniElement>()
                val usedPath = currentElement.elementPath
                currentElement.isLockChildUpdate = true

                enqueueUpdate(
                    UpdatePayload(
                        path = usedPath,
                        value = hydrate(currentElement),
                        customWrapper = currentElement.needCustomWrapper == true,
                        sid = innerId,
                        updateType = UpdateType.SELF,
                        updateRawPath = usedPath,
                        onConsume = {
                            currentElement.isLockChildUpdate = false
                            true
                        }
                    )
                )
            }
        }
    }

    /**
     * Save placeholder image view
     */
    private fun saveImageSpanView(view: MiniElement) {
        val hasElement = imageSpanViewList.indexOf(view)
        if (hasElement == -1) {
            // Element not inserted yet, insert it
            imageSpanViewList.push(view)
        }
    }

    /**
     * Handle special case of rich text placeholder image insertion,
     * need to insert placeholder image into placeholder span
     */
    private fun handleRichTextImageSpanInsert(view: MiniElement) {
        // Placeholder image structure is special, only has one child image node with fixed image style
        if (
            view.childNodes.length == 1 &&
            view.firstElementChild is MiniImageElement &&
            view.firstElementChild?.style?.cssText == RICH_TEXT_PLACEHOLDER_IMAGE_STYLE
        ) {
            // Save placeholder image node
            saveImageSpanView(view)
            // Insert image placeholder, first get placeholder image index
            val imageIndex = imageSpanViewList.indexOf(view)
            // Calculated index
            var usedIndex = 0
            // Traverse child nodes, handle image placeholders
            childNodes.forEach { item ->
                if (item is MiniParagraphElement) {
                    val krRichTextView = item.asDynamic().krRichTextView.unsafeCast<KRRichTextView?>()
                    if (krRichTextView != null && krRichTextView.isRichText && krRichTextView.imageSpanCount > 0) {
                        // Only process rich text nodes containing placeholder image nodes
                        // Note each rich text contains
                        for (index in 1 until krRichTextView.imageSpanCount + 1) {
                            if (imageIndex < usedIndex + index) {
                                // If placeholder image belongs to current rich text node, process it
                                // Note the index passed here is the placeholder image index in rich text
                                RichTextProcessor.insertPlaceHolderImageView(
                                    krRichTextView,
                                    view,
                                    index - 1
                                )
                                // Processing complete, return
                                return@forEach
                            }
                            // Processing complete, add processed index
                            usedIndex += 1
                        }
                    }
                }
            }
        }
    }

    companion object {
        // Rich text placeholder image special style
        private const val RICH_TEXT_PLACEHOLDER_IMAGE_STYLE =
            "box-sizing: border-box; width: 100%; height: 100%; display: block;"
    }
}

/**
 * Mini program class list implementation
 */
class MiniClassList(private val element: MiniElement) {
    private val classSet = mutableSetOf<String>()
    
    private fun updateClassAttribute() {
        val classString = classSet.joinToString(" ")
        element.setAttribute("class", classString)
    }

    @JsName("add")
    fun add(vararg tokens: String) {
        tokens.forEach { token ->
            if (token.isNotEmpty()) {
                classSet.add(token)
            }
        }
        updateClassAttribute()
    }
    
    fun remove(vararg tokens: String) {
        tokens.forEach { token ->
            classSet.remove(token)
        }
        updateClassAttribute()
    }
    
    fun toggle(token: String, force: Boolean? = null): Boolean {
        return when {
            force == true -> {
                classSet.add(token)
                updateClassAttribute()
                true
            }
            force == false -> {
                classSet.remove(token)
                updateClassAttribute()
                false
            }
            else -> {
                if (classSet.contains(token)) {
                    classSet.remove(token)
                    updateClassAttribute()
                    false
                } else {
                    classSet.add(token)
                    updateClassAttribute()
                    true
                }
            }
        }
    }
    
    fun contains(token: String): Boolean {
        return classSet.contains(token)
    }
    
    fun replace(token: String, newToken: String): Boolean {
        return if (classSet.contains(token)) {
            classSet.remove(token)
            classSet.add(newToken)
            updateClassAttribute()
            true
        } else {
            false
        }
    }
    
    fun supports(token: String): Boolean {
        return true
    }
    
    val length: Int
        get() = classSet.size
    
    fun item(index: Int): String? {
        return if (index >= 0 && index < classSet.size) {
            classSet.elementAt(index)
        } else {
            null
        }
    }
    
    override fun toString(): String {
        return classSet.joinToString(" ")
    }
}
