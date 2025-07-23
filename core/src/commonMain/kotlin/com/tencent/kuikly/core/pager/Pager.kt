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

package com.tencent.kuikly.core.pager

import com.tencent.kuikly.core.base.*
import com.tencent.kuikly.core.collection.fastArrayListOf
import com.tencent.kuikly.core.collection.fastHashMapOf
import com.tencent.kuikly.core.collection.fastHashSetOf
import com.tencent.kuikly.core.collection.toFastList
import com.tencent.kuikly.core.coroutines.LifecycleScope
import com.tencent.kuikly.core.exception.throwRuntimeError
import com.tencent.kuikly.core.global.GlobalFunctions
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.manager.Task
import com.tencent.kuikly.core.manager.TaskManager
import com.tencent.kuikly.core.module.*
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.timer.setTimeout

abstract class Pager : ComposeView<ComposeAttr, ComposeEvent>(), IPager {
    open var ignoreLayout: Boolean = false
    private var fontWeightScale = -1.0
    private var fontSizeScale = -1.0

    private val modulesMap = fastHashMapOf<String, Module>()
    private val moduleCreatorMap = fastHashMapOf<String, IModuleCreator>()
    private val viewCreatorMap by lazy(LazyThreadSafetyMode.NONE) {
        fastHashMapOf<String, IViewCreator>()
    }
    private val nativeRefViewMap = fastHashMapOf<Int, AbstractBaseView<*, *>>()
    override var pageData = PageData()
    override var pageName: String = ""
    override val lifecycleScope: LifecycleScope by lazy(LazyThreadSafetyMode.NONE) {
        LifecycleScope(this)
    }
    override var animationManager: AnimationManager? = null
    private var pagerEventObserverSet = fastHashSetOf<IPagerEventObserver>()
    private var layoutEventObserverSet = fastHashSetOf<IPagerLayoutEventObserver>()
    private var taskManager: TaskManager? = null
    private var layoutFinishTasks = fastArrayListOf<() -> Unit>()
    private var didCalculateLayoutTasks = fastArrayListOf<() -> Unit>()
    private val keyValueMap = fastHashMapOf<String, Any>()
    private var willDestroy = false
    private var pageTrace : PageCreateTrace? = null
    override val isDebugUIInspector by lazy { debugUIInspector() } // debug ui
    override var didCreateBody: Boolean = false

    override fun createAttr(): ComposeAttr = ComposeAttr()

    /**
     * call from native
     */
    override fun onCreatePager(pagerId: String, pageData: JSONObject) {
        pageTrace?.onCreateStart()
        this.pagerId = pagerId
        this.pageData.init(pageData)
        taskManager = TaskManager(pagerId)
        willInit()
        initModule() // 1.初始化 module
        didMoveToParentView()
        didInit() // 构建shadow树
        createBody()
        didCreateBody = true
        pageTrace?.onCreateEnd()
    }

    override fun onDestroyPager() {
        willDestroy = true
        pageWillDestroy()
        willRemoveFromParentView()
        didRemoveFromParentView()
        pagerEventObserverSet.clear()
        layoutEventObserverSet.clear()
        GlobalFunctions.destroyGlobalFunction(pagerId)
        taskManager?.destroy()
        flexNode.setNeedDirtyCallback = null
        nativeRefViewMap.clear()
        modulesMap.clear()
        keyValueMap.clear()
        animationManager?.destroy()
    }

    override fun onLayoutView() {
        layoutIfNeed()
    }

    override fun onFirstFramePaint() {
        PagerManager.getCurrentPager().acquireModule<PerformanceModule>(PerformanceModule.MODULE_NAME).getPerformanceData {
            KLog.d(pageName, "${it?.pageLoadTime}")
        }
    }

    override fun onViewEvent(viewRef: Int, event: String, res: JSONObject?) {
        getViewWithNativeRef(viewRef)?.onFireEvent(event, res)
    }

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }

    //
    override fun <T : Module> getModule(name: String): T? {
        createModuleIfNeed(name)
        return modulesMap[name] as? T?
    }

    override fun <T : Module> acquireModule(name: String): T {
        createModuleIfNeed(name)
        if (!modulesMap.containsKey(name)) {
            throwRuntimeError("acquireModule 失败：$name 未注册，请在重写Pager.createExternalModules方法时机中添加注册(调用Pager.registerModule方法注册)")
        }
        return modulesMap[name] as T
    }

    /**
     * 注册扩展module接口，（注：注册时机为override Pager.createExternalModules中统一注册）
     */
    fun registerModule(moduleName: String, moduleCreator: IModuleCreator) {
        moduleCreatorMap[moduleName] = moduleCreator
    }
    /**
     * 注册扩展内置View能力, 用于宿主接入工程在原接口不变情况下扩展内置View组件能力
     * 注：请注意该方法调用在Pager#body方法之前注册，建议为Pager#created时注册
     * @param viewClassName  sdk内置的View类名，取自ViewConst.TYPE_XXX_CLASS_NAME，如：ViewConst.TYPE_TEXT_CLASS_NAME 则是扩展 Text{} 组件的TextView创建
     * @param viewCreator view实例创建器
     */
    fun registerViewCreator(viewClassName: String, viewCreator: IViewCreator) {
        viewCreatorMap[viewClassName] = viewCreator
    }

    override fun createExternalModules(): Map<String, Module>? {
        return null
    }

    override fun createViewFromRegister(viewClassName: String): DeclarativeBaseView<*, *>? {
        if (viewCreatorMap.isEmpty()) {
            return null
        }
        val creator = viewCreatorMap[viewClassName]
        if (creator != null) {
            return creator.createView()
        }
        return null
    }

    override fun pageDidAppear() {
    }

    override fun pageDidDisappear() {
    }

    override fun pageWillDestroy() {
        super.pageWillDestroy()
    }

    /*
    * 同步UI上屏操作(UI队列中任务立即执行上屏，用于当前帧提前同步上屏优化)
    */
    override fun syncFlushUI() {
       BridgeManager.callSyncFlushUIMethod(pagerId)
    }

    override fun themeDidChanged(data: JSONObject) {
    }

    override fun bindValueChange(
        valueBlock: () -> Any,
        byOwner: Any,
        valueChange: (value: Any) -> Unit
    ) {
        PagerManager.getCurrentReactiveObserver().bindValueChange(valueBlock, byOwner, valueChange)
    }

    override fun unbindAllValueChange(byOwner: Any) {
        PagerManager.getCurrentReactiveObserver().unbindValueChange(byOwner)
    }

    override fun addPagerEventObserver(observer: IPagerEventObserver) {
        pagerEventObserverSet.add(observer)
    }

    override fun removePagerEventObserver(observer: IPagerEventObserver) {
        pagerEventObserverSet.remove(observer)
    }

    override fun addPagerLayoutEventObserver(observer: IPagerLayoutEventObserver) {
        layoutEventObserverSet.add(observer)
    }

    override fun removePagerLayoutEventObserver(observer: IPagerLayoutEventObserver) {
        layoutEventObserverSet.remove(observer)
    }

    override fun onReceivePagerEvent(pagerEvent: String, eventData: JSONObject) {
        pagerEventObserverSet.toFastList().forEach { observer ->
            observer.onPagerEvent(pagerEvent, eventData)
        }
        when (pagerEvent) {
            PAGER_EVENT_DID_APPEAR -> pageDidAppear()
            PAGER_EVENT_DID_DISAPPEAR -> pageDidDisappear()
            PAGER_EVENT_THEME_DID_CHANGED -> themeDidChanged(eventData)
            PAGER_EVENT_FIRST_FRAME_PAINT -> {
                onFirstFramePaint()
            }
            PAGER_EVENT_ROOT_VIEW_SIZE_CHANGED -> {
                handlePagerViewSizeDidChanged(
                    eventData,
                    eventData.optDouble(WIDTH),
                    eventData.optDouble(HEIGHT),
                    eventData.optString(SAFE_AREA_INSETS, ""),
                    eventData.optString(DENSITY_INFO, "")
                )
            }
            PAGER_EVENT_WINDOW_SIZE_CHANGED -> {
                handlePagerWindowSizeDidChanged(eventData.optDouble(WIDTH),eventData.optDouble(
                    HEIGHT))
            }
            PAGER_EVENT_CONFIGURATION_DID_CHANGED -> {
                handlePagerConfigurationDidchanged(eventData)
            }
        }
    }

    override fun putNativeViewRef(nativeRef: Int, view: AbstractBaseView<*, *>) {
        nativeRefViewMap[nativeRef] = view
    }

    override fun removeNativeViewRef(nativeRef: Int) {
        nativeRefViewMap.remove(nativeRef)
    }

    override fun getViewWithNativeRef(nativeRef: Int): AbstractBaseView<*, *>? {
        return nativeRefViewMap[nativeRef]
    }

    override fun addNextTickTask(task: Task) {
        taskManager?.nextTick(task)
    }

    override fun addTaskWhenPagerUpdateLayoutFinish(task: () -> Unit) {
        layoutFinishTasks.add(task)
    }

    override fun addTaskWhenPagerDidCalculateLayout(task: () -> Unit) {
        didCalculateLayoutTasks.add(task)
    }

    override fun setMemoryCache(key: String, value: Any) {
        keyValueMap[key] = value
        acquireModule<MemoryCacheModule>(MemoryCacheModule.MODULE_NAME).setObject(key, value)
    }

    override fun getValueForKey(key: String): Any? {
        return keyValueMap[key]
    }

    private fun initModule() {
        initCoreModules()
        initExternalModules()
        injectVarToModule()
    }

    private fun injectVarToModule() {
        modulesMap.forEach { (_, module) ->
           injectVarToModule(module)
        }
    }

    private fun injectVarToModule(module: Module) {
        module.injectVar(pagerId, pageData)
    }

    private fun createModuleIfNeed(name: String) {
        if (!modulesMap.containsKey(name)) {
            // lazy create module
            if (moduleCreatorMap.containsKey(name)) {
                moduleCreatorMap[name]?.also {
                    val module  = it.createModule()
                    modulesMap[name] = module
                    injectVarToModule(module)
                }
            }
        }
    }
    private fun initCoreModules() {
        registerModule(ModuleConst.NOTIFY, object : IModuleCreator {
            override fun createModule(): Module {
                return NotifyModule()
            }
        })
        registerModule(ModuleConst.MEMORY, object : IModuleCreator {
            override fun createModule(): Module {
                return MemoryCacheModule()
            }
        })
        registerModule(ModuleConst.SHARED_PREFERENCES, object : IModuleCreator {
            override fun createModule(): Module {
                return SharedPreferencesModule()
            }
        })
        registerModule(ModuleConst.SNAPSHOT, object : IModuleCreator {
            override fun createModule(): Module {
                return SnapshotModule()
            }
        })
        registerModule(ModuleConst.ROUTER, object : IModuleCreator {
            override fun createModule(): Module {
                return RouterModule()
            }
        })
        registerModule(ModuleConst.NETWORK, object : IModuleCreator {
            override fun createModule(): Module {
                return NetworkModule()
            }
        })
        registerModule(ModuleConst.CODEC, object : IModuleCreator {
            override fun createModule(): Module {
                return CodecModule()
            }
        })
        registerModule(ModuleConst.CALENDAR, object : IModuleCreator {
            override fun createModule(): Module {
                return CalendarModule()
            }
        })
        registerModule(ModuleConst.REFLECTION, object : IModuleCreator {
            override fun createModule(): Module {
                return ReflectionModule()
            }
        })
        registerModule(ModuleConst.PERFORMANCE, object : IModuleCreator {
            override fun createModule(): Module {
                return PerformanceModule()
            }
        })
        registerModule(ModuleConst.TURBO_DISPLAY, object : IModuleCreator {
            override fun createModule(): Module {
                return TurboDisplayModule()
            }
        })
        registerModule(ModuleConst.FONT, object : IModuleCreator {
            override fun createModule(): Module {
                return FontModule()
            }
        })
        registerModule(ModuleConst.VSYNC, object : IModuleCreator {
            override fun createModule(): Module {
                return VsyncModule()
            }
        })
        registerModule(ModuleConst.BACK_PRESS, object : IModuleCreator {
            override fun createModule(): Module {
                return BackPressModule()
            }
        })
    }
    private fun initExternalModules() {
        createExternalModules()?.also { map ->
            modulesMap.putAll(map)
        }
    }

    private fun createBody() {
        createFlexNode()
        createRenderView()
        renderView?.insertToRootView()
        pageTrace?.onLayoutStart()
        layoutIfNeed()
        pageTrace?.onLayoutEnd()
        flexNode.setNeedDirtyCallback = {
            renderView?.setEvent(PAGER_EVENT_SET_NEED_LAYOUT)
        }
    }

    private fun layoutIfNeed() {
        if (ignoreLayout) {
            return
        }
        var maxLoopTimes = 3
        while (flexNode.isDirty && (--maxLoopTimes) >= 0) {
            notifyPagerWillCalculateLayoutObservers()
            flexNode.calculateLayout(null)
            notifyPagerCalculateLayoutFinishObservers()
            performDidCalculateLayoutTasks()
            notifyPagerDidLayoutObservers()
            performLayoutFinishTasks()
        }
        if (flexNode.isDirty) {
            performTask(true) {
                layoutIfNeed()
            }
        }
    }

    private fun performTask(async: Boolean, task: () -> Unit) {
        if (async) {
            setTimeout(pagerId, 1, task)
        } else {
            task()
        }
    }

    private fun performDidCalculateLayoutTasks() {
        val tasks = didCalculateLayoutTasks.toFastList()
        didCalculateLayoutTasks.clear()
        tasks.forEach {
            it()
        }
    }

    private fun performLayoutFinishTasks() {
        val tasks = layoutFinishTasks.toFastList()
        layoutFinishTasks.clear()
        tasks.forEach {
            it()
        }
    }

    private fun notifyPagerDidLayoutObservers() {
        layoutEventObserverSet.toFastList().forEach { observer ->
            observer.onPagerDidLayout()
        }
    }

    private fun notifyPagerWillCalculateLayoutObservers() {
        layoutEventObserverSet.toFastList().forEach { observer ->
            observer.onPagerWillCalculateLayoutFinish()
        }
    }

    private fun notifyPagerCalculateLayoutFinishObservers() {
        layoutEventObserverSet.toFastList().forEach { observer ->
            observer.onPagerCalculateLayoutFinish()
        }
    }

    override fun didInit() {
        pageTrace?.onBuildStart()
        super<ComposeView>.didInit()
        setupRootViewSizeStyle()
        pageTrace?.onBuildEnd()
    }

    override fun isWillDestroy(): Boolean {
        return willDestroy
    }

    override fun setPageTrace(pageTrace: PageCreateTrace) {
        this.pageTrace = pageTrace
    }

    private fun setupRootViewSizeStyle() {
        val ctx = this
        attr {
            width(ctx.pageData.pageViewWidth)
            height(ctx.pageData.pageViewHeight)
        }
    }

    // window size change event
    private fun handlePagerWindowSizeDidChanged(width: Double, height: Double){
        if(width.toFloat() != pageData.activityWidth){
            pageData.activityWidth = width.toFloat()
        }
        if(height.toFloat() != pageData.activityHeight){
            pageData.activityHeight = height.toFloat()
        }
    }

    private fun handlePagerConfigurationDidchanged(config: JSONObject){
        val platform = config.optString("platform")
        val fontWeightScale = config.optDouble("fontWeightScale", -1.0)
        val fontSizeScale = config.optDouble("fontSizeScale", -1.0)

        var shouldMarkTextDirty = false
        if(fontWeightScale > 0 && fontWeightScale != this.fontWeightScale){
            // fontWeightScale changed
            this.fontWeightScale = fontWeightScale
            shouldMarkTextDirty = true
        }
        if(fontSizeScale > 0 && fontSizeScale != this.fontSizeScale){
            // fontSizeScale changed
            this.fontSizeScale = fontSizeScale
            shouldMarkTextDirty = true
        }
        if(shouldMarkTextDirty){
            flexNode.markDirty()
            markChildTextViewsDirty()
        }
        layoutIfNeed()
    }

    // pager event
    private fun handlePagerViewSizeDidChanged(data: JSONObject, width: Double, height: Double, safeAreaInsetsString: String, densityInfo: String) {
        if (safeAreaInsetsString.isNotEmpty()) {
            pageData.safeAreaInsets = EdgeInsets.decodeWithString(safeAreaInsetsString)
        }
        pageData.updateRootViewSize(data, width, height)
        setupRootViewSizeStyle()
        if(densityInfo.isNotEmpty()) {
            val info = JSONObject(densityInfo)
            val newDensity = info.optDouble(DENSITY_INFO_KEY_NEW_DENSITY)
            pageData.density = newDensity.toFloat()
            flexNode.markDirty()
            markChildTextViewsDirty()
            layoutIfNeed()
        }
    }

    companion object {
        const val PAGER_EVENT_WINDOW_SIZE_CHANGED = "windowSizeDidChanged"
        const val PAGER_EVENT_ROOT_VIEW_SIZE_CHANGED = "rootViewSizeDidChanged"
        const val PAGER_EVENT_DID_APPEAR = "viewDidAppear"
        const val PAGER_EVENT_DID_DISAPPEAR = "viewDidDisappear"
        const val PAGER_EVENT_FIRST_FRAME_PAINT = "pageFirstFramePaint"
        const val PAGER_EVENT_THEME_DID_CHANGED = "themeDidChanged"
        const val PAGER_EVENT_WILL_DESTROY = "pageWillDestroy"
        const val PAGER_EVENT_SET_NEED_LAYOUT = "setNeedLayout"
        const val PAGER_EVENT_CONFIGURATION_DID_CHANGED = "configurationDidChanged"
        const val WIDTH = "width"
        const val HEIGHT = "height"
        const val SAFE_AREA_INSETS = "safeAreaInsets"
        const val DENSITY_INFO = "densityInfo"
        const val DENSITY_INFO_KEY_NEW_DENSITY = "newDensity"
    }
}

// 定义一个模块创建器接口
interface IModuleCreator {
    fun createModule(): Module
}
// 定义一个视图创建器接口
interface IViewCreator {
    fun createView(): DeclarativeBaseView<*, *>
}
