package com.tencent.kuikly.core.nvi.serialization

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

interface ISerialization {

    fun serialization(): JSONObject

}