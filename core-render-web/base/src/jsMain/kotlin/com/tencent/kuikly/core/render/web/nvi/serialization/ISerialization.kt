package com.tencent.kuikly.core.render.web.nvi.serialization

import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject

interface ISerialization {
    fun serialization(): JSONObject
}

interface IDeserialization {
    fun deserialization(json: JSONObject)
}
