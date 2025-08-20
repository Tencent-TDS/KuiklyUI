package com.tencent.kuikly.core.render.web.expand.module

import com.tencent.kuikly.core.render.web.export.KuiklyRenderBaseModule
import com.tencent.kuikly.core.render.web.ktx.KuiklyRenderCallback

/**
 * Kuikly snapshot generation module, not supported in web
 */
class KRSnapshotModule : KuiklyRenderBaseModule() {
    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            SNAPSHOT_PAGER -> snapshotPager()
            else -> super.call(method, params, callback)
        }
    }


    /**
     * Get content from localStorage cache
     */
    private fun snapshotPager() {
        // This method is not supported in web, do nothing
    }


    companion object {
        const val MODULE_NAME = "KRSnapshotModule"
        private const val SNAPSHOT_PAGER = "snapshotPager"
    }
}
