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

package com.tencent.kuikly.core.render.android.export

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.UiThread
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.render.android.IKuiklyRenderContext
import com.tencent.kuikly.core.render.android.KuiklyRenderView
import com.tencent.kuikly.core.render.android.const.KRCssConst
import com.tencent.kuikly.core.render.android.css.decoration.IKRViewDecoration
import com.tencent.kuikly.core.render.android.css.ktx.activity
import com.tencent.kuikly.core.render.android.css.ktx.clearViewData
import com.tencent.kuikly.core.render.android.css.ktx.contentOverBounds
import com.tencent.kuikly.core.render.android.css.ktx.hasInitAccessibilityDelegate
import com.tencent.kuikly.core.render.android.css.ktx.drawCommonDecoration
import com.tencent.kuikly.core.render.android.css.ktx.drawCommonForegroundDecoration
import com.tencent.kuikly.core.render.android.css.ktx.frameOverBounds
import com.tencent.kuikly.core.render.android.css.ktx.hasCustomClipPath
import com.tencent.kuikly.core.render.android.css.ktx.optViewDecorator
import com.tencent.kuikly.core.render.android.css.ktx.removeOnSetFrameObservers
import com.tencent.kuikly.core.render.android.css.ktx.removeViewData
import com.tencent.kuikly.core.render.android.css.ktx.resetCommonProp
import com.tencent.kuikly.core.render.android.css.ktx.setCommonProp
import com.tencent.kuikly.core.render.android.css.ktx.setContentOverBounds
import com.tencent.kuikly.core.render.android.css.ktx.shouldClipContent
import com.tencent.kuikly.core.render.android.css.ktx.stopAnimations
import com.tencent.kuikly.core.render.android.css.ktx.transformOverBounds
import com.tencent.kuikly.core.render.android.expand.module.KRMemoryCacheModule
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

/**
 * 渲染视图组件协议, 组件通过实现[IKuiklyRenderViewExport]协议 完成一个kuikly ui组件暴露
 */
interface IKuiklyRenderViewExport : IKuiklyRenderModuleExport, IKRViewDecoration {

    /**
     * 更新属性时调用
     * @param propKey 视图实例属性名
     * @param propValue 视图实例属性值，类型一般为字符串等基础数据结构以及KuiklyRenderCallback（用于事件绑定）
     * @return 是否处理，返回true并且组件是[IKuiklyRenderViewExport.reusable]为true时，
     * [com.tencent.kuikly.core.render.android.layer.IKuiklyRenderLayerHandler]会记录属性的key，
     * 在组件被复用时，调用[IKuiklyRenderViewExport.resetProp]
     * 和[IKuiklyRenderViewExport.resetShadow]方法供组件重置View
     */
    @UiThread
    fun setProp(propKey: String, propValue: Any): Boolean = view().setCommonProp(propKey, propValue)

    override fun drawCommonDecoration(w: Int, h: Int, canvas: Canvas) {
        view().drawCommonDecoration(canvas)
    }

    override fun drawCommonForegroundDecoration(w: Int, h: Int, canvas: Canvas) {
        view().drawCommonForegroundDecoration(canvas)
    }

    override fun hasCustomClipPath(): Boolean = view().hasCustomClipPath()

    /**
     * 重置view, 准备被复用 (可选实现). 若实现该方法返回true则意味着能被复用
     */
    val reusable: Boolean
        get() = false

    /**
     * 重置属性
     * @param propKey 被重置属性的key
     */
    @UiThread
    fun resetProp(propKey: String): Boolean {
        val view = view()
        view.removeOnSetFrameObservers()
        return view.resetCommonProp(propKey)
    }

    /**
     * 重置shadow
     */
    @UiThread
    fun resetShadow() {
    }

    /**
     * 设置当前renderView实例对应的shadow对象
     */
    @UiThread
    fun setShadow(shadow: IKuiklyRenderShadowExport) {
    }

    /**
     * 实现[IKuiklyRenderViewExport]的[android.view.View]
     */
    fun view(): View = this as View

    /**
     * 获取Kuikly页面根View
     * @return Kuikly页面根View
     */
    fun krRootView(): ViewGroup? = kuiklyRenderContext?.kuiklyRenderRootView?.view

    /**
     * Kuikly render context
     */
    override var kuiklyRenderContext: IKuiklyRenderContext?
        get() {
            return view().context as? IKuiklyRenderContext
        }
        set(value) {}

    /**
     * 获取实现[IKuiklyRenderViewExport]的View所在的[Activity]
     */
    override val activity: Activity?
        get() {
             return view().context.activity
        }

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            "bringToFront" -> {
                view().bringToFront()
            }
            "accessibilityAnnounce" -> {
                val view = view()
                if (view.hasInitAccessibilityDelegate()) {
                    params?.apply {
                        view().announceForAccessibility(params)
                    }
                }
                ""
            }
            "accessibilityFocus" -> {
                val view = view()
                if (view.hasInitAccessibilityDelegate()) {
                    view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_HOVER_ENTER)
                    view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED)
                }
                ""
            }
            "toImage" -> {
                toImage(params, callback)
                ""
            }
            else -> super.call(method, params, callback)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        view().stopAnimations()
        view().clearViewData()
    }

    /**
     * 当[View]被add到父亲时，会回调该方法
     * @param parent [View]的父亲
     */
    fun onAddToParent(parent: ViewGroup) {
        if (KuiklyRenderView.lazyClipChildren) {
            val view = view()
            if (parent.clipChildren) {
                if (view.optViewDecorator()?.hasBoxShadow == true ||
                    (view is ViewGroup && view.contentOverBounds() && !view.shouldClipContent)) {
                    parent.clipChildren = false
                    parent.setContentOverBounds()
                } else if (view.transformOverBounds() || view.frameOverBounds()) {
                    parent.setContentOverBounds()
                }
            }
        }
    }

    /**
     * 当[View]被父亲remove时，会回调该方法
     * @param parent [View]的父亲
     */
    fun onRemoveFromParent(parent: ViewGroup) {}

    fun resetClipChildren() {
        if (KuiklyRenderView.lazyClipChildren) {
            (view() as? ViewGroup)?.clipChildren = true
        }
    }

    /**
     * View截图能力，对齐iOS/鸿蒙侧实现
     * @param params JSON参数：{ type: "cacheKey"|"dataUri"|"file", sampleSize: Int }
     * @param callback 回调格式：{ code: Int, data: String?, message: String? }
     */
    private fun toImage(params: String?, callback: KuiklyRenderCallback?) {
        if (callback == null) {
            return
        }

        val json = try { JSONObject(params ?: "{}") } catch (e: Exception) { JSONObject() }
        val type = json.optString("type")
        if (type.isEmpty()) {
            callback.invoke(mapOf("code" to -1, "message" to "type is required"))
            return
        }
        val sampleSize = maxOf(1, json.optInt("sampleSize", 1))
        val v = view()

        // 主线程同步截图
        val bitmap = safeSnapshot(v, sampleSize)
        if (bitmap == null) {
            callback.invoke(mapOf("code" to -1, "message" to "snapshot failed: bitmap is null"))
            return
        }

        when (type) {
            "dataUri" -> {
                // 异步编码 base64
                toImageExecutor.execute {
                    val base64 = bitmapToBase64(bitmap)
                    bitmap.recycle()
                    val dataUri = "data:image/png;base64,$base64"
                    Handler(Looper.getMainLooper()).post {
                        callback.invoke(mapOf("code" to 0, "data" to dataUri))
                    }
                }
            }
            "file" -> {
                // 异步保存到缓存目录
                toImageExecutor.execute {
                    val filePath = saveBitmapToTempFile(v, bitmap)
                    bitmap.recycle()
                    Handler(Looper.getMainLooper()).post {
                        if (filePath != null) {
                            callback.invoke(mapOf("code" to 0, "data" to "file://$filePath"))
                        } else {
                            callback.invoke(mapOf("code" to -1, "message" to "failed to save snapshot to file"))
                        }
                    }
                }
            }
            "cacheKey" -> {
                // 生成 data:image 前缀的 key，存入 KRMemoryCacheModule
                val cacheKey = "data:image_Md5_snapshot_${System.currentTimeMillis()}_${(Math.random() * 0xFFFFFF).toInt()}"
                val drawable = BitmapDrawable(v.resources, bitmap)
                val module = kuiklyRenderContext?.module<KRMemoryCacheModule>(KRMemoryCacheModule.MODULE_NAME)
                if (module == null) {
                    callback.invoke(mapOf("code" to -1, "message" to "snapshot failed: KRMemoryCacheModule is null"))
                    return
                }
                module?.set(cacheKey, drawable)
                callback.invoke(mapOf("code" to 0, "data" to cacheKey))
            }
            else -> {
                bitmap.recycle()
                callback.invoke(mapOf("code" to -1, "message" to "unsupported type: $type"))
            }
        }
    }

    companion object {
        private const val TAG = "KRView"
        private val toImageExecutor = Executors.newSingleThreadExecutor()

        /**
         * 安全截图：通过 View.draw(Canvas) 将 View 绘制到 Bitmap 上
         */
        private fun safeSnapshot(view: View, sampleSize: Int): Bitmap? {
            val width = view.width
            val height = view.height
            if (width <= 0 || height <= 0) return null
            return try {
                val scaledWidth = maxOf(1, width / sampleSize)
                val scaledHeight = maxOf(1, height / sampleSize)
                val bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                if (sampleSize > 1) {
                    val scale = 1f / sampleSize
                    canvas.scale(scale, scale)
                }
                view.draw(canvas)
                bitmap
            } catch (e: Exception) {
                KLog.e(TAG, "[toImage] snapshot exception: ${e.message}")
                null
            }
        }

        /**
         * Bitmap 编码为 PNG base64 字符串
         */
        private fun bitmapToBase64(bitmap: Bitmap): String {
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        }

        /**
         * 保存 Bitmap 到应用缓存目录的临时文件
         */
        private fun saveBitmapToTempFile(view: View, bitmap: Bitmap): String? {
            return try {
                val cacheDir = view.context.cacheDir
                val fileName = "kr_snapshot_${System.currentTimeMillis()}_${(Math.random() * 0xFFFFFF).toInt()}.png"
                val file = File(cacheDir, fileName)
                FileOutputStream(file).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                    fos.flush()
                }
                file.absolutePath
            } catch (e: Exception) {
                KLog.e(TAG, "[toImage] save file exception: ${e.message}")
                null
            }
        }
    }
}
