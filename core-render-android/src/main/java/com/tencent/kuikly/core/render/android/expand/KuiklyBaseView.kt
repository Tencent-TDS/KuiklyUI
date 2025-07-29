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

package com.tencent.kuikly.core.render.android.expand

import android.content.Context
import android.util.Size
import android.widget.FrameLayout
import com.tencent.kuikly.core.render.android.IKuiklyRenderExport
import com.tencent.kuikly.core.render.android.IKuiklyRenderViewLifecycleCallback
import com.tencent.kuikly.core.render.android.context.KuiklyRenderCoreExecuteModeBase
import com.tencent.kuikly.core.render.android.context.KuiklyRenderJvmContextHandler
import com.tencent.kuikly.core.render.android.exception.ErrorReason
import com.tencent.kuikly.core.render.android.performace.KRMonitorType
import com.tencent.kuikly.core.render.android.performace.KRPerformanceData
import com.tencent.kuikly.core.render.android.performace.launch.KRLaunchData

/**
 * Created by kam on 2023/9/5.
 * Kuikly View粒度接入，页面粒度接入使用[KuiklyRenderViewDelegator]
 */
open class KuiklyBaseView(context: Context, private val delegate: KuiklyRenderViewBaseDelegatorDelegate? = null) :
    FrameLayout(context), IKuiklyView, KuiklyRenderViewBaseDelegatorDelegate {

    private val kuiklyRenderViewDelegator = KuiklyRenderViewBaseDelegator(this)

    override fun onAttach(
        contextCode: String,
        pageName: String,
        pageData: Map<String, Any>,
        size: Size?,
        assetsPath: String?
    ) {
        kuiklyRenderViewDelegator.onAttach(this, contextCode, pageName, pageData, size, assetsPath)
    }

    override fun onPause() {
        kuiklyRenderViewDelegator.onPause()
    }

    override fun onResume() {
        kuiklyRenderViewDelegator.onResume()
    }

    override fun onDetach() {
        kuiklyRenderViewDelegator.onDetach()
    }

    override fun sendEvent(event: String, data: Map<String, Any>) {
        kuiklyRenderViewDelegator.sendEvent(event, data)
    }

    override fun isPageExist(pageName: String): Boolean {
        return KuiklyRenderJvmContextHandler.isPageExist(pageName)
    }

    override fun addKuiklyRenderViewLifeCycleCallback(callback: IKuiklyRenderViewLifecycleCallback) {
        kuiklyRenderViewDelegator.addKuiklyRenderViewLifeCycleCallback(callback)
    }

    override fun removeKuiklyRenderViewLifeCycleCallback(callback: IKuiklyRenderViewLifecycleCallback) {
        kuiklyRenderViewDelegator.removeKuiklyRenderViewLifeCycleCallback(callback)
    }

    override fun registerExternalRenderView(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerExternalRenderView(kuiklyRenderExport)
        delegate?.registerExternalRenderView(kuiklyRenderExport)
    }

    override fun registerExternalModule(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerExternalModule(kuiklyRenderExport)
        delegate?.registerExternalModule(kuiklyRenderExport)
    }

    override fun registerTDFModule(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerTDFModule(kuiklyRenderExport)
        delegate?.registerTDFModule(kuiklyRenderExport)
    }

    override fun registerViewExternalPropHandler(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerViewExternalPropHandler(kuiklyRenderExport)
        delegate?.registerViewExternalPropHandler(kuiklyRenderExport)
    }

    override fun coreExecuteModeX(): KuiklyRenderCoreExecuteModeBase {
        return delegate?.coreExecuteModeX() ?: super.coreExecuteModeX()
    }

    override fun performanceMonitorTypes(): List<KRMonitorType> {
        return delegate?.performanceMonitorTypes() ?: super.performanceMonitorTypes()
    }

    override fun onKuiklyRenderViewCreated() {
        super.onKuiklyRenderViewCreated()
        delegate?.onKuiklyRenderViewCreated()
    }

    override fun onKuiklyRenderContentViewCreated() {
        super.onKuiklyRenderContentViewCreated()
        delegate?.onKuiklyRenderContentViewCreated()
    }

    override fun syncRenderingWhenPageAppear(): Boolean {
        return delegate?.syncRenderingWhenPageAppear() ?: super.syncRenderingWhenPageAppear()
    }

    override fun enablePreloadClass(): Boolean {
        return delegate?.enablePreloadClass() ?: super.enablePreloadClass()
    }

    override fun softInputMode(): Int? {
        return delegate?.softInputMode() ?: super.softInputMode()
    }

    override fun onGetLaunchData(data: KRLaunchData) {
        super.onGetLaunchData(data)
        delegate?.onGetLaunchData(data)
    }

    override fun onGetPerformanceData(data: KRPerformanceData) {
        super.onGetPerformanceData(data)
        delegate?.onGetPerformanceData(data)
    }

    override fun onUnhandledException(
        throwable: Throwable,
        errorReason: ErrorReason,
        executeMode: KuiklyRenderCoreExecuteModeBase
    ) {
        super.onUnhandledException(throwable, errorReason, executeMode)
        delegate?.onUnhandledException(throwable, errorReason, executeMode)
    }

    override fun onPageLoadComplete(
        isSucceed: Boolean,
        errorReason: ErrorReason?,
        executeMode: KuiklyRenderCoreExecuteModeBase
    ) {
        super.onPageLoadComplete(isSucceed, errorReason, executeMode)
        delegate?.onPageLoadComplete(isSucceed, errorReason, executeMode)
    }

}
