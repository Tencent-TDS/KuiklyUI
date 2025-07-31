package com.tencent.kuikly.demo.pages.compose.chatDemo

import com.tencent.kuikly.core.module.CallbackFn
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

class OhosStreamRequestModule : Module() {

    companion object {
        const val MODULE_NAME = "KROhosStreamRequestModule"
    }
    override fun moduleName(): String = MODULE_NAME

    fun request(url: String,
                model: String,
                apiKey: String,
                prompt: String,
                callbackFn: CallbackFn) {
        val params = JSONObject().apply {
            put("url", url)
            put("model", model)
            put("apiKey", apiKey)
            put("prompt", prompt)
        }.toString()

        toNative(true,
                 "request",
                 params,
                { retEvent ->
                    println(retEvent)
                    val jsonObj = JSONObject(retEvent.toString())
                    val dataStr = jsonObj.optString("data", "")

                    val sseLines = dataStr.split("\n\n")
                    for (line in sseLines) {
                        if (line.startsWith("data: ")) {
                            val jsonPart = line.removePrefix("data: ").trim()
                            if (jsonPart == "[DONE]") continue
                            try {
                                val eventObj = JSONObject()
                                eventObj.put("event", "data")
                                eventObj.put("data", jsonPart)
                                callbackFn.invoke(eventObj)
                            } catch (e: Exception) {
                                // 解析失败，忽略
                            }
                        }
                    }
                },
                false)
    }
}
