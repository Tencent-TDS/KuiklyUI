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

import com.tencent.kuikly.core.render.android.KuiklyRenderView
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderLog
import com.tencent.kuikly.core.render.android.const.KRCssConst
import com.tencent.kuikly.core.render.android.const.KRExtConst
import com.tencent.kuikly.core.render.android.const.KRViewConst
import com.tencent.kuikly.core.render.android.context.KuiklyRenderCommonContextHandler
import com.tencent.kuikly.core.render.android.context.KuiklyRenderJvmContextHandler
import com.tencent.kuikly.core.render.android.context.KuiklyRenderNativeMethod
import com.tencent.kuikly.core.render.android.core.KuiklyRenderCore
import com.tencent.kuikly.core.render.android.css.animation.KRCSSAnimation
import com.tencent.kuikly.core.render.android.css.animation.KRCSSTransform
import com.tencent.kuikly.core.render.android.css.decoration.KRViewDecoration
import com.tencent.kuikly.core.render.android.css.drawable.KRCSSBackgroundDrawable
import com.tencent.kuikly.core.render.android.css.gesture.KRCSSGestureDetector
import com.tencent.kuikly.core.render.android.css.ktx.isMainThread
import com.tencent.kuikly.core.render.android.css.ktx.stackTraceToString
import com.tencent.kuikly.core.render.android.expand.component.KRAPNGView
import com.tencent.kuikly.core.render.android.expand.component.KRImageView
import com.tencent.kuikly.core.render.android.expand.component.KRRichTextShadow
import com.tencent.kuikly.core.render.android.expand.component.KRView
import com.tencent.kuikly.core.render.android.expand.component.KRHoverView

import com.tencent.kuikly.core.render.android.expand.component.KRWrapperImageView
import com.tencent.kuikly.core.render.android.expand.component.KRActivityIndicatorView
import com.tencent.kuikly.core.render.android.expand.component.KRTextFieldView
import com.tencent.kuikly.core.render.android.expand.component.KRTextProps
import com.tencent.kuikly.core.render.android.expand.component.KRCanvasView
import com.tencent.kuikly.core.render.android.expand.component.KRRichTextView
import com.tencent.kuikly.core.render.android.expand.component.blur.KRBlurView
import com.tencent.kuikly.core.render.android.expand.component.list.KRRecyclerContentView
// import com.tencent.kuikly.core.render.android.expand.component.list.KRRecyclerView
import com.tencent.kuikly.core.render.android.expand.component.pag.KRPAGView
import com.tencent.kuikly.core.render.android.expand.module.KRLogModule
import com.tencent.kuikly.core.render.android.expand.module.KRMemoryCacheModule
import com.tencent.kuikly.core.render.android.expand.module.KRSharedPreferencesModule
import com.tencent.kuikly.core.render.android.expand.vendor.KRFileManager
import com.tencent.kuikly.core.render.android.layer.KuiklyRenderLayerHandler
import com.tencent.kuikly.core.render.android.scheduler.KuiklyRenderCoreUIScheduler
import java.util.concurrent.Executors

/*
 * 用于触发类加载，避免首次加载带来的类加载耗时
 */
object KuiklyRenderClassLoad {
    private var didLoadRenderClass = false
    init {
        loadRenderClassIfNeed()
    }
    /*
     * 触发Kuikly SDK类加载（子线程并行执行类加载）
     */
    fun loadRenderClassIfNeed() {
        if (didLoadRenderClass) {
            return
        }
        didLoadRenderClass = true
        // 并发加载类(每个类加载普遍1-3ms)
        executeOnSubThread {
            try {
                KuiklyRenderView
                KuiklyRenderCore
                KuiklyRenderCommonContextHandler
                KuiklyRenderLayerHandler
                KuiklyRenderJvmContextHandler
                KuiklyRenderCoreUIScheduler
                loadCoreClass()
                KRView
                KRImageView
                KRRichTextView
                KRRichTextShadow
                KRTextProps
                KRCSSBackgroundDrawable
                KRViewDecoration
                KRViewConst
                KRHoverView
                KRBlurView
                KRCSSTransform
                KRLogModule
                try {
                    com.tencent.kuikly.core.render.android.expand.component.list.KRRecyclerView
                } catch (e : Throwable) {
                   // do nothing
                }
                KRWrapperImageView
                KRCSSGestureDetector
                KRActivityIndicatorView
                KRTextFieldView
                KRExtConst
                KRCanvasView
                KRCssConst
                KRRecyclerContentView
                KRCSSAnimation
                KuiklyRenderLog
                KuiklyRenderNativeMethod
                KRMemoryCacheModule
                KRSharedPreferencesModule
                KRFileManager
                KRAPNGView
                KRPAGView
            } catch (e : Throwable) {
                KuiklyRenderLog.e("KuiklyRenderClassLoad",
                    "exception:${e.stackTraceToString()}")
            }

        }
    }

    /**
     * 触发Core的类加载
     * @param classLoader 加载core类的类加载器
     */
    fun loadCoreClass(classLoader: ClassLoader? = null) {
        val loadClassAction: () -> Unit = {
            try {
                val innerClass = Class.forName("com.tencent.kuikly.core.global.CoreClassLoader", true, classLoader ?: javaClass.classLoader)
                innerClass.newInstance()
            } catch (e: Throwable) {
                KuiklyRenderLog.e("KuiklyRenderClassLoad",
                    "loadCoreClassException:${e.stackTraceToString()}")
            }
        }
        if (isMainThread()) { // 在外部主动使用该方法时可保证子线程执行
            executeOnSubThread(loadClassAction) // 并发加载类(每个类加载普遍1-3ms)
        } else {
            loadClassAction()
        }
    }

    private fun executeOnSubThread(task: () -> Unit) {
        val threadAdapter = KuiklyRenderAdapterManager.krThreadAdapter
        if (threadAdapter != null) {
            threadAdapter.executeOnSubThread(task)
        } else {
            val executor = Executors.newSingleThreadExecutor()
            executor.execute {
                task()
                executor.shutdown()
            }
        }
    }

}