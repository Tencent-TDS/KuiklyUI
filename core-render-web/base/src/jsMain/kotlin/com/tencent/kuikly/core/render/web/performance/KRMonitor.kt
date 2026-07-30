package com.tencent.kuikly.core.render.web.performance

import com.tencent.kuikly.core.render.web.IKuiklyRenderViewLifecycleCallback
import com.tencent.kuikly.core.render.web.exception.ErrorReason

abstract class KRMonitor<T> : IKuiklyRenderViewLifecycleCallback {
    abstract fun name(): String

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

    override fun onDestroy() {}

    abstract fun getMonitorData(): T?

    override fun onRenderException(throwable: Throwable, errorReason: ErrorReason) {}
}
