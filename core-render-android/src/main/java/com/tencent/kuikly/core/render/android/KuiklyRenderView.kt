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

package com.tencent.kuikly.core.render.android

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.util.ArrayMap
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.FrameLayout
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.const.KRViewConst
import com.tencent.kuikly.core.render.android.context.IKotlinBridgeStatusListener
import com.tencent.kuikly.core.render.android.context.KuiklyRenderCoreExecuteModeBase
import com.tencent.kuikly.core.render.android.core.IKuiklyRenderContextInitCallback
import com.tencent.kuikly.core.render.android.core.IKuiklyRenderCore
import com.tencent.kuikly.core.render.android.core.KuiklyRenderCore
import com.tencent.kuikly.core.render.android.css.ktx.activity
import com.tencent.kuikly.core.render.android.css.ktx.getDisplaySize
import com.tencent.kuikly.core.render.android.css.ktx.isMainThread
import com.tencent.kuikly.core.render.android.css.ktx.screenHeight
import com.tencent.kuikly.core.render.android.css.ktx.screenWidth
import com.tencent.kuikly.core.render.android.css.ktx.statusBarHeight
import com.tencent.kuikly.core.render.android.css.ktx.toDpF
import com.tencent.kuikly.core.render.android.css.ktx.versionName
import com.tencent.kuikly.core.render.android.exception.ErrorReason
import com.tencent.kuikly.core.render.android.exception.IKuiklyRenderExceptionListener
import com.tencent.kuikly.core.render.android.exception.KuiklyRenderModuleExportException
import com.tencent.kuikly.core.render.android.exception.KuiklyRenderShadowExportException
import com.tencent.kuikly.core.render.android.exception.KuiklyRenderViewExportException
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegatorDelegate
import com.tencent.kuikly.core.render.android.expand.component.image.KRImageLoader
import com.tencent.kuikly.core.render.android.expand.component.text.TypeFaceLoader
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderModuleExport
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderShadowExport
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderViewPropExternalHandler
import com.tencent.kuikly.core.render.android.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.android.scheduler.KuiklyRenderCoreTask
import com.tencent.tdf.module.TDFBaseModule
import com.tencent.tdf.module.TDFModuleContext
import com.tencent.tdf.module.TDFModuleManager
import com.tencent.tdf.module.TDFModuleProvider
import java.lang.ref.WeakReference

@SuppressLint("ViewConstructor")
class KuiklyRenderView(
    context: Context,
    private var executeMode: KuiklyRenderCoreExecuteModeBase = KuiklyRenderCoreExecuteModeBase.JVM,
    private val enablePreloadClass: Boolean = true,
    private val delegate: KuiklyRenderViewBaseDelegatorDelegate? = null
) : FrameLayout(context), IKuiklyRenderView {

    private var renderCore: IKuiklyRenderCore? = null

    /**
     * [KuiklyRenderView]的大小
     */
    private var lastSize: SizeF? = null

    /**
     * 初始化[renderCore]的闭包
     * 如果外部调用[init]方法没传size时，会先缓存住初始化[renderCore]的请求,
     * 等系统回调[onSizeChanged]后才运行这个lazyTask
     */
    private var initRenderCoreLazyTask: InitRenderCoreLazyTask? = null

    /**
     * [KuiklyRenderView]上下文对象
     */
    private val kuiklyRenderViewContext = KuiklyRenderViewContext(
        if ((delegate?.enableContextReplace() == true) && context is KuiklyRenderViewContext) context.baseContext!! else context,
        this,
        delegate?.enableContextReplace() ?: false,
        delegate?.useHostDisplayMetrics() ?: false
    )

    /**
     * 暴露给Kuikly页面的管理者
     */
    private val renderExport = KuiklyRenderExport(kuiklyRenderContext)

    /**
     * 如果外部还没调用[init]前就调用[sendEvent]发送事件的话，事件会被缓存住
     * 等外部调用[init]时，再全部发送
     */
    private var coreEventLazyEventList: MutableList<RenderCoreLazyEvent>? = null
    /*
     * Core初始化之后的待执行任务队列
     */
    private var coreLazyTaskList = arrayListOf<RenderCoreLazyTask>()

    /**
     * 生命周期监听
     */
    private var lifecycleCallbacks = ArrayList<IKuiklyRenderViewLifecycleCallback>()

    /**
     * KuiklyRender对外生命周期回调
     */
    private val kuiklyRenderLifecycleCallbacks by lazy(LazyThreadSafetyMode.NONE) { mutableListOf<IKuiklyRenderLifecycleCallback>() }

    /**
     * 异常监听
     */
    private var exceptionListener = object : IKuiklyRenderExceptionListener {

        override fun onRenderException(throwable: Throwable, errorReason: ErrorReason) {
            lifecycleCallbacks.forEach {
                it.onRenderException(throwable, errorReason)
            }
        }

    }

    private var contextInitCallback: IKuiklyRenderContextInitCallback? = null

    init {
        clipChildren = false // 不裁剪, 防止孩子做scale或者translation动画时, 显示不全
    }

    /**
     * 替换 Context
     * @param newContext 新的 Context
     */
    fun replaceContext(newContext: Context) {
        kuiklyRenderViewContext.replaceContext(newContext)
    }

    override fun init(
        contextCode: String,
        pageName: String,
        params: Map<String, Any>,
        size: Size?,
        assetsPath: String?
    ) {
        dispatchLifecycleStateChanged(STATE_INIT)
        assert(isMainThread()) // init方法必须在主线程调用
        initKuiklyClassLoaderIfNeed(contextCode)
        val initRenderCoreTask = { sz: SizeF ->
            initRenderCore(
                contextCode,
                pageName,
                params,
                sz,
                assetsPath
            )
        }
        if (size != null) { // 外部有指定size，直接运行initRenderCoreTask
            val sizeF = SizeF(size.width.toFloat(), size.height.toFloat())
            initRenderCoreTask.invoke(sizeF)
            lastSize = sizeF
        } else {
            val sizeF = lastSize
            if (sizeF == null) {
                // 缓存住初始化任务, 等系统回调onSizeChange后，在运行初始化任务
                initRenderCoreLazyTask = initRenderCoreTask
            } else {
                initRenderCoreTask.invoke(sizeF) // 已经系统回调了onSizeChange, 直接运行初始化任务
            }
        }
    }

    override fun sendEvent(event: String, data: Map<String, Any>) {
        val shouldSync = delegate?.syncSendEvent(event) == true
        renderCore?.sendEvent(event, data, shouldSync) ?: also { // core没初始化时, lazy住事件, 等core初始化后统一发送
            val lazyEventList =
                coreEventLazyEventList ?: mutableListOf<RenderCoreLazyEvent>().apply {
                    coreEventLazyEventList = this
                }
            lazyEventList.add(event to data)
        }
    }

    override fun addView(child: View?, index: Int) {
        super.addView(child, index)
        dispatchLifecycleStateChanged(STATE_FIRST_FRAME_PAINT)
    }

    override fun resume() {
        sendEvent(VIEW_DID_APPEAR,
            mapOf(VIEW_DID_APPEAR to VIEW_DID_APPEAR_VALUE))
        dispatchLifecycleStateChanged(STATE_RESUME)
    }

    override fun pause() {
        sendEvent(VIEW_DID_DISAPPEAR,
            mapOf(VIEW_DID_DISAPPEAR to VIEW_DID_DISAPPEAR_VALUE)
        )
        dispatchLifecycleStateChanged(STATE_PAUSE)
    }

    override fun destroy() {
        dispatchLifecycleStateChanged(STATE_DESTROY)
        renderCore?.destroy()
    }

    override fun syncFlushAllRenderTasks() {
        addTaskWhenCoreDidInit {
            it.syncFlushAllRenderTasks()
        }
    }

    override fun performWhenViewDidLoad(task: KuiklyRenderCoreTask) {
        addTaskWhenCoreDidInit {
            it.performWhenViewDidLoad(task)
        }
    }

    override fun addKuiklyRenderLifecycleCallback(callback: IKuiklyRenderLifecycleCallback) {
        kuiklyRenderLifecycleCallbacks.add(callback)
    }

    override fun removeKuiklyRenderLifeCycleCallback(callback: IKuiklyRenderLifecycleCallback) {
        kuiklyRenderLifecycleCallbacks.remove(callback)
    }

    override fun dispatchOnActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        kuiklyRenderLifecycleCallbacks.toList().forEach {
            it.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun <T : KuiklyRenderBaseModule> module(name: String): T? = renderCore?.module(name)

    override fun <T : TDFBaseModule> getTDFModule(name: String): T? = renderCore?.getTDFModule(name)

    override fun getView(tag: Int): View? = renderCore?.getView(tag)

    override val view: ViewGroup
        get() = this

    override val kuiklyRenderContext: IKuiklyRenderContext
        get() = kuiklyRenderViewContext

    override val kuiklyRenderExport: IKuiklyRenderExport
        get() = renderExport

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        performInitRenderCoreLazyTaskOnce(w, h)
        sendSizeChangeIfNeed(w, h)
    }

    private fun performInitRenderCoreLazyTaskOnce(w: Int, h: Int
    ) {
        if (initRenderCoreLazyTask == null) {
            return
        }
        initRenderCoreLazyTask?.invoke(SizeF(w.toFloat(), h.toFloat()))
        initRenderCoreLazyTask = null
    }

    private fun sendSizeChangeIfNeed(w: Int, h: Int) {
        val sizeF = SizeF(w.toFloat(), h.toFloat())
        if (lastSize == null) {
            lastSize = sizeF
        } else if (lastSize != sizeF) {
            val activitySize = getActivitySize()
            val deviceSize = getDeviceSize()
            sendEvent(
                EVENT_ROOT_VIEW_SIZE_CHANGED, mapOf(
                    KRViewConst.WIDTH to kuiklyRenderContext.toDpF(sizeF.width),
                    KRViewConst.HEIGHT to kuiklyRenderContext.toDpF(sizeF.height),
                    ACTIVITY_WIDTH to kuiklyRenderContext.toDpF(activitySize.width.toFloat()),
                    ACTIVITY_HEIGHT to kuiklyRenderContext.toDpF(activitySize.height.toFloat()),
                    DEVICE_WIDTH to kuiklyRenderContext.toDpF(deviceSize.width.toFloat()),
                    DEVICE_HEIGHT to kuiklyRenderContext.toDpF(deviceSize.height.toFloat())
                )
            )
            lastSize = sizeF
        }
    }

    private fun initRenderCore(
        contextCode: String,
        url: String,
        params: Map<String, Any>,
        size: SizeF,
        assetsPath: String?
    ) {
        dispatchLifecycleStateChanged(STATE_INIT_CORE_START)
        val contextParams = KuiklyContextParams(executeMode, url, params, assetsPath)
        kuiklyRenderViewContext.initContextParams(contextParams)
        val contextInitCallback = object : IKuiklyRenderContextInitCallback {

            override fun onStart() {
                dispatchLifecycleStateChanged(STATE_INIT_CONTEXT_START)
            }

            override fun onFinish() {
                dispatchLifecycleStateChanged(STATE_INIT_CONTEXT_FINISH)
            }

            override fun onCreateInstanceStart() {
                dispatchLifecycleStateChanged(STATE_CREATE_INSTANCE_START)
            }

            override fun onCreateInstanceFinish() {
                dispatchLifecycleStateChanged(STATE_CREATE_INSTANCE_FINISH)
            }

        }
        this.contextInitCallback = contextInitCallback
        renderCore = createRenderCore().apply {
            init(
                this@KuiklyRenderView,
                contextCode,
                url,
                generateWithParams(params, size),
                assetsPath,
                contextInitCallback)
        }
        dispatchLifecycleStateChanged(STATE_INIT_CORE_FINISH)
        trySendCoreEventList() // 尝试发送lazy事件，如果有的话
        performCoreLazyTaskList()
    }

    private fun createRenderCore(): IKuiklyRenderCore =
        KuiklyRenderCore(executeMode.getContextHandler()).apply {
            setRenderExceptionListener(exceptionListener)
        }

    private fun generateWithParams(params: Map<String, Any>, size: SizeF): Map<String, Any> {
        val contentView = context.activity?.findViewById<View>(android.R.id.content)
        return mutableMapOf<String, Any>().apply {
            put(ROOT_VIEW_WIDTH, kuiklyRenderViewContext.toDpF(size.width))
            put(ROOT_VIEW_HEIGHT, kuiklyRenderViewContext.toDpF(size.height))
            put(STATUS_BAR_HEIGHT, kuiklyRenderViewContext.toDpF(context.statusBarHeight.toFloat()))
            put(PLATFORM, "android")
            put(DEVICE_WIDTH, kuiklyRenderViewContext.toDpF(context.screenWidth.toFloat()))
            put(DEVICE_HEIGHT, kuiklyRenderViewContext.toDpF(context.screenHeight.toFloat()))
            put(OS_VERSION, Build.VERSION.SDK_INT.toString())
            put(APP_VERSION, context.versionName)
            put(PARAMS, params)
            put(NATIVE_BUILD, 8)
            put(SAFE_AREA_INSETS, "${kuiklyRenderContext.toDpF(context.statusBarHeight.toFloat())} 0 0 0")
            put(ACTIVITY_WIDTH, kuiklyRenderContext.toDpF((contentView?.width ?: 0).toFloat()))
            put(ACTIVITY_HEIGHT, kuiklyRenderContext.toDpF((contentView?.height ?: 0).toFloat()))
            put(DENSITY, Resources.getSystem().displayMetrics.density)
            put(ACCESSIBILITY_RUNNING, if (isAccessibilityRunning()) 1 else 0)
        }
    }

    private fun trySendCoreEventList() {
        val lazyEventList = coreEventLazyEventList ?: return
        for (event in lazyEventList) {
            renderCore?.sendEvent(event.first, event.second)
        }
        coreEventLazyEventList = null // 及时释放掉无用的内存
    }

    private fun addTaskWhenCoreDidInit(task: RenderCoreLazyTask) {
        if (renderCore != null) {
            task(renderCore!!)
        } else {
            coreLazyTaskList.add(task)
        }
    }

    private fun performCoreLazyTaskList() {
        renderCore?.also {
            for (task in coreLazyTaskList) {
                task(it)
            }
            coreLazyTaskList.clear()
        }
    }

    fun registerCallback(callback: IKuiklyRenderViewLifecycleCallback) {
        lifecycleCallbacks.add(callback)
    }

    fun unregisterCallback(callback: IKuiklyRenderViewLifecycleCallback) {
        lifecycleCallbacks.remove(callback)
    }

    fun setViewTreeUpdateListener(listener: IKuiklyRenderViewTreeUpdateListener) {
        renderCore?.setViewTreeUpdateListener(listener)
    }

    fun setKotlinBridgeStatusListener(listener: IKotlinBridgeStatusListener) {
        renderCore?.setKotlinBridgeStatusListener(listener)
    }

    private fun dispatchLifecycleStateChanged(state: Int, params: Any? = null) {
        when (state) {
            STATE_INIT -> {
                lifecycleCallbacks.forEach {
                    it.onInit()
                }
            }
            STATE_PRELOAD_CLASS_FINISH -> {
                lifecycleCallbacks.forEach {
                    it.onPreloadClassFinish()
                }
            }
            STATE_INIT_CORE_START -> {
                lifecycleCallbacks.forEach {
                    it.onInitCoreStart()
                }
            }
            STATE_INIT_CORE_FINISH -> {
                lifecycleCallbacks.forEach {
                    it.onInitCoreFinish()
                }
            }
            STATE_INIT_CONTEXT_START -> {
                lifecycleCallbacks.forEach {
                    it.onInitContextStart()
                }
            }
            STATE_INIT_CONTEXT_FINISH -> {
                lifecycleCallbacks.forEach {
                    it.onInitContextFinish()
                }
            }
            STATE_CREATE_INSTANCE_START -> {
                lifecycleCallbacks.forEach {
                    it.onCreateInstanceStart()
                }
            }
            STATE_CREATE_INSTANCE_FINISH -> {
                lifecycleCallbacks.forEach {
                    it.onCreateInstanceFinish()
                }
            }
            STATE_FIRST_FRAME_PAINT -> {
                lifecycleCallbacks.forEach {
                    it.onFirstFramePaint()
                }
            }
            STATE_RESUME -> {
                lifecycleCallbacks.forEach {
                    it.onResume()
                }
            }
            STATE_PAUSE -> {
                lifecycleCallbacks.forEach {
                    it.onPause()
                }
            }
            STATE_DESTROY -> {
                lifecycleCallbacks.forEach {
                    it.onDestroy()
                }
            }
        }
    }

    private fun initKuiklyClassLoaderIfNeed(contextCode: String) {
        if (enablePreloadClass) {
            executeMode.initClassLoaderIfNeed(contextCode)
        }
        dispatchLifecycleStateChanged(STATE_PRELOAD_CLASS_FINISH)
    }

    private fun isAccessibilityRunning(): Boolean {
        if (context == null) {
            return false
        }
        try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            return am.isEnabled && am.isTouchExplorationEnabled
        } catch (t: Throwable) {
            // do nothing
        }
        return false
    }

    private fun getActivitySize(): Size {
        val contentView = context.activity?.findViewById<View>(android.R.id.content) ?: return Size(0, 0)
        return Size(contentView.width, contentView.height)
    }

    private fun getDeviceSize(): Size {
        return getDisplaySize(context)
    }

    fun onBackPressed() {
        sendEvent(ON_BACK_PRESSED, mapOf())
    }

    companion object {
        private const val EVENT_ROOT_VIEW_SIZE_CHANGED = "rootViewSizeDidChanged"
        private const val ROOT_VIEW_WIDTH = "rootViewWidth"
        private const val ROOT_VIEW_HEIGHT = "rootViewHeight"
        const val STATUS_BAR_HEIGHT = "statusBarHeight"
        private const val ANDROID_BOTTOM_NAV_BAR_HEIGHT = "androidBottomNavBarHeight"
        private const val PLATFORM = "platform"
        private const val DEVICE_WIDTH = "deviceWidth"
        private const val DEVICE_HEIGHT = "deviceHeight"
        private const val OS_VERSION = "osVersion"
        private const val APP_VERSION = "appVersion"
        private const val PARAMS = "param"
        private const val NATIVE_BUILD = "nativeBuild"
        private const val SAFE_AREA_INSETS = "safeAreaInsets"
        private const val ACTIVITY_WIDTH = "activityWidth"
        private const val ACTIVITY_HEIGHT = "activityHeight"
        private const val SCALE_FONT_ENABLE = "scaleFontEnable"
        private const val VIEW_DID_DISAPPEAR = "viewDidDisappear"
        private const val VIEW_DID_DISAPPEAR_VALUE = "1"

        private const val VIEW_DID_APPEAR = "viewDidAppear"
        private const val VIEW_DID_APPEAR_VALUE = "1"
        private const val DENSITY = "density"

        const val PAGER_EVENT_FIRST_FRAME_PAINT = "pageFirstFramePaint"
        private const val ACCESSIBILITY_RUNNING = "isAccessibilityRunning" // 无障碍化是否开启

        private const val ON_BACK_PRESSED = "onBackPressed"

        // RenderView 生命周期状态
        private const val STATE_INIT = 0
        private const val STATE_PRELOAD_CLASS_FINISH = STATE_INIT + 1
        private const val STATE_INIT_CORE_START = STATE_PRELOAD_CLASS_FINISH + 1
        private const val STATE_INIT_CORE_FINISH = STATE_INIT_CORE_START + 1
        private const val STATE_INIT_CONTEXT_START = STATE_INIT_CORE_FINISH + 1
        private const val STATE_INIT_CONTEXT_FINISH = STATE_INIT_CONTEXT_START + 1
        private const val STATE_CREATE_INSTANCE_START = STATE_INIT_CONTEXT_FINISH + 1
        private const val STATE_CREATE_INSTANCE_FINISH = STATE_CREATE_INSTANCE_START + 1
        private const val STATE_FIRST_FRAME_PAINT = STATE_CREATE_INSTANCE_FINISH + 1
        private const val STATE_RESUME = STATE_FIRST_FRAME_PAINT + 1
        private const val STATE_PAUSE = STATE_RESUME + 1
        private const val STATE_DESTROY = STATE_PAUSE + 1

    }
}

class KuiklyRenderViewContext(
    context: Context,
    kuiklyRenderView: IKuiklyRenderView,
    private val enableContextReplace: Boolean = false,
    private val useHostDisplayMetrics: Boolean = false
) :
    ContextWrapper(context), IKuiklyRenderContext {

    private var contextParams : KuiklyContextParams? = null

    /**
     * 保存关联到View对象的数据
     */
    private val viewData = SparseArray<ArrayMap<String, Any>>()

    private var imageLoader: KRImageLoader? = null

    private var typeFaceLoader: TypeFaceLoader? = null

    /**
     * [IKuiklyRenderView]弱引用
     */
    private val kuiklyRenderViewWeakRef = WeakReference(kuiklyRenderView)

    override val context: Context
        get() = this

    override val kuiklyRenderRootView: IKuiklyRenderView?
        get() = kuiklyRenderViewWeakRef.get()

    /**
     * 实际真正的 Context
     */
    private var customBaseContext: Context? = null

    init {
        if (enableContextReplace) {
            customBaseContext = context
        }
    }

    override fun getBaseContext(): Context? {
        return if (enableContextReplace) {
            customBaseContext ?: super.getBaseContext()
        } else {
            super.getBaseContext()
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        if (enableContextReplace) {
            customBaseContext = base
        }
    }

    override fun initContextParams(contextParams: KuiklyContextParams) {
        this.contextParams = contextParams
    }

    override fun replaceContext(newContext: Context) {
        if (enableContextReplace) {
            customBaseContext = newContext
        }
    }

    override fun useHostDisplayMetrics(): Boolean {
        return useHostDisplayMetrics
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getViewData(view: View, key: String): T? =
        viewData[view.hashCode()]?.get(key) as? T?

    override fun putViewData(view: View, key: String, data: Any) {
        val hashCode = view.hashCode()
        val map = viewData[hashCode] ?: ArrayMap<String, Any>().apply {
            viewData.put(hashCode, this)
        }
        map[key] = data
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> removeViewData(view: View, key: String): T? =
        viewData[view.hashCode()]?.remove(key) as? T?

    override fun clearViewData(view: View) {
        viewData.delete(view.hashCode())
    }

    override fun <T : KuiklyRenderBaseModule> module(name: String): T? =
        kuiklyRenderViewWeakRef.get()?.module(name)

    override fun <T : TDFBaseModule> getTDFModule(name: String): T? =
        kuiklyRenderViewWeakRef.get()?.getTDFModule(name)

    override fun getView(tag: Int): View? = kuiklyRenderViewWeakRef.get()?.getView(tag)

    override fun getImageLoader(): KRImageLoader? {
        if (imageLoader == null) {
            contextParams?.let {
                imageLoader = KRImageLoader(it.executeMode, it.assetsPath)
            }
        }
        return imageLoader
    }

    override fun getTypeFaceLoader(): TypeFaceLoader? {
        if (typeFaceLoader == null) {
            typeFaceLoader = TypeFaceLoader(contextParams)
        }
        return typeFaceLoader
    }

}

class KuiklyRenderExport(private val renderContext: IKuiklyRenderContext) : IKuiklyRenderExport {

    /**
     * 保存ViewName -> [IKuiklyRenderViewExport]闭包的Map, 用于创建Native暴露给KTV页面的View
     */
    private val viewExportCreator = ArrayMap<String, (Context) -> IKuiklyRenderViewExport>()

    /**
     * 保存ViewName -> [IKuiklyRenderShadowExport]闭包的map，用于创建Native暴露给KTV页面的shadow
     */
    private val shadowExportCreator = ArrayMap<String, () -> IKuiklyRenderShadowExport>()

    /**
     * 保存ModuleName -> [IKuiklyRenderModuleExport]闭包的map，用于创建Native暴露给KTV页面的module
     */
    private val moduleExportCreator = ArrayMap<String, () -> IKuiklyRenderModuleExport>()

    /**
     * View的自定义属性处理器，方便宿主可以对任意的[IKuiklyRenderViewExport]实现类的prop进行扩展
     */
    private val viewPropExternalHandlerList = mutableListOf<IKuiklyRenderViewPropExternalHandler>()

    /**
     * TDF 通用 Module 管理
     */
    private val tdfModuleManager = TDFModuleManager(object : TDFModuleContext() {

        override fun getRootView(): View? {
            return renderContext.kuiklyRenderRootView?.view
        }

        override fun getModule(moduleName: String?): TDFBaseModule? {
            if (moduleName == null) {
                return null
            }
            // TODO: 待优化
            return renderContext.kuiklyRenderRootView?.kuiklyRenderExport?.getTDFModule(moduleName)
        }

        override fun getView(id: Int): View? {
            return renderContext.getView(id)
        }

        override fun sendEvent(eventName: String, params: MutableMap<String, Any>) {
            renderContext.kuiklyRenderRootView?.sendEvent(eventName, params)
        }

    })

    override fun registerTDFModule(clazz: Class<out TDFBaseModule>, provider: TDFModuleProvider) {
        tdfModuleManager.addModule(clazz, provider)
    }

    override fun getTDFModule(moduleName: String): TDFBaseModule {
        return tdfModuleManager.getModule(moduleName)
            ?: throw KuiklyRenderModuleExportException("can not find TDFModule, name: $moduleName")
    }

    override fun moduleExport(name: String, creator: () -> IKuiklyRenderModuleExport) {
        moduleExportCreator[name] = creator
    }

    override fun renderViewExport(
        viewName: String,
        renderViewExportCreator: (Context) -> IKuiklyRenderViewExport,
        shadowExportCreator: (() -> IKuiklyRenderShadowExport)?
    ) {
        viewExportCreator[viewName] = renderViewExportCreator
        shadowExportCreator?.also {
            this.shadowExportCreator[viewName] = it
        }
    }

    override fun viewPropExternalHandlerExport(handler: IKuiklyRenderViewPropExternalHandler) {
        viewPropExternalHandlerList.add(handler)
    }

    override fun createModule(name: String): IKuiklyRenderModuleExport {
        return moduleExportCreator[name]?.invoke()
            ?: throw KuiklyRenderModuleExportException("can not find moduleExport, name: $name")
    }

    override fun createRenderView(name: String, context: Context): IKuiklyRenderViewExport {
        return viewExportCreator[name]?.invoke(context)
            ?: throw KuiklyRenderViewExportException("can not find viewExport, name: $name")
    }

    override fun createRenderShadow(name: String): IKuiklyRenderShadowExport {
        val shadow = shadowExportCreator[name]?.invoke()
            ?: throw KuiklyRenderShadowExportException("can not find shadowExport, name: $name")
        if (shadow is IKuiklyRenderContextWrapper) {
            shadow.kuiklyRenderContext = renderContext
        }
        return shadow
    }

    override fun setViewExternalProp(
        renderViewExport: IKuiklyRenderViewExport,
        propKey: String,
        propValue: Any
    ): Boolean {
        for (handler in viewPropExternalHandlerList) {
            if (handler.setViewExternalProp(renderViewExport, propKey, propValue)) {
                return true
            }
        }
        return false
    }

    override fun resetViewExternalProp(
        renderViewExport: IKuiklyRenderViewExport,
        propKey: String
    ): Boolean {
        for (handler in viewPropExternalHandlerList) {
            if (handler.resetViewExternalProp(renderViewExport, propKey)) {
                return true
            }
        }
        return false
    }

}

private typealias InitRenderCoreLazyTask = (size: SizeF) -> Unit
private typealias RenderCoreLazyEvent = Pair<String, Map<String, Any>>
private typealias RenderCoreLazyTask = (core: IKuiklyRenderCore) -> Unit