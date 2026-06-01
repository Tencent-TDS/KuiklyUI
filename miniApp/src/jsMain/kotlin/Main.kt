import com.tencent.kuikly.core.render.web.KuiklyRenderView
import com.tencent.kuikly.core.render.web.IKuiklyRenderViewLifecycleCallback
import com.tencent.kuikly.core.render.web.exception.ErrorReason
import com.tencent.kuikly.core.render.web.collection.FastMutableMap
import com.tencent.kuikly.core.render.web.ktx.SizeI
import com.tencent.kuikly.core.render.web.processor.TextMeasureCache
import com.tencent.kuikly.core.render.web.runtime.miniapp.MiniDocument
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.App
import com.tencent.kuikly.core.render.web.runtime.miniapp.core.NativeApi
import com.tencent.kuikly.core.render.web.runtime.miniapp.expand.KuiklyRenderViewDelegator
import com.tencent.kuikly.core.render.web.runtime.miniapp.processor.RichTextProcessor
import com.tencent.kuikly.miniapp.KuiklyWebRenderViewDelegator

const val TAG = "Main"

// Global list of active delegators for font loaded notification
private val activeDelegators = mutableListOf<KuiklyRenderViewDelegator>()

fun main() {
    App.onShow {
        console.log(TAG, "app show")
    }

    App.onLaunch {
        console.log(TAG, "app launch")
    }

    App.onHide {
        console.log(TAG, "app hide")
    }
}

/**
 *  Mini program page entry, use renderView delegate method to initialize and create renderView
 */
@JsName(name = "renderView")
@JsExport
@ExperimentalJsExport
fun renderView(json: dynamic) {
    // View size
    var size: SizeI? = null
    if (json.width != null && json.height != null) {
        size = SizeI(json.width.unsafeCast<Int>(), json.height.unsafeCast<Int>())
    }

    // Pass raw json object directly to initPage, which will wrap it in FastMutableMap internally
    MiniDocument.initPage(json) { pageId: Int, pageName: String, paramsMap: FastMutableMap<String, Any> ->
        val systemInfo = NativeApi.plat.getSystemInfoSync()
        val isAndroid = systemInfo.platform == "android"
        val params = paramsMap["param"].unsafeCast<FastMutableMap<String, Any>>()
        params["is_wx_mp"] = "true"

        // paramsMap["platform"] = if (isAndroid) "android" else "iOS"
        paramsMap["platform"] = "miniprogram"
        paramsMap["isIOS"] = !isAndroid
        paramsMap["isIphoneX"] = !isAndroid && systemInfo.safeArea.top > 30

        val delegatorWrapper = KuiklyWebRenderViewDelegator()
        val delegator = delegatorWrapper.delegate
        delegator.onAttach(
            pageId,
            pageName,
            paramsMap,
            size,
        )
        // Save delegator reference for font loaded notification
        activeDelegators.add(delegator)
        // Register lifecycle callback to remove delegator when page is destroyed
        delegator.addKuiklyRenderViewLifeCycleCallback(object : IKuiklyRenderViewLifecycleCallback {
            override fun onInit() {}
            override fun onPreloadDexClassFinish() {}
            override fun onInitCoreStart() {}
            override fun onInitCoreFinish() {}
            override fun onInitContextStart() {}
            override fun onInitContextFinish() {}
            override fun onCreateInstanceStart() {}
            override fun onCreateInstanceFinish() {}
            override fun onFirstFramePaint() {}
            override fun onResume() {}
            override fun onPause() {}
            override fun onDestroy() {
                activeDelegators.remove(delegator)
            }
            override fun onRenderException(throwable: Throwable, errorReason: ErrorReason) {}
        })
    }
}

/**
 * Notify all active Kuikly pages that font has been loaded,
 * triggering re-measurement of text with correct font metrics.
 * This should be called from app.js after wx.loadFontFace succeeds.
 */
@JsName(name = "fontLoaded")
@JsExport
@ExperimentalJsExport
fun fontLoaded() {
    RichTextProcessor.resetMeasureContext()
    TextMeasureCache.clear()
    activeDelegators.forEach { delegator ->
        try {
            delegator.sendEvent(KuiklyRenderView.PAGER_EVENT_ON_FONT_LOADED, mapOf())
        } catch (e: Throwable) {
            console.log(TAG, "fontLoaded: sendEvent failed: ${e.message}")
        }
    }
}

/**
 * Register callback methods on the mini program App object, needs to be called in the app.js of the mini program
 */
@JsName(name = "initApp")
@JsExport
@ExperimentalJsExport
fun initApp(options: dynamic = js("{}")) {
    App.initApp(options)
}
