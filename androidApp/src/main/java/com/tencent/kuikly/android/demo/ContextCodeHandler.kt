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

package com.tencent.kuikly.android.demo

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.tencent.kuikly.android.demo.module.KRBridgeModule
import com.tencent.kuikly.android.demo.module.KRShareModule
import com.tencent.kuikly.android.demo.module.tdf.KRTDFTestModule
import com.tencent.kuikly.core.render.android.IKuiklyRenderExport
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.context.KuiklyRenderCoreExecuteModeBase
import com.tencent.kuikly.core.render.android.exception.ErrorReason
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegatorDelegate
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegator
import com.tencent.kuikly.core.render.android.performace.KRMonitorType
import com.tencent.kuikly.core.render.android.performace.KRPerformanceData
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData

open class ContextCodeHandler(
    private val context: Context,
    val pageName: String
) {
    private var beginTime : Long = 0
    lateinit var kuiklyRenderViewDelegator: KuiklyRenderViewBaseDelegator

    open fun initContextHandler() : KuiklyRenderViewBaseDelegator{
        // 2.1 实现KuiklyRenderViewBaseDelegatorDelegate接口
        val delegate = object : KuiklyRenderViewBaseDelegatorDelegate {
            //  2.1.1 处理Kuikly性能监控数据回调
            override fun onGetPerformanceData(data: KRPerformanceData) {
                this@ContextCodeHandler.onGetPerformanceData(data)
            }
            //  2.1.2 处理Kuikly 启动性能数据回调
            override fun onGetLaunchData(data: KRLaunchData) {
                this@ContextCodeHandler.onGetLaunchData(data)
            }
            // 2.1.3 注册自定义module
            override fun registerExternalModule(kuiklyRenderExport: IKuiklyRenderExport) {
                super.registerExternalModule(kuiklyRenderExport)
                this@ContextCodeHandler.registerExternalModule(kuiklyRenderExport)
            }
            // 2.1.4 让宿主工程暴露自己的View
            override fun registerExternalRenderView(kuiklyRenderExport: IKuiklyRenderExport) {
                super.registerExternalRenderView(kuiklyRenderExport)
                this@ContextCodeHandler.registerExternalRenderView(kuiklyRenderExport)
            }
            // 2.1.5 注册 TDF 通用 Module
            override fun registerTDFModule(kuiklyRenderExport: IKuiklyRenderExport) {
                super.registerTDFModule(kuiklyRenderExport)
                this@ContextCodeHandler.registerTDFModule(kuiklyRenderExport)
            }
            // 2.1.6 给Kuikly框架设置页面打开模式，即KuiklyRenderCore的执行模式, 默认JVM模式
            override fun coreExecuteModeX(): KuiklyRenderCoreExecuteModeBase {
                return KuiklyRenderCoreExecuteModeBase.JVM
            }
            // 2.1.7 给Kuikly框架设置性能监控选项，默认只开启动监控
            override fun performanceMonitorTypes(): List<KRMonitorType> {
                return this@ContextCodeHandler.performanceMonitorTypes()
            }
            // 2.1.8 [KuiklyRenderView]创建回调
            override fun onKuiklyRenderViewCreated() {
                super.onKuiklyRenderViewCreated()
                this@ContextCodeHandler.onKuiklyRenderViewCreated()
            }
            // 2.1.9 [KuiklyRenderView]的子View已经创建回调
            override fun onKuiklyRenderContentViewCreated() {
                super.onKuiklyRenderContentViewCreated()
                this@ContextCodeHandler.onKuiklyRenderContentViewCreated()
            }
            // 2.1.10 异常通知回调
            override fun onUnhandledException(
                throwable: Throwable,
                errorReason: ErrorReason,
                executeMode: KuiklyRenderCoreExecuteModeBase
            ) {
                this@ContextCodeHandler.onUnhandledException(throwable, errorReason, executeMode.mode)
            }
            // 2.1.11 页面加载回调
            override fun onPageLoadComplete(
                isSucceed: Boolean,
                errorReason: ErrorReason?,
                executeMode: KuiklyRenderCoreExecuteModeBase
            ) {
                this@ContextCodeHandler.onPageLoadComplete(isSucceed, errorReason, executeMode.mode)
            }

        }
        // 2.2 创建KuiklyRenderViewBaseDelegator实例
        kuiklyRenderViewDelegator = KuiklyRenderViewBaseDelegator(delegate)
        return kuiklyRenderViewDelegator
    }

    open fun openPage(hrContainerView: ViewGroup, pageName: String, pageData: Map<String, Any>) {
        //  4.1 通过框架Delegator，打开Kuikly页面
        kuiklyRenderViewDelegator.onAttach(
            hrContainerView,
            "",
            pageName,
            pageData
        )
    }

    fun onGetPerformanceData(data: KRPerformanceData) {
        KuiklyRenderLog.d(TAG, "------------------------------------------ [PerformanceData] ------------------------------------------")
        KuiklyRenderLog.d(TAG, "[PerformanceData] $data")
        KuiklyRenderLog.d(TAG, "[PerformanceData] [Launch] ${data.launchData}")
        KuiklyRenderLog.d(TAG, "[PerformanceData] [Frame] ${data.frameData}, fps: ${data.frameData?.getFps()}, kuiklyFps: ${data.frameData?.getKuiklyFps()}")
        KuiklyRenderLog.d(TAG, "[PerformanceData] [Memory] maxPss: ${data.memoryData?.getMaxPss()}, deltaPss: ${data.memoryData?.getMaxPssIncrement()}, firstDeltaPss: ${data.memoryData?.getFirstPssIncrement()}")
        KuiklyRenderLog.d(TAG, "[PerformanceData] [Memory] maxJavaHeap: ${data.memoryData?.getMaxJavaHeap()}, deltaJavaHeap: ${data.memoryData?.getMaxJavaHeapIncrement()}, fristDeltaJavaHeap: ${data.memoryData?.getFirstDeltaJavaHeap()}")
        KuiklyRenderLog.d(TAG, "------------------------------------------ [PerformanceData] ------------------------------------------")

        val performanceData = """
            [PerformanceData] [Launch] ${data.launchData?.firstFramePaintCost}
            [PerformanceData] [Frame] fps: ${data.frameData?.getFps()}, kuiklyFps: ${data.frameData?.getKuiklyFps()}
            [PerformanceData] [Memory] firstPss: ${data.memoryData?.getFirstPssIncrement()}
            [PerformanceData] [Memory] maxPss: ${data.memoryData?.getMaxPss()}
        """.trimIndent()
//        getSharedPreferences("performance", MODE_PRIVATE).edit().putString("PerformanceData", performanceData).commit()
    }

    fun onGetLaunchData(data: KRLaunchData) {
        KuiklyRenderLog.d(TAG, "[onGetLaunchData] $data")
    }

    fun registerExternalModule(kuiklyRenderExport: IKuiklyRenderExport) {
        with(kuiklyRenderExport) {
            moduleExport(KRBridgeModule.MODULE_NAME) {
                KRBridgeModule()
            }
            moduleExport(KRShareModule.MODULE_NAME) {
                KRShareModule()
            }
        }
    }

    fun registerExternalRenderView(kuiklyRenderExport: IKuiklyRenderExport) {
        with(kuiklyRenderExport) {
            renderViewExport(KuiklyPageView.VIEW_NAME, {
                KuiklyPageView(it)
            })
        }
    }

    fun registerTDFModule(kuiklyRenderExport: IKuiklyRenderExport) {
        with(kuiklyRenderExport) {
            registerTDFModule(KRTDFTestModule::class.java) {
                KRTDFTestModule(it)
            }
        }
    }

    fun performanceMonitorTypes(): List<KRMonitorType> {
        return listOf(KRMonitorType.LAUNCH, KRMonitorType.FRAME, KRMonitorType.MEMORY)
    }

    fun turboDisplayKey(): String? {
        return pageName
    }

    fun onKuiklyRenderViewCreated() {
        beginTime = System.currentTimeMillis()
    }

    fun onKuiklyRenderContentViewCreated() {
        val endTime = System.currentTimeMillis()
        val elapsedTime = endTime - beginTime

        KuiklyRenderLog.d("", "[KRLaunchMeta] onKuiklyRenderContentViewCreated ${System.currentTimeMillis()}")

        Log.d(TAG, "pageCostTime:${elapsedTime}ms")
    }

    fun onUnhandledException(
        throwable: Throwable,
        errorReason: ErrorReason,
        executeMode: Int
    ) {
        if (context is Activity) {
            val exceptionContent = throwable.stackTraceToString()
            AlertDialog.Builder(context)
                .setTitle("错误")
                .setMessage(exceptionContent)
                .setCancelable(true)
                .setNegativeButton("关闭") { dialog, _ ->
                    dialog.dismiss()
                    context.finish()
                }
                .setPositiveButton("复制") { dialog, _ ->
                    // Copy text to clipboard
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Dialog Text", exceptionContent)
                    clipboard.setPrimaryClip(clip)
                    // Show confirmation toast
                    Toast.makeText(context, "文本已复制", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    context.finish()
                }
                .show()
            return
        }
        KuiklyRenderLog.e(TAG, "onUnhandledException, errorReason: $errorReason, executeMode: ${executeMode}, ${throwable.stackTraceToString()}")
    }

    fun onPageLoadComplete(
        isSucceed: Boolean,
        errorReason: ErrorReason?,
        executeMode: Int
    ) {
        KuiklyRenderLog.e(TAG, "onPageLoadComplete isSucceed: $isSucceed, errorReason: $errorReason, executeMode: ${executeMode}")
    }

    companion object {
        private const val TAG: String = "ContextCodeHandler"
    }
}