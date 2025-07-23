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

package com.tencent.kuikly.core.render.android.core

import android.graphics.RectF
import android.util.SizeF
import android.util.SparseArray
import android.view.View
import com.tencent.kuikly.core.render.android.IKuiklyRenderView
import com.tencent.kuikly.core.render.android.IKuiklyRenderViewTreeUpdateListener
import com.tencent.kuikly.core.render.android.const.KRExtConst
import com.tencent.kuikly.core.render.android.context.IKotlinBridgeStatusListener
import com.tencent.kuikly.core.render.android.context.KuiklyRenderContextMethod
import com.tencent.kuikly.core.render.android.context.KuiklyRenderNativeMethodCallback
import com.tencent.kuikly.core.render.android.context.IKuiklyRenderContextHandler
import com.tencent.kuikly.core.render.android.context.KuiklyRenderNativeMethod
import com.tencent.kuikly.core.render.android.context.KuiklyRenderJvmContextHandler
import com.tencent.kuikly.core.render.android.css.ktx.fifthArg
import com.tencent.kuikly.core.render.android.css.ktx.fourthArg
import com.tencent.kuikly.core.render.android.css.ktx.secondArg
import com.tencent.kuikly.core.render.android.css.ktx.thirdArg
import com.tencent.kuikly.core.render.android.css.ktx.sixthArg
import com.tencent.kuikly.core.render.android.css.ktx.isMainThread
import com.tencent.kuikly.core.render.android.exception.IKuiklyRenderExceptionListener
import com.tencent.kuikly.core.render.android.exception.ErrorReason
import com.tencent.kuikly.core.render.android.exception.KRKotlinBizException
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import com.tencent.kuikly.core.render.android.export.IKuiklyRenderModuleExport
import com.tencent.kuikly.core.render.android.layer.KuiklyRenderLayerHandler
import com.tencent.kuikly.core.render.android.layer.IKuiklyRenderLayerHandler
import com.tencent.kuikly.core.render.android.scheduler.KuiklyRenderCoreContextScheduler
import com.tencent.kuikly.core.render.android.scheduler.KuiklyRenderCoreTask
import com.tencent.kuikly.core.render.android.scheduler.KuiklyRenderCoreUIScheduler
import com.tencent.tdf.module.TDFBaseModule
import org.json.JSONObject
import java.util.Locale

class KuiklyRenderCore(
    private var contextHandler: IKuiklyRenderContextHandler? = null
) : IKuiklyRenderCore {

    /**
     * 代表KTV页面唯一的id
     */
    private val instanceId: String = instanceIdProducer++.toString()

    /**
     * KTV页面渲染层协议处理器
     */
    private var renderLayerHandler: IKuiklyRenderLayerHandler? = null

    /**
     * KTV UI线程调度器
     */
    private var uiScheduler: KuiklyRenderCoreUIScheduler? = null

    /**
     * KTV调用Native方法的回调map
     */
    private val nativeMethodRegistry = SparseArray<KuiklyRenderNativeMethodCallback>()

    /**
     * 异常监听
     */
    private var exceptionListener: IKuiklyRenderExceptionListener? = null

    override fun init(
        renderView: IKuiklyRenderView,
        contextCode: String,
        url: String,
        params: Map<String, Any>,
        assetsPath: String?,
        contextInitCallback: IKuiklyRenderContextInitCallback
    ) {
        uiScheduler = KuiklyRenderCoreUIScheduler {
            // 同步主线程任务前，需要告诉KTV Core 去 layoutIfNeed, 避免viewFrame设置时机和创建view时机不同步
            contextHandler?.call(
                KuiklyRenderContextMethod.KuiklyRenderContextMethodLayoutView,
                listOf(instanceId)
            )
        }.apply {
            setRenderExceptionListener(exceptionListener)
        }
        renderLayerHandler = KuiklyRenderLayerHandler().apply {
            init(renderView)
        }
        initNativeMethodRegisters()
        performOnContextQueue {
            initContextHandler(contextCode, url, params, contextInitCallback)
        }
    }

    override fun sendEvent(event: String, data: Map<String, Any>, shouldSync: Boolean) {
        performOnContextQueue(sync = shouldSync) {
            contextHandler?.call(
                KuiklyRenderContextMethod.KuiklyRenderContextMethodUpdateInstance,
                listOf(
                    instanceId,
                    event,
                    data
                )
            )
            if (shouldSync) {
                uiScheduler?.performSyncMainQueueTasksBlockIfNeed(true)
            }
        }
        if (shouldSync) {
            uiScheduler?.performMainThreadTaskWaitToSyncBlockIfNeed()
        }
    }

    override fun <T : IKuiklyRenderModuleExport> module(name: String): T? =
        renderLayerHandler?.module(name)

    override fun <T : TDFBaseModule> getTDFModule(name: String): T? =
        renderLayerHandler?.getTDFModule(name)

    override fun getView(tag: Int): View? = renderLayerHandler?.getView(tag)

    override fun destroy() {
        renderLayerHandler?.onDestroy()
        performOnContextQueue {
            contextHandler?.call(
                KuiklyRenderContextMethod.KuiklyRenderContextMethodDestroyInstance,
                listOf(
                    instanceId
                )
            )
            contextHandler?.destroy()
        }
        uiScheduler?.destroy()
    }

    override fun syncFlushAllRenderTasks() {
        performOnContextQueue(sync = true) {
            uiScheduler?.performSyncMainQueueTasksBlockIfNeed(true)
            uiScheduler?.performOnMainQueueWithTask(sync = false) {
                uiScheduler?.performMainThreadTaskWaitToSyncBlockIfNeed()
            }
        }
        uiScheduler?.performMainThreadTaskWaitToSyncBlockIfNeed()
    }

    override fun performWhenViewDidLoad(task: KuiklyRenderCoreTask) {
        uiScheduler?.performWhenViewDidLoad(task)
    }

    private fun initNativeMethodRegisters() {
        nativeMethodRegistry.put(
            KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCreateRenderView.value, object : KuiklyRenderNativeMethodCallback {
                override fun invoke(methodId: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
                    return createRenderView(methodId, args)
                }
            }
        )
        nativeMethodRegistry.put(
            KuiklyRenderNativeMethod.KuiklyRenderNativeMethodRemoveRenderView.value, object : KuiklyRenderNativeMethodCallback {
                override fun invoke(methodId: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
                    return removeRenderView(methodId, args)
                }
            }
        )
        nativeMethodRegistry.put(
            KuiklyRenderNativeMethod.KuiklyRenderNativeMethodInsertSubRenderView.value, object : KuiklyRenderNativeMethodCallback {
                override fun invoke(methodId: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
                    return insertSubRenderView(methodId, args)
                }
            }
        )
        nativeMethodRegistry.put(
            KuiklyRenderNativeMethod.KuiklyRenderNativeMethodSetViewProp.value, object : KuiklyRenderNativeMethodCallback {
                override fun invoke(methodId: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
                    return setViewProp(methodId, args)
                }
            }
        )
        nativeMethodRegistry.put(
            KuiklyRenderNativeMethod.KuiklyRenderNativeMethodSetRenderViewFrame.value, object : KuiklyRenderNativeMethodCallback {
                override fun invoke(methodId: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
                    return setRenderViewFrame(methodId, args)
                }
            }
        )
        nativeMethodRegistry.put(
            KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCalculateRenderViewSize.value, object : KuiklyRenderNativeMethodCallback {
                override fun invoke(methodId: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
                    return calculateRenderViewSize(methodId, args)
                }
            }
        )
        nativeMethodRegistry.put(
            KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCallViewMethod.value, object : KuiklyRenderNativeMethodCallback {
                override fun invoke(methodId: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
                    return callViewMethod(methodId, args)
                }
            }
        )
        nativeMethodRegistry.put(
            KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCallModuleMethod.value, object : KuiklyRenderNativeMethodCallback {
                override fun invoke(methodId: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
                    return callModuleMethod(methodId, args)
                }
            }
        )
        nativeMethodRegistry.put(
            KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCallTDFNativeMethod.value, object : KuiklyRenderNativeMethodCallback {
                override fun invoke(methodId: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
                    return callTDFModuleMethod(methodId, args)
                }
            }
        )

        nativeMethodRegistry.put(
            KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCreateShadow.value, object : KuiklyRenderNativeMethodCallback {
                override fun invoke(methodId: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
                    return createShadow(methodId, args)
                }
            }
        )
        nativeMethodRegistry.put(
            KuiklyRenderNativeMethod.KuiklyRenderNativeMethodRemoveShadow.value, object : KuiklyRenderNativeMethodCallback {
                override fun invoke(methodId: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
                    return removeShadow(methodId, args)
                }
            }
        )
        nativeMethodRegistry.put(
            KuiklyRenderNativeMethod.KuiklyRenderNativeMethodSetShadowProp.value, object : KuiklyRenderNativeMethodCallback {
                override fun invoke(methodId: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
                    return setShadowProp(methodId, args)
                }
            }
        )
        nativeMethodRegistry.put(
            KuiklyRenderNativeMethod.KuiklyRenderNativeMethodSetShadowForView.value, object : KuiklyRenderNativeMethodCallback {
                override fun invoke(methodId: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
                    return setShadow(methodId, args)
                }
            }
        )
        nativeMethodRegistry.put(
            KuiklyRenderNativeMethod.KuiklyRenderNativeMethodSetTimeout.value, object : KuiklyRenderNativeMethodCallback {
                override fun invoke(methodId: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
                    return setTimeout(methodId, args)
                }
            }
        )
        nativeMethodRegistry.put(
            KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCallShadowMethod.value, object : KuiklyRenderNativeMethodCallback {
                override fun invoke(methodId: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
                    return callShadowMethod(methodId, args)
                }
            }
        )
        nativeMethodRegistry.put(KuiklyRenderNativeMethod.KuiklyRenderNativeMethodFireFatalException.value,
            object : KuiklyRenderNativeMethodCallback {
                override fun invoke(methodId: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
                    return fireFatalException(methodId, args)
                }
            }
        )
        nativeMethodRegistry.put(
            KuiklyRenderNativeMethod.KuiklyRenderNativeMethodSyncFlushUI.value, object : KuiklyRenderNativeMethodCallback {
                override fun invoke(methodId: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
                    return syncFlushUI(methodId, args)
                }
            }
        )
    }

    private fun initContextHandler(
        contextCode: String,
        url: String,
        params: Map<String, Any>,
        initCallback: IKuiklyRenderContextInitCallback
    ) {
        if (contextHandler == null) {
            contextHandler = KuiklyRenderJvmContextHandler()
        }
        contextHandler?.apply {
            setRenderExceptionListener(exceptionListener)
            registerCallNative { method, args ->
                performNativeMethodWithMethod(method, args)
            }

            initCallback.onStart()
            init(contextCode)
            initCallback.onFinish()

            initCallback.onCreateInstanceStart()
            call(
                KuiklyRenderContextMethod.KuiklyRenderContextMethodCreateInstance, listOf(
                    instanceId,
                    url,
                    params
                )
            )
            initCallback.onCreateInstanceFinish()

        }
    }

    private fun performNativeMethodWithMethod(
        method: KuiklyRenderNativeMethod,
        args: List<Any?>
    ): Any? {
        val cb = nativeMethodRegistry[method.value]
        cb?.also {
            assert(!isMainThread())
            if (isSyncMethodCall(method, args)) { // 同步方法的话，直接调用，不调度到UI线程
                return it(method, args)
            } else {
                uiScheduler?.scheduleTask(isUpdateViewTree = isUpdateViewTreeMethodCall(method)) {
                    it(method, args)
                }
            }
        }
        return null
    }

    private fun createRenderView(method: KuiklyRenderNativeMethod, args: List<Any?>): Any? =
        renderLayerHandler?.createRenderView(args.secondArg(), args.thirdArg())

    private fun removeRenderView(method: KuiklyRenderNativeMethod, args: List<Any?>): Any? =
        renderLayerHandler?.removeRenderView(args.secondArg())

    private fun insertSubRenderView(method: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
        return renderLayerHandler?.insertSubRenderView(
            args.secondArg(),
            args.thirdArg(),
            args.fourthArg()
        )
    }

    private fun setViewProp(method: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
        var propValue = args.fourthArg<Any>()
        if (isEvent(args)) {
            val tag = args.secondArg<Int>()
            var syncCall = args.sixthArg<Int>() == 1
            propValue = object : KuiklyRenderCallback {
                override fun invoke(result: Any?) {
                    var shouldSync = syncCall
                    // 正在主线程执行任务产生的同步事件->异步
                    if (shouldSync && uiScheduler?.isPerformingMainQueueTask == true) {
                        shouldSync = false
                    }
                    performOnContextQueue(sync = shouldSync) {
                        contextHandler?.call(
                            KuiklyRenderContextMethod.KuiklyRenderContextMethodFireViewEvent,
                            listOf(instanceId, tag, args.thirdArg(), result)
                        )
                        if (shouldSync) {
                            uiScheduler?.performSyncMainQueueTasksBlockIfNeed(true)
                        }
                    }
                    if (shouldSync) {
                        uiScheduler?.performMainThreadTaskWaitToSyncBlockIfNeed()
                    }
                }
            }
        }
        return renderLayerHandler?.setProp(args.secondArg(), args.thirdArg(), propValue)
    }

    private fun isEvent(args: List<Any?>): Boolean = args.fifthArg<Int?>() == 1

    private fun setRenderViewFrame(method: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
        val frame = RectF(args.thirdArg(), args.fourthArg(), args.fifthArg(), args.sixthArg())
        return renderLayerHandler?.setRenderViewFrame(args.secondArg(), frame)
    }

    private fun calculateRenderViewSize(method: KuiklyRenderNativeMethod, args: List<Any?>): Any {
        val size = renderLayerHandler?.calculateRenderViewSize(
            args.secondArg(), SizeF(
                args.thirdArg(),
                args.fourthArg()
            )
        )
        return String.format(Locale.ENGLISH, "%.2f|%.2f", size?.width ?: 0f, size?.height ?: 0f)
    }

    private fun callViewMethod(method: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
        val callbackId = args.fifthArg<String?>()
        val callback: ((Any?) -> Unit)? = if (callbackId?.isNotEmpty() == true) {
            { result: Any? ->
                performOnContextQueue {
                    contextHandler?.call(
                        KuiklyRenderContextMethod.KuiklyRenderContextMethodFireCallback,
                        listOf(
                            instanceId,
                            callbackId,
                            result
                        )
                    )
                }
            }
        } else {
            null
        }
        return renderLayerHandler?.callViewMethod(
            args.secondArg(),
            args.thirdArg(),
            args.fourthArg(),
            callback
        )
    }

    private fun callModuleMethod(method: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
        val callbackId = args.fifthArg<String?>()
        val callback: ((Any?) -> Unit)? = if (callbackId?.isNotEmpty() == true) {
            { result: Any? ->
                performOnContextQueue {
                    contextHandler?.call(
                        KuiklyRenderContextMethod.KuiklyRenderContextMethodFireCallback,
                        listOf(
                            instanceId,
                            callbackId,
                            result
                        )
                    )
                }
            }
        } else {
            null
        }
        return renderLayerHandler?.callModuleMethod(
            args.secondArg(),
            args.thirdArg(),
            args.fourthArg(),
            callback
        )
    }

    private fun callTDFModuleMethod(method: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
        var successCallback: KuiklyRenderCallback? = null
        var errorCallback: KuiklyRenderCallback? = null
        var callId: String? = null
        args.fifthArg<String?>()?.let {
            val cbsJsonObj = JSONObject(it)
            val successCallbackId = cbsJsonObj.optString("succ")
            callId = successCallbackId
            val errorCallbackId = cbsJsonObj.optString("error")
            if (successCallbackId.isNotEmpty()) {
                successCallback = { result: Any? ->
                        performOnContextQueue {
                            contextHandler?.call(
                                KuiklyRenderContextMethod.KuiklyRenderContextMethodFireCallback,
                                listOf(
                                    instanceId,
                                    successCallbackId,
                                    result
                                )
                            )
                        }
                    }
            }
            if (errorCallbackId.isNotEmpty()) {
                errorCallback = { result: Any? ->
                        performOnContextQueue {
                            contextHandler?.call(
                                KuiklyRenderContextMethod.KuiklyRenderContextMethodFireCallback,
                                listOf(
                                    instanceId,
                                    errorCallbackId,
                                    result
                                )
                            )
                        }
                    }
            }

        }
        return renderLayerHandler?.callTDFModuleMethod(
            args.secondArg(),
            args.thirdArg(),
            args.fourthArg(),
            callId,
            successCallback,
            errorCallback
        )
    }

    private fun createShadow(method: KuiklyRenderNativeMethod, args: List<Any?>): Any? =
        renderLayerHandler?.createShadow(args.secondArg(), args.thirdArg())

    private fun removeShadow(method: KuiklyRenderNativeMethod, args: List<Any?>): Any? =
        renderLayerHandler?.removeShadow(args.secondArg())

    private fun setShadowProp(method: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
        return renderLayerHandler?.setShadowProp(
            args.secondArg(),
            args.thirdArg(),
            args.fourthArg()
        )
    }

    private fun callShadowMethod(method: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
        return renderLayerHandler?.callShadowMethod(
            args.secondArg(),
            args.thirdArg(),
            args.fourthArg()
        )
    }

    private fun setShadow(method: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
        val shadow = renderLayerHandler?.shadow(args.secondArg()) ?: return null
        uiScheduler?.scheduleTask {
            renderLayerHandler?.setShadow(args.secondArg(), shadow)
        }
        return null
    }

    private fun setTimeout(method: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
        performOnContextQueue(args.secondArg()) {
            contextHandler?.call(
                KuiklyRenderContextMethod.KuiklyRenderContextMethodFireCallback,
                listOf(
                    instanceId,
                    args.thirdArg()
                )
            )
        }
        return null
    }

    private fun fireFatalException(method: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
        val exception = KRKotlinBizException((args.secondArg() as? String) ?: "")
        exceptionListener?.onRenderException(exception, ErrorReason.CALL_KOTLIN)
        return null
    }

    private fun syncFlushUI(method: KuiklyRenderNativeMethod, args: List<Any?>): Any? {
        uiScheduler?.performSyncMainQueueTasksBlockIfNeed(false)
        return null
    }

    private fun performOnContextQueue(delayMs: Float = 0f, sync: Boolean = false, task: () -> Unit) {
        if (sync) {
            KuiklyRenderCoreContextScheduler.runTaskSyncUnsafely(delayMs.toLong(), 1000, task)
        } else {
            KuiklyRenderCoreContextScheduler.scheduleTask(delayMs.toLong(), task)
        }
    }

    private fun isSyncMethodCall(method: KuiklyRenderNativeMethod, args: List<Any?>): Boolean {
        if (method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCallModuleMethod ||
            method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCallTDFNativeMethod
        ) {
            val fifthArg = if (args.size >= IKuiklyRenderContextHandler.CALL_ARGS_COUNT) {
                args[KRExtConst.SIXTH_ARG_INDEX] as? Int ?: KRExtConst.FIRST_ARG_INDEX
            } else {
                KRExtConst.FIRST_ARG_INDEX
            }
            return fifthArg == SYNC_CALL_TYPE
        }

        return method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCalculateRenderViewSize ||
                method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCreateShadow ||
                method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodRemoveShadow ||
                method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodSetShadowForView ||
                method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodSetShadowProp ||
                method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodSetTimeout ||
                method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCallShadowMethod ||
                method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodSyncFlushUI
    }

    /**
     * 判断是否为更新 ViewTree 的调用
     */
    private fun isUpdateViewTreeMethodCall(method: KuiklyRenderNativeMethod): Boolean {
        return method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodCreateRenderView ||
                method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodRemoveRenderView ||
                method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodInsertSubRenderView ||
                method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodSetViewProp ||
                method == KuiklyRenderNativeMethod.KuiklyRenderNativeMethodSetRenderViewFrame
    }

    /**
     * 设置更新 View Tree 监听
     */
    override fun setViewTreeUpdateListener(listener: IKuiklyRenderViewTreeUpdateListener) {
        uiScheduler?.setViewTreeUpdateListener(listener)
    }

    /**
     * 设置 Kotlin Bridge 状态监听
     */
    override fun setKotlinBridgeStatusListener(listener: IKotlinBridgeStatusListener) {
        contextHandler?.setBridgeStatusListener(listener)
    }

    /**
     * 设置渲染异常监听
     */
    override fun setRenderExceptionListener(listener: IKuiklyRenderExceptionListener) {
        exceptionListener = listener
    }

    companion object {
        private var instanceIdProducer = 0L
        private const val SYNC_CALL_TYPE = 1
    }

}
