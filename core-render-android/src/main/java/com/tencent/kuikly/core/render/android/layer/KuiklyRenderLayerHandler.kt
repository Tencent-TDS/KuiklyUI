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

package com.tencent.kuikly.core.render.android.layer

import android.graphics.Rect
import android.graphics.RectF
import android.util.ArrayMap
import android.util.SizeF
import android.util.SparseArray
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup
import com.tencent.kuikly.core.render.android.IKuiklyRenderView
import com.tencent.kuikly.core.render.android.KuiklyContextParams
import com.tencent.kuikly.core.render.android.const.KRCssConst
import com.tencent.kuikly.core.render.android.css.ktx.isMainThread
import com.tencent.kuikly.core.render.android.css.ktx.removeFromParent
import com.tencent.kuikly.core.render.android.css.ktx.toDpSizeF
import com.tencent.kuikly.core.render.android.css.ktx.toPxI
import com.tencent.kuikly.core.render.android.css.ktx.toPxSizeF
import com.tencent.kuikly.core.render.android.css.ktx.toTDFModuleCallResult
import com.tencent.kuikly.core.render.android.css.ktx.viewGroup
import com.tencent.kuikly.core.render.android.expand.module.KRTDFModulePromise
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderModuleExport
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderShadowExport
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderViewExport
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import com.tencent.tdf.module.TDFBaseModule
import com.tencent.tdf.utils.TDFListUtils
import java.lang.ref.WeakReference
import java.util.concurrent.locks.ReentrantReadWriteLock

class KuiklyRenderLayerHandler : IKuiklyRenderLayerHandler {

    /**
     * [IKuiklyRenderView]弱引用
     */
    private var renderViewWeakRef: WeakReference<IKuiklyRenderView>? = null

    /**
     * 缓存已经创建的[RenderViewHandler]
     */
    private val renderViewRegistry = SparseArray<RenderViewHandler>()

    /**
     * 缓存已经创建的[IKuiklyRenderShadowExport]
     */
    private var shadowRegistry: SparseArray<IKuiklyRenderShadowExport>? = null

    /**
     * 缓存已经创建的[IKuiklyRenderModuleExport]
     */
    private val moduleRegistry = ArrayMap<String, IKuiklyRenderModuleExport>()

    /**
     * [moduleRegistry]的读写锁，用于确保[IKuiklyRenderModuleExport]中的同步方法的正确性
     */
    private val moduleRegistryWRLock = ReentrantReadWriteLock()

    /**
     * [RenderViewHandler]复用队列的map
     */
    private val renderViewReuseListMap = ArrayMap<String, MutableList<RenderViewHandler>>()

    override fun init(renderView: IKuiklyRenderView) {
        renderViewWeakRef = WeakReference(renderView)
    }

    override fun createRenderView(tag: Int, viewName: String) {
        assert(isMainThread()) { // assert中的字符串不定义成常量，因为assert在运行的时候会被移除，定义成常量的话，会出现无用常量
            "must call on ui thread"
        }
        assert(getRenderViewHandler(tag) == null) {
            "tag of handler should be null"
        }
        createRenderViewHandler(tag, viewName)
    }

    override fun removeRenderView(tag: Int) {
        assert(isMainThread()) {
            "must call on ui thread"
        }
        innerRemoveRenderView(tag)
    }

    override fun insertSubRenderView(parentTag: Int, childTag: Int, index: Int) {
        val isRootViewTag = parentTag == ROOT_VIEW_TAG
        assert(isRootViewTag || getRenderViewHandler(parentTag) != null) {
            "tag of handler can not be null"
        }
        assert(getRenderViewHandler(childTag) != null) {
            "tag of handler can not be null"
        }

        val parentView = if (isRootViewTag) {
            renderViewWeakRef?.get()?.view ?: return
        } else {
            getRenderViewHandler(parentTag)?.viewExport?.viewGroup ?: return
        }
        val viewExport = getRenderViewHandler(childTag)?.viewExport ?: return
        val childView = viewExport.view()

        var insertIndex = index
        if (index > parentView.childCount || index == -1) { // index为-1表示末尾添加
            insertIndex = parentView.childCount
        }
        parentView.addView(childView, insertIndex)
        viewExport.onAddToParent(parentView)
    }

    override fun setProp(tag: Int, propKey: String, propValue: Any) {
        assert(isMainThread()) {
            "must call on ui thread"
        }
        getRenderViewHandler(tag)?.viewExport?.also {
            var process = it.setProp(propKey, propValue)
            if (!process) {
                process = renderViewWeakRef?.get()?.kuiklyRenderExport?.setViewExternalProp(it,
                    propKey,
                    propValue) ?: false
            }
            if (it.reusable && process) {
                recordSetPropOperation(it.view(), propKey)
            }
        }
    }

    override fun setShadow(tag: Int, shadow: IKuiklyRenderShadowExport) {
        assert(isMainThread()) {
            "must call on ui thread"
        }
        getRenderViewHandler(tag)?.viewExport?.setShadow(shadow)
    }

    override fun setRenderViewFrame(tag: Int, frame: RectF) {
        assert(isMainThread()) {
            "must call on ui thread"
        }
        val kuiklyContext = renderViewWeakRef?.get()?.kuiklyRenderContext
        setProp(tag, KRCssConst.FRAME, Rect().apply {
            left = kuiklyContext.toPxI(frame.left)
            top = kuiklyContext.toPxI(frame.top)
            right = kuiklyContext.toPxI(frame.right)
            bottom = kuiklyContext.toPxI(frame.bottom)
        })
    }

    override fun calculateRenderViewSize(tag: Int, constraintSize: SizeF): SizeF {
        assert(!isMainThread()) {
            "must call on sub thread"
        }

        val shadowHandler = getShadowHandler(tag)
        assertShadowHandlerNotNull(shadowHandler)
        val kuiklyContext = renderViewWeakRef?.get()?.kuiklyRenderContext
        val pxSizeF = kuiklyContext.toPxSizeF(constraintSize)
        return if (shadowHandler != null) {
            kuiklyContext.toDpSizeF(shadowHandler.calculateRenderViewSize(pxSizeF))
        } else {
            constraintSize
        }
    }

    override fun callViewMethod(
        tag: Int,
        method: String,
        params: String?,
        callback: KuiklyRenderCallback?
    ) {
        assert(isMainThread()) {
            "must call on ui thread"
        }
        getRenderViewHandler(tag)?.viewExport?.call(method, params, callback)
    }

    override fun callModuleMethod(
        moduleName: String,
        method: String,
        params: Any?,
        callback: KuiklyRenderCallback?
    ): Any? = getModuleHandler(moduleName)?.call(method, params, callback)

    override fun callTDFModuleMethod(
        moduleName: String,
        method: String,
        params: String?,
        callId: String?,
        successCallback: KuiklyRenderCallback?,
        errorCallback: KuiklyRenderCallback?
    ): Any? {
        val result = renderViewWeakRef?.get()?.kuiklyRenderExport?.getTDFModule(moduleName)
            ?.invoke(method, TDFListUtils.fromJsonStr(params), KRTDFModulePromise(
                callId ?: KRTDFModulePromise.CALL_ID_NO_CALLBACK, successCallback, errorCallback
            ))
        return result?.toTDFModuleCallResult()
    }

    override fun createShadow(tag: Int, viewName: String) {
        assert(!isMainThread()) {
            "must call on sub thread"
        }

        val shadowMap = shadowRegistry ?: SparseArray<IKuiklyRenderShadowExport>().apply {
            shadowRegistry = this
        }
        assert(shadowRegistry?.get(tag) == null) {
            "shadow had created"
        }

        val kuiklyRenderExport = renderViewWeakRef?.get()?.kuiklyRenderExport ?: return
        shadowMap.put(tag, kuiklyRenderExport.createRenderShadow(viewName))
    }

    override fun removeShadow(tag: Int) {
        assert(!isMainThread()) {
            "must call on sub thread"
        }
        shadowRegistry?.remove(tag)
    }

    override fun setShadowProp(tag: Int, propKey: String, propValue: Any) {
        getShadowHandler(tag)?.setProp(propKey, propValue)
    }

    override fun callShadowMethod(tag: Int, methodName: String, params: String): Any? =
        getShadowHandler(tag)?.call(methodName, params)

    override fun shadow(tag: Int): IKuiklyRenderShadowExport? {
        val shadowHandler = getShadowHandler(tag)
        assertShadowHandlerNotNull(shadowHandler)
        return shadowHandler
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : IKuiklyRenderModuleExport> module(name: String): T? =
        getModuleHandler(name) as? T

    @Suppress("UNCHECKED_CAST")
    override fun <T : TDFBaseModule> getTDFModule(name: String): T? =
        renderViewWeakRef?.get()?.kuiklyRenderExport?.getTDFModule(name) as? T

    override fun getView(tag: Int): View? = getRenderViewHandler(tag)?.viewExport?.view()

    override fun onDestroy() {
        moduleRegistryWRLock.withReadLock {
            for (module in moduleRegistry.values) {
                module.onDestroy()
            }
        }

        for (i in 0 until renderViewRegistry.size()) {
            renderViewRegistry.valueAt(i).viewExport.onDestroy()
        }
    }
    private fun getRenderViewHandler(tag: Int): RenderViewHandler? {
        assert(isMainThread()) {
            "must call on ui thread"
        }
        return renderViewRegistry[tag]
    }

    private fun putRenderViewHandler(tag: Int, renderViewHandler: RenderViewHandler) {
        renderViewRegistry.put(tag, renderViewHandler)
    }

    private fun removeRenderViewHandler(tag: Int) {
        renderViewRegistry.remove(tag)
    }

    private fun createRenderViewHandler(tag: Int, viewName: String) {
        assert(isMainThread()) {
            "must call on ui thread"
        }
        val renderView: IKuiklyRenderView = renderViewWeakRef?.get() ?: return

        var renderViewHandler = getRenderViewHandler(tag)

        if (renderViewHandler == null) {
            renderViewHandler = popRenderViewHandlerFromReuseQueue(viewName)
        }
        if (renderViewHandler == null) {
            renderViewHandler = RenderViewHandler(
                viewName,
                renderView.kuiklyRenderExport.createRenderView(viewName,
                    renderView.kuiklyRenderContext.context)
            )
        }
        renderViewHandler.viewExport.kuiklyRenderContext = renderView.kuiklyRenderContext
        putRenderViewHandler(tag, renderViewHandler)
    }

    private fun popRenderViewHandlerFromReuseQueue(viewName: String): RenderViewHandler? {
        assert(isMainThread()) {
            "must call on ui thread"
        }

        val queue = renderViewReuseListMap[viewName]

        if (queue == null || queue.isEmpty()) {
            return null
        }
        return queue.removeAt(queue.lastIndex)
    }

    private fun pushRenderViewHandlerToReuseQueue(
        viewName: String,
        renderViewHandler: RenderViewHandler
    ) {
        assert(isMainThread()) {
            "must call on ui thread"
        }
        if (!renderViewHandler.viewExport.reusable) {
            return
        }
        var reuseQueue = renderViewReuseListMap[viewName]
        if (reuseQueue == null) {
            reuseQueue = mutableListOf()
            renderViewReuseListMap[viewName] = reuseQueue
        }
        if (reuseQueue.size >= MAX_REUSE_COUNT) {
            return
        }

        prepareForReuse(renderViewHandler.viewExport) // 重置View的样式，防止复用的时候样式错乱
        reuseQueue.add(renderViewHandler)
    }

    private fun innerRemoveRenderView(tag: Int) {
        val renderViewHandler = getRenderViewHandler(tag)
        assert(renderViewHandler != null)

        renderViewHandler?.viewExport?.also {
            pushRenderViewHandlerToReuseQueue(renderViewHandler.viewName, renderViewHandler)
            it.removeFromParent()
            it.onDestroy()
        }
        removeRenderViewHandler(tag)
    }

    private fun getShadowHandler(tag: Int): IKuiklyRenderShadowExport? {
        assert(!isMainThread()) {
            "must call on sub thread"
        }
        return shadowRegistry?.get(tag)
    }

    private fun getModuleHandler(moduleName: String): IKuiklyRenderModuleExport? {
        var moduleHandler = moduleHandlerWithName(moduleName)
        if (moduleHandler == null) {
            moduleRegistryWRLock.withWriteLock {
                moduleHandler = renderViewWeakRef?.get()?.kuiklyRenderExport?.createModule(moduleName)?.apply {
                    kuiklyRenderContext = renderViewWeakRef?.get()?.kuiklyRenderContext
                }
                moduleRegistry[moduleName] = moduleHandler
            }
            moduleHandler = moduleHandlerWithName(moduleName) // 检验一次
        }
        return moduleHandler
    }

    private fun moduleHandlerWithName(moduleName: String): IKuiklyRenderModuleExport? {
        var handler: IKuiklyRenderModuleExport? = null
        moduleRegistryWRLock.withReadLock {
            handler = moduleRegistry[moduleName]
        }
        return handler
    }

    private fun recordSetPropOperation(view: View, propKey: String) {
        val kuiklyRenderViewContext = renderViewWeakRef?.get()?.kuiklyRenderContext ?: return

        val setPropOperationSet =
            kuiklyRenderViewContext.getViewData<MutableSet<String>>(view, HR_SET_PROP_OPERATION)
                ?: mutableSetOf<String>().apply {
                    kuiklyRenderViewContext.putViewData(view, HR_SET_PROP_OPERATION, this)
                }
        setPropOperationSet.add(propKey)
    }

    private fun prepareForReuse(viewExport: IKuiklyRenderViewExport) {
        renderViewWeakRef?.get()?.kuiklyRenderContext?.removeViewData<MutableSet<String>>(viewExport.view(),
            HR_SET_PROP_OPERATION)?.also {
            for (propKey in it) {
                if (!viewExport.resetProp(propKey)) {
                    renderViewWeakRef?.get()?.kuiklyRenderExport?.resetViewExternalProp(viewExport,
                        propKey)
                }
            }
        }
        viewExport.resetShadow()
    }

    private fun assertShadowHandlerNotNull(shadowHandler: IKuiklyRenderShadowExport?) {
        assert(shadowHandler != null) {
            "shadow must not null"
        }
    }

    companion object {
        const val ROOT_VIEW_TAG = -1
        internal const val HR_SET_PROP_OPERATION = "hr_set_prop_operation"
        private const val MAX_REUSE_COUNT = 50
        private const val TDF_METHOD_PARAMS_KEY = "result"
    }
}

/**
 * [IKuiklyRenderViewExport]包装类，用于关联到对于的viewName
 */
data class RenderViewHandler(
    val viewName: String,
    val viewExport: IKuiklyRenderViewExport
) {
    init {
        viewExport.view().apply {
            if (layoutParams == null) {
                layoutParams = ViewGroup.LayoutParams(0, 0)
            } else {
                layoutParams.width = 0
                layoutParams.height = 0
            }
        }
        viewExport.viewGroup?.also {
            it.addOnLayoutChangeListener(object : OnLayoutChangeListener {
                override fun onLayoutChange(
                    v: View?,
                    left: Int,
                    top: Int,
                    right: Int,
                    bottom: Int,
                    oldLeft: Int,
                    oldTop: Int,
                    oldRight: Int,
                    oldBottom: Int
                ) {
                    it.clipChildren = false // 全部View不裁剪
                    it.removeOnLayoutChangeListener(this)
                }

            })
        }
    }
}

/**
 * 扩展[ReentrantReadWriteLock], 保证writeLock在task执行完后会unlock
 */
private fun ReentrantReadWriteLock.withWriteLock(task: () -> Unit) {
    try {
        writeLock().lock()
        task()
    } finally {
        writeLock().unlock()
    }
}

/**
 * 扩展[ReentrantReadWriteLock], 保证readLock在task执行完后会unlock
 */
private fun ReentrantReadWriteLock.withReadLock(task: () -> Unit) {
    try {
        readLock().lock()
        task()
    } finally {
        readLock().unlock()
    }
}
