package com.tencent.kuikly.core.render.web.expand.module

import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONException
import com.tencent.kuikly.core.render.web.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.render.web.utils.Log
import kotlin.js.Date
import kotlin.math.floor

/**
 * Kuikly date module
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


    /**
     * Parse timestamp from formatStr like yyyy-dd-MM and valueStr like 2024-01-01
     */
    private fun parseTimeStringToTimestamp(formatStr: String, valueStr: String): Long {
        val yearStart = formatStr.indexOf("yyyy")
        val mouthStart = formatStr.indexOf("MM")
        val dayStart = formatStr.indexOf("dd")

        if (yearStart == -1 || mouthStart == -1 || dayStart == -1) {
            Log.error(TAG, "parseFormat: error, the params is yyyy MM dd must in formatStr")
            return 0L
        }
        val year = valueStr.substring(yearStart, yearStart + 4).toInt()
        val mouth = valueStr.substring(mouthStart, mouthStart + 2).toInt()
        val day = valueStr.substring(dayStart, dayStart + 2).toInt()

        val hourStart = formatStr.indexOf("HH")
        val minuteStart = formatStr.indexOf("mm")
        val secondStart = formatStr.indexOf("ss")
        val millisecondStart = formatStr.indexOf("SSS")

        val hour = if (hourStart != -1) {
            valueStr.substring(hourStart, hourStart + 2).toInt()
        } else {
            0
        }
        val minute = if (minuteStart != -1) {
            valueStr.substring(minuteStart, minuteStart + 2).toInt()
        } else {
            0
        }
        val second = if (secondStart != -1) {
            valueStr.substring(secondStart, secondStart + 2).toInt()
        } else {
            0
        }
        val millisecond = if (millisecondStart != -1) {
            valueStr.substring(millisecondStart, millisecondStart + 3).toInt()
        } else {
            0
        }

        val date = Date().asDynamic()
        date.setFullYear(year)
        date.setMonth(mouth - 1)
        date.setDate(day)
        date.setHours(hour)
        date.setMinutes(minute)
        date.setSeconds(second)
        date.setMilliseconds(millisecond)
        return js("String")(date.getTime()).unsafeCast<String>().toLongOrNull() ?: 0L
    }

    /**
     * Get current timestamp
     */
    private fun curTimestamp(): String = "${Date.now()}"

    /**
     * Get specified date field
     */
    private fun getField(params: String?): String? {
        val paramsJSObj = params?.toJSObjSafely() ?: run {
            Log.error(TAG, "getField: error, the params is null")
            return null
        }
        // Get original timestamp from params
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
        return calendar.get(filed)
    }

    /**
     * Get processed timestamp
     */
    private fun getTimeInMillis(params: String?): String? {
        val paramsJSObj = params?.toJSObjSafely() ?: run {
            Log.error(TAG, "getTimeInMillis: error, the params is null")
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

    /**
     * Format string
     */
    private fun dateStrFormat(date: Date, formatString: String): String {
        return formatString
            .replace("yyyy", date.getFullYear().toString())
            .replace("MM", (date.getMonth() + 1).toString().padStart(2, '0'))
            .replace("dd", date.getDate().toString().padStart(2, '0'))
            .replace("HH", date.getHours().toString().padStart(2, '0'))
            .replace("mm", date.getMinutes().toString().padStart(2, '0'))
            .replace("ss", date.getSeconds().toString().padStart(2, '0'))
            .replace("SSS", date.getMilliseconds().toString().padStart(3, '0'))
    }

    /**
     * Process single quotes in formatString and return processed string
     */
    private fun getReplaceReadyFormatString(formatString: String): String {
        val result = StringBuilder()
        var inLiteral = false // Mark if in text block wrapped by single quotes
        val literalBuffer = StringBuilder() // Cache text block content

        // Step 1: Parse single quote blocks and handle escaping
        var i = 0
        while (i < formatString.length) {
            val char = formatString[i]
            if (char == '\'') {
                if (inLiteral) {
                    // Check if next character is single quote (escape)
                    if (i + 1 < formatString.length && formatString[i + 1] == '\'') {
                        literalBuffer.append("'") // Escape two single quotes to one
                        i += 2 // Skip next character
                    } else {
                        // End text block, append processed content
                        result.append(literalBuffer)
                        literalBuffer.clear()
                        inLiteral = false
                        i++
                    }
                } else {
                    // Start text block
                    inLiteral = true
                    i++
                }
            } else {
                if (inLiteral) {
                    // In text block, directly cache character
                    literalBuffer.append(char)
                    i++
                } else {
                    // In placeholder block, directly append character
                    result.append(char)
                    i++
                }
            }
        }

        // Handle unclosed single quotes (if any)
        if (inLiteral) {
            result.append("'").append(literalBuffer)
        }

        return result.toString()
    }

    /**
     * Format string with single quotes
     */
    private fun formatDateStrWithSingleQuote(date: Date, formatString: String): String =
        dateStrFormat(date, getReplaceReadyFormatString(formatString))


    /**
     * Return formatted date string
     */
    private fun dateFormat(date: Date, formatString: String): String {
        // Simple handling without single quotes
        if (!formatString.contains("'")) {
            return dateStrFormat(date, formatString)
        }
        return formatDateStrWithSingleQuote(date, formatString)
    }

    /**
     * Format and return time string according to given format and timestamp
     */
    private fun format(params: String?): String? {
        val paramsJSObj = params?.toJSObjSafely() ?: run {
            Log.error(TAG, "format: error, the params is null")
            return null
        }
        return dateFormat(
            Date(paramsJSObj.optLong(PARAM_TIME_MILLIS)),
            paramsJSObj.optString(PARAM_FORMAT)
        )
    }


    private fun parseDateStringToLong(formatString: String, formattedTime: String): Long {
        val replaceReadyFormatString = getReplaceReadyFormatString(formatString)
        return parseTimeStringToTimestamp(replaceReadyFormatString, formattedTime)
    }

    /**
     * Process formatted time string to get timestamp
     */
    private fun parseFormat(params: String?): Long {
        val paramsJSObj = params?.toJSObjSafely() ?: run {
            Log.error(TAG, "parseFormat: error, the params is null")
            return 0L
        }
        val formatStr = paramsJSObj.optString(PARAM_FORMAT)
        val formattedTime = paramsJSObj.optString(PARAM_FORMATTED_TIME)

        return try {
            parseDateStringToLong(formatStr, formattedTime)
        } catch (e: dynamic) {
            Log.error(TAG, "parseFormat: error, e=${e.message}")
            0L
        }
    }

    /**
     * Safe conversion JSON
     */
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

private sealed class Operation(val opt: String, val field: Int, val value: Int) {
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
            Log.error("toOperations", "parse json error")
        }
    }
    return list
}

/**
 * Date processing unified object
 */
internal object Calendar {
    /**
     * Calendar field
     */
    enum class Field(val id: Int) {
        YEAR(1),

        /**
         * From 0, 0 represents January, 11 represents December
         */
        MONTH(2),

        /**
         * From 1, 1 represents the 1st
         */
        DAY_OF_MONTH(5),

        DAY_OF_YEAR(6),

        /**
         * Date corresponding to the day of the week, 1 is Sunday, 2 is Monday, and so on to Saturday
         */
        DAY_OF_WEEK(7),

        /**
         * 24-hour system, 22 represents 22:00
         */
        HOUR_OF_DAY(11),

        MINUTE(12),

        SECOND(13),

        MILLISECOND(14)
    }

    fun getInstance(): CalendarData = CalendarData()
}

/**
 * Date processing object
 */
internal class CalendarData {
    private var _timestamp = 0L

    // Current timestamp
    var timeInMillis: Long
        get() {
            if (_timestamp == 0L) {
                // If not initialized, use the current time
                _timestamp = Date.now().toLong()
            }
            return _timestamp
        }
        set(value) {
            _timestamp = value
        }

    /**
     * According to Calendar rules, add or subtract a specified time amount for a given calendar field
     */
    fun add(field: Int, value: Int) {
        // Use current timestamp to initialize Date instance
        val date = Date(timeInMillis)
        when (field) {
            Calendar.Field.YEAR.id -> {
                // Set year
                date.asDynamic().setFullYear(date.getFullYear() + value)
            }
            /**
             * From 0, 0 represents January, 11 represents December
             */
            Calendar.Field.MONTH.id -> {
                // Set month
                date.asDynamic().setMonth(date.getMonth() + value)
            }
            /**
             * From 1, 1 represents the 1st
             */
            Calendar.Field.DAY_OF_MONTH.id -> {
                // Set the date of the month
                date.asDynamic().setDate(date.getDate() + value)
            }
            /**
             * 24-hour system, 22 represents 22:00
             */
            Calendar.Field.HOUR_OF_DAY.id -> {
                // Set hour
                date.asDynamic().setHours(date.getHours() + value)
            }

            Calendar.Field.MINUTE.id -> {
                // Set minutes
                date.asDynamic().setMinutes(date.getMinutes() + value)
            }

            Calendar.Field.SECOND.id -> {
                // Set seconds
                date.asDynamic().setSeconds(date.getSeconds() + value)
            }

            Calendar.Field.MILLISECOND.id -> {
                // Set milliseconds
                date.asDynamic().setMilliseconds(date.getMilliseconds() + value)
            }

            else -> {}
        }
        // Save updated timestamp
        timeInMillis = date.getTime().toLong()
    }

    /**
     * Return the value of the given calendar field
     */
    fun get(field: Int): String {
        // Use current timestamp to initialize Date instance
        val date = Date(timeInMillis)
        return when (field) {
            Calendar.Field.YEAR.id -> {
                // Return year
                date.getFullYear().toString()
            }
            /**
             * From 0, 0 represents January, 11 represents December
             */
            Calendar.Field.MONTH.id -> {
                // Return month
                date.getMonth().toString()
            }
            /**
             * From 1, 1 represents the 1st
             */
            Calendar.Field.DAY_OF_MONTH.id -> {
                // Return the date of the month
                date.getDate().toString()
            }

            Calendar.Field.DAY_OF_YEAR.id -> {
                // First get the time point at the beginning of the year
                val startOfYear = Date(date.getFullYear(), 0, 0)
                // Calculate how long it has been since the beginning of the year
                val gap = timeInMillis - startOfYear.getTime()
                // Divide the difference in milliseconds by the number of milliseconds in a day to get the current day of the year
                floor(gap / 86400000).toString()
            }
            /**
             * Date corresponding to the day of the week, 1 is Sunday, 2 is Monday, and so on to Saturday
             */
            Calendar.Field.DAY_OF_WEEK.id -> {
                (date.getDay() + 1).toString()
            }
            /**
             * 24-hour system, 22 represents 22:00
             */
            Calendar.Field.HOUR_OF_DAY.id -> {
                // Return the current hour
                date.getHours().toString()
            }

            Calendar.Field.MINUTE.id -> {
                // Return the current minute
                date.getMinutes().toString()
            }

            Calendar.Field.SECOND.id -> {
                // Return the current second
                date.getSeconds().toString()
            }

            Calendar.Field.MILLISECOND.id -> {
                // Return the current millisecond
                date.getMilliseconds().toString()
            }

            else -> {
                ""
            }
        }
    }

    /**
     * Set the given calendar field to the given value, year, month, day, hour, minute, second
     */
    fun set(field: Int, value: Int) {
        // Use current timestamp to initialize Date instance
        val date = Date(timeInMillis)
        when (field) {
            Calendar.Field.YEAR.id -> {
                // Set year
                date.asDynamic().setFullYear(value)
            }
            /**
             * From 0, 0 represents January, 11 represents December
             */
            Calendar.Field.MONTH.id -> {
                // Set month
                date.asDynamic().setMonth(value)
            }
            /**
             * From 1, 1 represents the 1st
             */
            Calendar.Field.DAY_OF_MONTH.id -> {
                // Set the date of the month
                date.asDynamic().setDate(value)
            }
            /**
             * Set the day of the year
             */
            Calendar.Field.DAY_OF_YEAR.id -> {
                date.apply {
                    val year = getFullYear()
                    val hours = getHours()
                    val minutes = getMinutes()
                    val seconds = getSeconds()
                    val milliseconds = getMilliseconds()

                    asDynamic().setFullYear(year, 0, value)
                    asDynamic().setHours(hours, minutes, seconds, milliseconds).unsafeCast<Unit>()
                }
            }

            /**
             * 24-hour system, 22 represents 22:00
             */
            Calendar.Field.HOUR_OF_DAY.id -> {
                // Set hour
                date.asDynamic().setHours(value)
            }

            Calendar.Field.MINUTE.id -> {
                // Set minute
                date.asDynamic().setMinutes(value)
            }

            Calendar.Field.SECOND.id -> {
                // Set second
                date.asDynamic().setSeconds(value)
            }

            Calendar.Field.MILLISECOND.id -> {
                // Set millisecond
                date.asDynamic().setMilliseconds(value)
            }

            else -> {}
        }
        // Save updated timestamp
        timeInMillis = date.getTime().toLong()
    }
}
