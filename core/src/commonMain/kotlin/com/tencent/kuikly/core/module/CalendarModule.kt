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

package com.tencent.kuikly.core.module

import com.tencent.kuikly.core.collection.fastArrayListOf
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

/**
 *  日期计算，通过 [newCalendarInstance] 拿到 [ICalendar] 实例，然后可访问 [ICalendar] 提供读写的接口
 */
class CalendarModule : Module() {
    override fun moduleName(): String = MODULE_NAME
    companion object {
        const val MODULE_NAME = ModuleConst.CALENDAR
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

    /**
     * [timeMillis] 为 0 则返回当前时间，否则返回这个 UTC 对应的日期
     */
    fun newCalendarInstance(timeMillis: Long = 0L): ICalendar {
        val curTime =
            if (timeMillis == 0L) {
                toNative(
                    keepCallbackAlive = false,
                    methodName = METHOD_CURRENT_TIMESTAMP,
                    syncCall = true,
                    param = null
                ).toString().toLongOrNull() ?: 0L
            } else {
                timeMillis
            }
        return Calendar(curTime)
    }

    /**
     * 将时间戳(单位毫秒) [timeMillis] 按照 [format] 格式化
     */
    fun formatTime(timeMillis: Long, format: String): String {
        val param = JSONObject().apply {
            put(PARAM_TIME_MILLIS, timeMillis)
            put(PARAM_FORMAT, format)
        }
        return toNative(
            keepCallbackAlive = false,
            methodName = METHOD_FORMAT,
            syncCall = true,
            param = param.toString()
        ).toString()
    }

    /**
     * 将一个被 [format] 格式化的事件 [formattedTime] 解析成时间戳, 单位毫秒
     */
    fun parseFormattedTime(formattedTime: String, format: String): Long {
        val param = JSONObject().apply {
            put(PARAM_FORMATTED_TIME, formattedTime)
            put(PARAM_FORMAT, format)
        }
        return toNative(
            keepCallbackAlive = false,
            methodName = METHOD_PARSE_FORMAT,
            syncCall = true,
            param = param.toString()
        ).toString().toLongOrNull() ?: 0L
    }

    private inner class Calendar(val originTimestamp: Long) : ICalendar {

        private val operationRecords = fastArrayListOf<Operation>()

        override fun set(field: ICalendar.Field, value: Int): ICalendar {
            operationRecords.add(Operation.Set(field, value))
            return this
        }

        override fun add(field: ICalendar.Field, value: Int): ICalendar {
            operationRecords.add(Operation.Add(field, value))
            return this
        }

        override fun get(field: ICalendar.Field): Int {
            val param = JSONObject().apply {
                put(PARAM_TIME_MILLIS, originTimestamp)
                put(PARAM_OPERATIONS, operationRecords.toJSONArray().toString())
                put(PARAM_FIELD, field.id)
            }

            return toNative(
                keepCallbackAlive = false,
                methodName = METHOD_GET_FIELD,
                syncCall = true,
                param = param.toString()
            ).toString().toIntOrNull() ?: 0
        }

        override fun timeInMillis(): Long {
            val param = JSONObject().apply {
                put(PARAM_TIME_MILLIS, originTimestamp)
                put(PARAM_OPERATIONS, operationRecords.toJSONArray().toString())
            }

            return toNative(
                keepCallbackAlive = false,
                methodName = METHOD_GET_TIME_IN_MILLIS,
                syncCall = true,
                param = param.toString()
            ).toString().toLongOrNull() ?: 0L
        }

    }
}

interface ICalendar {

    enum class Field(val id: Int) {
        YEAR(1),

        /**
         * 从 0 开始, 0 表示一月, 11 表示十二月
         */
        MONTH(2),

        /**
         * 从 1 开始，1 表示一号
         */
        DAY_OF_MONTH(5),

        /**
         * 一年中第一天的值为 1
         */
        DAY_OF_YEAR(6),
        /*
         * 日期对应礼拜几，1为周日，2为周一,以此类推7为周六
         */
        DAY_OF_WEEK(7),

        /**
         * 24 小时制，22 表示 22:00
         */
        HOUR_OF_DAY(11),

        MINUS(12),

        SECOND(13),

        MILLISECOND(14)
    }

    /**
     * 指定年月日时分秒并返回日期
     * @param field 参考 [Field] 注意: [Field.MONTH] 从 0 开始，0 表示一月, 11 表示十二月
     * @return 返回修改后的日期
     */
    fun set(field: Field, value: Int): ICalendar

    /**
     * 给当前时间添加或者减去指定时间字段，比如如果要减去 5 天
     *
     * ```kotlin
     * add(Filed.DAY, -5)
     * ```
     * @param field 参考 [Field] 注意: [Field.MONTH] 从 0 开始，0 表示一月, 11 表示十二月
     * @return 返回修改后的日期
     */
    fun add(field: Field, value: Int): ICalendar

    /**
     * 获取年月日时分秒, [field] 指定具体要获取的字段
     */
    fun get(field: Field): Int

    /**
     * 获取当前日历的时间戳，单位毫秒
     */
    fun timeInMillis(): Long
}

private sealed class Operation(
    val opt: String,
    val field: ICalendar.Field,
    val value: Int
) {
    class Set(field: ICalendar.Field, value: Int) : Operation("set", field, value)
    class Add(field: ICalendar.Field, value: Int) : Operation("add", field, value)
}

private fun Operation.toJSON(): JSONObject {
    return JSONObject()
        .put("opt", this.opt)
        .put("field", this.field.id)
        .put("value", this.value)
}

private fun List<Operation>.toJSONArray(): JSONArray {
    val jsonArray = JSONArray()
    forEach {
        jsonArray.put(it.toJSON().toString())
    }
    return jsonArray
}
