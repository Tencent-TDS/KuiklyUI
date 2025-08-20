package com.tencent.kuikly.core.render.web.runtime.miniapp.dom

import com.tencent.kuikly.core.render.web.collection.array.JsArray
import com.tencent.kuikly.core.render.web.collection.array.add
import com.tencent.kuikly.core.render.web.collection.array.clear
import com.tencent.kuikly.core.render.web.collection.array.get
import com.tencent.kuikly.core.render.web.collection.map.JsMap
import com.tencent.kuikly.core.render.web.collection.map.get
import com.tencent.kuikly.core.render.web.collection.map.isNotEmpty
import com.tencent.kuikly.core.render.web.collection.map.set
import com.tencent.kuikly.core.render.web.collection.set.JsSet
import com.tencent.kuikly.core.render.web.runtime.miniapp.MiniGlobal
import com.tencent.kuikly.core.render.web.runtime.miniapp.const.ShortCutsConst
import com.tencent.kuikly.core.render.web.runtime.miniapp.const.TransformConst
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.UpdatePayload
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.UpdateType
import com.tencent.kuikly.core.render.web.scheduler.KuiklyRenderCoreContextScheduler
import kotlin.js.Json
import kotlin.js.json

/**
 * Root node of the mini program, containing key logic for updating data to the mini program via setData
 */
class MiniRootElement(
    nodeName: String = TransformConst.VIEW,
    nodeType: Int = MiniElementUtil.ELEMENT_NODE
) : MiniElement(nodeName, nodeType) {
    // Update task list
    private var updatePayloadList: JsArray<UpdatePayload> = JsArray()

    // Whether update is in progress
    private var pendingUpdate = false

    // Mini program page context
    var miniPageInstance: Any? = null

    // Root node
    override val rootElement: MiniRootElement
        get() = this

    // Node's json path
    override val elementPath: String = ShortCutsConst.ROOT_NAME

    /**
     * Enqueue update task
     */
    override fun enqueueUpdate(payload: UpdatePayload) {
        this.updatePayloadList.add(payload)
        if (!this.pendingUpdate && miniPageInstance != null) {
            this.performUpdate()
        }
    }

    /**
     * Extract numeric index from string like "[0]"
     */
    private fun extractIntFromBrackets(input: String): Int? {
        val inputText = input.asDynamic()
        // Use simple string operations to remove brackets and convert to Int
        if (inputText.startsWith("[").unsafeCast<Boolean>() && inputText.endsWith("]")
                .unsafeCast<Boolean>()
        ) {
            val numberStr = input.substring(1, inputText.length.unsafeCast<Int>() - 1)
            return js("Number")(numberStr).unsafeCast<Int?>()
        }
        return null
    }

    /**
     * Check if given index is the last index
     */
    private fun isLastIndex(index: Int, length: Int): Boolean = index == length - 1

    /**
     * Check if there's a customWrapper, if yes do partial update, get customWrapperId and update path
     */
    private fun findCustomWrapper(root: MiniElement, dataPathArr: dynamic): JsArray<String> {
        val list: JsArray<String> = dataPathArr.slice(1).unsafeCast<JsArray<String>>()
        // Calculate from root, match the nearest customWrapperId
        var currentData: Any? = root
        var customWrapperId = ""
        var splitedPath = ""

        list.forEach { item, index ->
            // Performance sensitive here, use js's own string methods
            val key = item.asDynamic()
            // If not cn or [0] string, not related to customWrapper calculation, can ignore
            var isIgnoreItem = false
            // If current data is not null, do calculation
            if (currentData != null) {
                // cn string indicates operator node, set currentData to currentData's childNodes
                if (key == ShortCutsConst.CHILD_NODE && currentData is MiniElement) {
                    currentData = currentData.unsafeCast<MiniElement>().childNodes
                }

                // If current key is not cn string, need to handle different cases
                if (key != ShortCutsConst.CHILD_NODE) {
                    // Ignore content starting with p or st, these are characters for setting
                    // attributes and styles, not related to customWrapper calculation
                    if (
                        key.startsWith(ShortCutsConst.ATTR_START_CHAR).unsafeCast<Boolean>() ||
                        key.startsWith(ShortCutsConst.STYLE).unsafeCast<Boolean>()
                    ) {
                        isIgnoreItem = true
                    } else {
                        // Get numbers like 0, 11 from strings like [0], [11] as child node index,
                        // then set currentData based on indexKey
                        val indexKey = extractIntFromBrackets(key.unsafeCast<String>())
                        if (
                            indexKey != null &&
                            currentData != null &&
                            currentData.unsafeCast<JsArray<*>>().length - 1 >= indexKey
                        ) {
                            currentData = currentData.unsafeCast<JsArray<*>>()[indexKey]
                        }
                    }
                }
                // If isIgnoreItem is true, this is a character that can be ignored
                if (isIgnoreItem) {
                    return@forEach
                }
                // If current index == list.length -1, means we've reached the last character
                // This case usually means single node update and the node needs needCustomWrapper operation
                // If there's no existing customWrapper component in the environment,
                // need to ignore and use parent's customWrapperId
                if (
                    !isLastIndex(index, list.length) &&
                    currentData is MiniElement &&
                    currentData.unsafeCast<MiniElement>().needCustomWrapper == true
                ) {
                    splitedPath = "cn.[0]." + dataPathArr.slice(index + 2).join(".")
                    customWrapperId = "custom-${currentData.unsafeCast<MiniElement>().innerId}"
                }
            }
        }

        return JsArray(splitedPath, customWrapperId)
    }

    /**
     * Execute update tasks
     */
    fun performUpdate() {
        this.pendingUpdate = true
        // Add to task queue, call mini program native setData, update UI
        // Here scheduleTask is not set to 0 because when set to 0, kuikly core's update task
        // will generate two update dependency collections, causing two setData calls
        KuiklyRenderCoreContextScheduler.scheduleTask(1) {
            // Reset path list, used later to determine which update tasks can be discarded
            val resetPathSet: JsSet<String> = JsSet()
            // Ignore attribute Paths, if it's a task updating the node itself, then in this update task
            // those updating this node's attributes, content, styles can be ignored
            val ignoreAttrPathSet: JsSet<String> = JsSet()
            // Map of tasks to update
            val elementsMap: JsMap<String, UpdatePayload> = JsMap()

            updatePayloadList.forEach { item ->
                // Consume this update
                item.onConsume?.let {
                    it()
                }
                val path = item.path
                // For single node content setting, subsequent style settings can be ignored
                // When style changes, can check if parent node has node update, then can ignore style update
                // Here is the case when parent node doesn't have update, if the node itself is updated
                // then this node's style updates can also be ignored
                if (item.updateType == UpdateType.SELF) {
                    ignoreAttrPathSet.add(item.path)
                }

                // If updating child nodes, then child nodes' own updates can be ignored
                if (item.updateType == UpdateType.CHILD) {
                    resetPathSet.add(path)
                }

                elementsMap[path] = item
            }

            updatePayloadList.clear()

            // Loop through elementsMap, remove update tasks that can be ignored
            elementsMap.forEach { currentUpload, key ->
                val currentPath = key.asDynamic()
                var needRemove = false

                // If it's style or text update, and the updated node has had UpdateType.SELF update task, ignore it
                // Note: attribute updates cannot be ignored here, for example operations like setting scroll position
                // are attributes and cannot be ignored
                if (
                    currentUpload.updateType == UpdateType.STYLE ||
                    currentPath.endsWith(ShortCutsConst.TEXT).unsafeCast<Boolean>()
                ) {
                    if (ignoreAttrPathSet.has(currentUpload.updateRawPath)) {
                        needRemove = true
                    }
                }

                // If it's a task updating the node itself, check if parent nodes have updates,
                // then can ignore this update task
                if (currentUpload.updateType == UpdateType.SELF) {
                    resetPathSet.forEach { p ->
                        if (currentPath.includes(p).unsafeCast<Boolean>() && currentPath != p) {
                            needRemove = true
                        }
                    }
                }

                if (needRemove) {
                    elementsMap.delete(key)
                }
            }

            val updateData = json()
            // Whether page level setData is needed
            var needPageSetData = false
            // Custom component update content Map
            val customWrapperMap: JsMap<String, Json> = JsMap()

            elementsMap.forEach { updatePayload, key ->
                val currentPath = key.asDynamic()
                // Get specific content for setData
                val dataDetailValue = updatePayload.value()
                // Check if current update content is wrapped by customWrapper
                val customData = findCustomWrapper(this, currentPath.split("."))
                val splitedPath = customData[0]
                val customWrapperId = customData[1]
                // Prepare different update data based on whether the update task's node is wrapped by customWrapper
                if (splitedPath != "" && customWrapperId != "") {
                    val customUpdatePathJson = customWrapperMap[customWrapperId]
                    if (customUpdatePathJson == null) {
                        customWrapperMap[customWrapperId] =
                            json("i.${splitedPath}" to dataDetailValue)
                    } else {
                        customUpdatePathJson["i.${splitedPath}"] = dataDetailValue
                    }
                } else {
                    updateData[currentPath.unsafeCast<String>()] = dataDetailValue
                    needPageSetData = true
                }
            }

            // Call setData in different contexts based on prepared update data
            if (customWrapperMap.isNotEmpty()) {
                customWrapperMap.forEach { value, key ->
                    val customWrapperDom = MiniGlobal.globalThis.customWrapperCache.get(key)
                    customWrapperDom?.setData(value)
                }
            }
            if (needPageSetData) {
                miniPageInstance?.asDynamic()?.setData(updateData)
            }
            pendingUpdate = false
        }
    }
}
