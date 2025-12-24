package com.example.kuiklyworkwithminiapp.base

import com.tencent.kuikly.core.base.toInt
import com.tencent.kuikly.core.module.CallbackFn
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class BridgeModule : Module() {

    override fun moduleName(): String {
        return MODULE_NAME
    }

    fun closePage() {
        callNativeMethod(CLOSE_PAGE, null, null)
    }

    fun log(content: String) {
        val methodArgs = JSONObject()
        methodArgs.put("content", content)
        callNativeMethod(LOG, methodArgs, null)
    }

    fun copyToPasteboard(content: String) {
        val methodArgs = JSONObject()
        methodArgs.put("content", content)
        callNativeMethod("copyToPasteboard", methodArgs, null)
    }

    fun showAlert(
        title: String?,
        message: String?,
        leftBtnTitle: String?,
        rightBtnTitle: String?,
        responseCallbackFn: CallbackFn
    ) {
        val methodArgs = JSONObject()
        val buttonArray = JSONArray()
        leftBtnTitle?.also {
            buttonArray.put(it)
        }
        rightBtnTitle?.also {
            buttonArray.put(it)
        }

        methodArgs.put("buttons", buttonArray)
        title?.also {
            methodArgs.put("title", it)
        }
        message?.also {
            methodArgs.put("message", it)
        }
        callNativeMethod("showAlert", methodArgs) {
            responseCallbackFn(it)
        }
    }

    // 拨打电话
    fun callPhone(phoneNumber: String) {
        val methodArgs = JSONObject()
        methodArgs.put("phoneNumber", phoneNumber)
        callNativeMethod("callPhone", methodArgs, null)
    }

    fun toast(content: String) {
        val methodArgs = JSONObject()
        methodArgs.put("content", content)
        callNativeMethod("toast", methodArgs, null)
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

    suspend fun ssoRequest(cmd: String, reqParams: JSONObject): JSONObject? {
        return suspendCoroutine<JSONObject?> { continuation ->
            ssoRequest(cmd, reqParams) {
                continuation.resume(it)
            }
        }
    }

    fun ssoRequest(cmd: String, reqParams: JSONObject, responseCallbackFn: CallbackFn) {
        val methodArgs = JSONObject()
        methodArgs.put("cmd", cmd)
        methodArgs.put("reqParam", reqParams)
        callNativeMethod(SSO_REQUEST, methodArgs, responseCallbackFn)
    }

    fun qqLiveSSORequest(
        service: String,
        method: String,
        reqParams: JSONObject,
        responseCallbackFn: CallbackFn
    ) {
        val methodArgs = JSONObject()
        methodArgs.put("service", service)
        methodArgs.put("method", method)
        methodArgs.put("reqParams", reqParams)
        callNativeMethod(QQ_LIVE_SSO_REQUEST, methodArgs, responseCallbackFn)
    }

    // 灯塔上报
    fun reportDT(eventCode: String, data: JSONObject) {
        val methodArgs = JSONObject()
        methodArgs.put("eventCode", eventCode)
        methodArgs.put("data", data)
        // methodArgs.put("realtime", 1)
        callNativeMethod(REPORT_DT, methodArgs, null)
    }

    // 实时上报
    fun reportRealTime(eventCode: String, data: JSONObject) {
        val methodArgs = JSONObject()
        methodArgs.put("eventCode", eventCode)
        methodArgs.put("data", data)
        callNativeMethod(REPORT_REALTIME, methodArgs, null)
    }

    // 页面首屏（有内容，来自缓存）耗时上报
    fun reportPageCostTimeForCache() {
        callNativeMethod(REPORT_PAGE_COST_TIME_FOR_CACHE, null, null)
    }

    // 页面首屏（有内容，来自后台）耗时上报
    fun reportPageCostTimeForSuccess() {
        callNativeMethod(REPORT_PAGE_COST_TIME_FOR_SUCCESS, null, null)
    }

    // 页面首屏耗时上报 - 加载失败
    fun reportPageCostTimeForError() {
        callNativeMethod(REPORT_PAGE_COST_TIME_FOR_ERROR, null, null)
    }

    fun openSelectAddressView(addressData: JSONObject?, callbackFn: CallbackFn) {
        val methodArgs = JSONObject()
        addressData?.apply {
            methodArgs.put("addressData", addressData)
            methodArgs.put("from", 5)
        }

        callNativeMethod("openSelectAddressView", methodArgs) {
            callbackFn(it)
        }
    }

    fun openApplySampleSuccessPage(
        orderId: String,
        shopId: String,
        spuId: String,
        skuId: String,
        priSortId: String
    ) {
        val methodArgs = JSONObject()
        methodArgs.put("orderId", orderId)
        methodArgs.put("shopId", shopId)
        methodArgs.put("spuId", spuId)
        methodArgs.put("skuId", skuId)
        methodArgs.put("priSortId", priSortId)

        callNativeMethod("openApplySampleSuccessPage", methodArgs, null)
    }

    // 异步获取本地服务器时间戳
    fun localServeTime(cb: CallbackFn) {
        callNativeMethod(LOCAL_SERVE_TIME, null, cb)
    }

    //同步获取本地服务器时间戳
    suspend fun localServeTime(): JSONObject? {
        return suspendCoroutine<JSONObject?> { continuation ->
            localServeTime() {
                continuation.resume(it)
            }
        }
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
     * 根据 [key] 获取本地缓存的数据, 异步返回
     */
    fun fetchCachedFromNative(key: String, callbackFn: CallbackFn) {
        val param = JSONObject().apply {
            put("key", key)
        }
        callNativeMethod("fetchCachedFromNative", param) {
            callbackFn(it)
        }
    }

    /**
     * 根据 [key] 获取本地缓存的数据, 同步返回
     */
    fun getCachedFromNative(key: String): String {
        val param = JSONObject().apply {
            put("key", key)
        }
        return syncCallNativeMethod("getCachedFromNative", param, null)
    }

    /**
     * 向 native 写入 [key] 对应的缓存
     */
    fun setCachedToNative(key: String, value: String, callbackFn: CallbackFn? = null) {
        val param = JSONObject().apply {
            put("key", key)
            put("value", value)
        }
        callNativeMethod("setCachedToNative", param) {
            callbackFn?.invoke(it)
        }
    }

    /**
     * 预下载图片、PAG、APNG资源
     * */
    fun preDownloadImage(url: String, callbackFn: CallbackFn? = null) {
        val params = JSONObject().apply {
            put("url", url)
        }
        callNativeMethod("preDownloadImage", params) {
            if (callbackFn != null) {
                callbackFn(it)
            }
        }
    }

    fun preDownloadPAGResource(url: String) {
        val params = JSONObject().apply {
            put("url", url)
        }
        callNativeMethod("preDownloadPAGResource", params, null)
    }

    fun preDownloadAPNGResource(url: String) {
        val params = JSONObject().apply {
            put("url", url)
        }
        callNativeMethod("preDownloadAPNGResource", params, null)
    }

    // 更新离线包
    fun updateOfflineIfNeed(bid: String) {
        val params = JSONObject().apply {
            put("bid", bid)
        }
        callNativeMethod("updateOfflineIfNeed", params, null)
    }

    fun showSignJumpAlert(params: JSONObject): String {
        return syncCallNativeMethod(SIGN_ALERT, params, null)
    }

    fun closeKeyboard(data: JSONObject? = null, callbackFn: CallbackFn? = null): String {
        return syncCallNativeMethod(CLOSE_KEYBOARD, data, callbackFn)
    }

    fun humanVerification(params: JSONObject, callbackFn: CallbackFn? = null): String {
        return syncCallNativeMethod(HUMAN_VERIFICATION, params, callbackFn)
    }

    fun urlEncode(string: String): String {
        val params = JSONObject()
        params.put("string", string)
        return syncCallNativeMethod(URL_ENCODE, params, null)
    }

    fun urlDecode(string: String): String {
        val params = JSONObject()
        params.put("string", string)
        return syncCallNativeMethod(URL_DECODE, params, null)
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
        const val SSO_REQUEST = "ssoRequest"
        const val QQ_LIVE_SSO_REQUEST = "qqLiveSSORequest"
        const val REPORT_DT = "reportDT"
        const val LOCAL_SERVE_TIME = "localServeTime"
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
        const val SHOW_PHOTO_BROWSER = "showPhotoBrowser"
        const val HUMAN_VERIFICATION = "humanVerification"
    }

}