/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.core.render.android.expand.module

import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Created by zhiyueli on 2023/2/9 18:57.
 */
class KRCalendarModule : KuiklyRenderBaseModule() {

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_CURRENT_TIMESTAMP -> curTimestamp()
            METHOD_GET_FIELD -> getField(params)
            METHOD_GET_TIME_IN_MILLIS -> getTimeInMillis(params)
            METHOD_FORMAT -> format(params)
            METHOD_PARSE_FORMAT -> parseFormat(params).toString()
            else -> super.call(method, params, callback)
        }
    }

    private fun curTimestamp(): String = "${System.currentTimeMillis()}"

    private fun getField(params: String?): String? {
        val paramsJSObj = params?.toJSObjSafely() ?: run {
            KuiklyRenderLog.e(TAG, "getField: error, the params is null")
            return null
        }

        val originTimestamp = paramsJSObj.optString(PARAM_TIME_MILLIS).toLongOrNull() ?: 0L
        val operations = paramsJSObj.optString(PARAM_OPERATIONS).toOperations()
        val filed = paramsJSObj.optInt(PARAM_FIELD)
        val calendar = Calendar.getInstance().apply {
            timeInMillis = originTimestamp
        }

        operations.forEach {
            when (it) {
                is Operation.Add -> calendar.add(it.field, it.value)
                is Operation.Set -> calendar.set(it.field, it.value)
            }
        }
        return "${calendar.get(filed)}"
    }

    private fun getTimeInMillis(params: String?): String? {
        val paramsJSObj = params?.toJSObjSafely() ?: run {
            KuiklyRenderLog.e(TAG, "getTimeInMillis: error, the params is null")
            return null
        }
        val originTimestamp = paramsJSObj.optString(PARAM_TIME_MILLIS).toLongOrNull() ?: 0L
        val operations = paramsJSObj.optString(PARAM_OPERATIONS).toOperations()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = originTimestamp
        }
        operations.forEach {
            when (it) {
                is Operation.Add -> calendar.add(it.field, it.value)
                is Operation.Set -> calendar.set(it.field, it.value)
            }
        }
        return "${calendar.timeInMillis}"
    }

    private fun format(params: String?): String? {
        val paramsJSObj = params?.toJSObjSafely() ?: run {
            KuiklyRenderLog.e(TAG, "format: error, the params is null")
            return null
        }
        val time = paramsJSObj.optLong(PARAM_TIME_MILLIS)
        val format = paramsJSObj.optString(PARAM_FORMAT)
        return SimpleDateFormat(format, Locale.CHINA).format(Date(time))
    }

    private fun parseFormat(params: String?): Long {
        val paramsJSObj = params?.toJSObjSafely() ?: run {
            KuiklyRenderLog.e(TAG, "parseFormat: error, the params is null")
            return 0L
        }
        val formattedTime = paramsJSObj.optString(PARAM_FORMATTED_TIME)
        val format = paramsJSObj.optString(PARAM_FORMAT)
        return try {
            SimpleDateFormat(format, Locale.CHINA).parse(formattedTime)?.time ?: 0L
        } catch (e: ParseException) {
            KuiklyRenderLog.e(TAG, "parseFormat: error, e=${e.message}")
            0L
        }
    }

    private fun String.toJSObjSafely(): JSONObject? {
        return try {
            JSONObject(this)
        } catch (e: JSONException) {
            null
        }
    }

    companion object {
        const val MODULE_NAME = "KRCalendarModule"
        private const val TAG = MODULE_NAME
        private const val METHOD_CURRENT_TIMESTAMP = "method_cur_timestamp"
        private const val METHOD_GET_FIELD = "method_get_field"
        private const val METHOD_GET_TIME_IN_MILLIS = "method_get_time_in_millis"
        private const val METHOD_FORMAT = "method_format"
        private const val METHOD_PARSE_FORMAT = "method_parse_format"

        private const val PARAM_OPERATIONS = "operations"
        private const val PARAM_FIELD = "field"
        private const val PARAM_TIME_MILLIS = "timeMillis"
        private const val PARAM_FORMAT = "format"
        private const val PARAM_FORMATTED_TIME = "formattedTime"
    }
}

private sealed class Operation(@JvmField val opt: String, @JvmField val field: Int, @JvmField val value: Int) {
    class Set(field: Int, value: Int) : Operation("set", field, value)
    class Add(field: Int, value: Int) : Operation("add", field, value)
}

private fun JSONObject.toOperation(): Operation? {
    val field = this.optInt("field")
    val value = this.optInt("value")
    return when (this.opt("opt")) {
        "set" -> Operation.Set(field, value)
        "add" -> Operation.Add(field, value)
        else -> null
    }
}

private fun String.toOperations(): List<Operation> {
    val jsArray = try {
        JSONArray(this)
    } catch (e: JSONException) {
        JSONArray()
    }
    val list = mutableListOf<Operation>()
    for (i in 0 until jsArray.length()) {
        try {
            val jsonObj = JSONObject(jsArray.optString(i) ?: "{}")
            jsonObj.toOperation()?.let {
                list.add(it)
            }
        } catch (e: JSONException) {
            KuiklyRenderLog.e("toOperations", "parse json error")
        }
    }
    return list
}
