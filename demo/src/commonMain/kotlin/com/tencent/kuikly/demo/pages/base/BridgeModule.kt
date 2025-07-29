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

package com.tencent.kuikly.demo.pages.base

import com.tencent.kuikly.core.base.toInt
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.module.CallbackFn
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

internal class BridgeModule : Module() {

    override fun moduleName(): String {
        return MODULE_NAME
    }

    fun toast(content: String) {
        val methodArgs = JSONObject()
        methodArgs.put("content", content)
        callNativeMethod("toast", methodArgs, null)
    }

    fun testArray() {
        //call
        val array = arrayOf<Any>("222", createByteArray())
        val res = syncToNativeMethod("testArray", array) {
            if (it is Array<*>) {
                KLog.i(
                    "testArray",
                    "callback res:${it[1] is ByteArray} ${(it[1] as ByteArray)[1].toString()}" + it.toString()
                )

            }
        }

        var i = 0

        if (res is Array<*>) {
            i = res.size

            KLog.i(
                "testArray res:",
                "${res[1] is ByteArray} | ${(res[1] as ByteArray).size}" + res[0].toString()
            )

        }
    }

    fun createByteArray(): ByteArray {
        val size = 10 // 指定 ByteArray 的大小
        val byteArray = ByteArray(size)

        for (i in 0 until size) {
            byteArray[i] = (i * 2).toByte() // 每个元素的值为其索引的两倍
        }

        return byteArray
    }

    fun openPage(
        url: String,
        closeCurPage: Boolean = false,
        closeSamePage: Boolean = false,
        userData: JSONObject? = null,
        callbackFn: CallbackFn? = null
    ) {
        val methodArgs = JSONObject()
        methodArgs.put("url", url)
        methodArgs.put("closeCurPage", closeCurPage.toInt())
        methodArgs.put("closeSamePage", closeSamePage.toInt())
        userData?.also {
            methodArgs.put("userData", it)
        }
        callNativeMethod(OPEN_PAGE, methodArgs, callbackFn)
    }

    // 同步获取时间戳（毫秒）
    // 注：一般不用于业务，仅为本地性能耗时测试
    fun currentTimeStamp(): Long {
        val timestamp = syncCallNativeMethod(CURRENT_TIMESTAMP, null, null)
        if (timestamp.isNotEmpty()) {
            return timestamp.toLong()
        } else {
            return 0
        }
    }

    // 同步获取日期格式化
    fun dateFormatter(timeStamp: Long, format: String): String {
        val params = JSONObject()
        params.put("timeStamp", timeStamp)
        params.put("format", format)
        return syncCallNativeMethod(DATE_FORMATTER, params, null)
    }

    /**
     * 获取指定 url 的本地缓存地址
     */
    fun getLocalImagePath(url: String, callback: CallbackFn?) {
        val params = JSONObject()
        params.put("imageUrl", url)
        callNativeMethod(GET_LOCAL_IMAGE_PATH, params, callback)
    }

    /**
     * 读取文件内容
     */
    fun readAssetFile(assetPath: String, callback: CallbackFn?) {
        val params = JSONObject()
        params.put("assetPath", assetPath)
        syncCallNativeMethod(READ_ASSET_FILE, params, callback)
    }

    private fun callNativeMethod(methodName: String, data: JSONObject?, callbackFn: CallbackFn?) {
        toNative(
            false,
            methodName,
            data?.toString(),
            callbackFn,
            false
        )
    }

    // --------- 同步调用Native方法 -------
    private fun syncCallNativeMethod(
        methodName: String,
        data: JSONObject?,
        callbackFn: CallbackFn?
    ): String {
        return toNative(
            false,
            methodName,
            data?.toString(),
            callbackFn,
            true
        ).toString()
    }

    companion object {
        const val MODULE_NAME = "HRBridgeModule"
        const val OPEN_PAGE = "openPage"
        const val CLOSE_PAGE = "closePage"
        const val LOG = "log"
        const val LOG_AND_TELEMETRY = "logAndTelemetry"
        const val REPORT_DT = "reportDT"
        const val LOCAL_SERVE_TIME = "localServeTime"
        const val SERVER_TIME_MILLIS = "serverTimeMillis"
        const val CURRENT_TIMESTAMP = "currentTimestamp"
        const val DATE_FORMATTER = "dateFormatter"
        const val REPORT_REALTIME = "reportRealTime"
        const val REPORT_PAGE_COST_TIME_FOR_CACHE = "reportPageCostTimeForCache"
        const val REPORT_PAGE_COST_TIME_FOR_SUCCESS = "reportPageCostTimeForSuccess"
        const val REPORT_PAGE_COST_TIME_FOR_ERROR = "reportPageCostTimeForError"
        const val REMOTE_CONFIG = "loadRemoteConfig"
        const val SIGN_ALERT = "signAlert"
        const val CLOSE_KEYBOARD = "closeKeyboard"
        const val URL_ENCODE = "urlEncode"
        const val URL_DECODE = "urlDecode"
        const val HUMAN_VERIFICATION = "humanVerification"
        const val PRELOAD_PB = "preloadPB"
        const val CLEAN_PB = "cleanPB"
        const val KEY_FEED_PB_TOKEN = "feedPbToken"
        const val SET_STATUS_BAR_WHITE = "setWhiteStatusBarStyle"
        const val SET_STATUS_BAR_BLACK = "setBlackStatusBarStyle"
        const val GET_CURRENT_ACCOUNT = "getAccount"
        const val DOWNLOAD_PAG_SO = "downloadPagSo"
        const val GET_LOCAL_IMAGE_PATH = "getLocalImagePath"
        const val READ_ASSET_FILE = "readAssetFile"
    }

}
