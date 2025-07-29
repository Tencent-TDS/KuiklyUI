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

package com.tencent.kuikly.core.render.android.expand.component.pag

import android.content.Context
import android.view.View
import com.tencent.kuikly.core.render.android.adapter.IPAGViewListener
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.expand.component.KRAPNGView
import com.tencent.kuikly.core.render.android.expand.component.KRView
import com.tencent.kuikly.core.render.android.expand.module.KRCodecModule
import com.tencent.kuikly.core.render.android.expand.vendor.KRFileManager
import com.tencent.kuikly.core.render.android.export.KuiklyRenderCallback
import java.io.File

/**
 * Created by kam on 2023/5/17.
 */
class KRPAGView(context: Context) : KRView(context), IPAGViewListener {
    private var src = ""
    var autoPlay = true

    private var loadFailureCallback: KuiklyRenderCallback? = null
    private var animationStartCallback: KuiklyRenderCallback? = null
    private var animationEndCallback: KuiklyRenderCallback? = null
    private var animationCancelCallback: KuiklyRenderCallback? = null
    private var animationRepeatCallback: KuiklyRenderCallback? = null

    private var hadStop = false
    private var hadFilePath = false
    private var didLayout = false

    private var pagView = KuiklyRenderAdapterManager.krPagViewAdapter?.createPAGView(context)?.apply {
        addPAGViewListener(this@KRPAGView)
        val view = asView()
        view.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(view)
    }

    override val reusable: Boolean
        get() = false

    override fun setProp(propKey: String, propValue: Any): Boolean {
        var result = when (propKey) {
            SRC -> setSrc(propValue)
            REPEAT_COUNT -> repeatCount(propValue)
            AUTO_PLAY -> autoPlay(propValue)
            REPLACE_TEXT_LAYER_CONTENT -> setReplaceTextLayerContent(propValue)
            REPLACE_IMAGE_LAYER_CONTENT -> setReplaceImageLayerContent(propValue)
            LOAD_FAIL -> {
                loadFailureCallback = propValue as KuiklyRenderCallback
                true
            }
            ANIMATION_START -> {
                animationStartCallback = propValue as KuiklyRenderCallback
                true
            }
            ANIMATION_END -> {
                animationEndCallback = propValue as KuiklyRenderCallback
                true
            }
            ANIMATION_CANCEL -> {
                animationCancelCallback = propValue as KuiklyRenderCallback
                true
            }
            ANIMATION_REPEAT -> {
                animationRepeatCallback = propValue as KuiklyRenderCallback
                true
            }
            else -> super.setProp(propKey, propValue)
        }
        if (!result) {
            result = pagView?.setKRProp(propKey, propValue) ?: false
        }
        return result
    }

    override fun call(method: String, params: String?, callback: KuiklyRenderCallback?): Any? {
        return when (method) {
            METHOD_PLAY -> play(params)
            METHOD_STOP -> stop(params)
            else -> super.call(method, params, callback)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stop(null)
        pagView?.removePAGViewListener(this)
    }

    override fun onAnimationStart(pagView: View) {
        animationStartCallback?.invoke(mapOf<String, Any>())
    }

    override fun onAnimationEnd(pagView: View) {
        animationEndCallback?.invoke(mapOf<String, Any>())
    }

    override fun onAnimationCancel(pagView: View) {
        animationCancelCallback?.invoke(mapOf<String, Any>())
    }

    override fun onAnimationRepeat(pagView: View) {
        animationRepeatCallback?.invoke(mapOf<String, Any>())
    }

    /**
     * 获取(下载)cdnUrl对应的pag文件接口
     * 注意: 如果该文件已存在（下载过&未被清理），则直接返回使用本地缓存文件
     * @param cdnUrl
     * @param resultCallback
     * @receiver
     */
    fun fetchPagFileIfNeedWithCdnUrl(
        cdnUrl: String,
        resultCallback: (filePath: String?) -> Unit
    ) {
        val storePath = generateStorePathWithCdnUrl(cdnUrl)
        kuiklyRenderContext?.also {
            KRFileManager.fetchFile(it, cdnUrl, storePath) { filePath ->
                if (filePath == null) {
                    loadFailureCallback?.invoke(null)
                    resultCallback(null)
                } else {
                    resultCallback(filePath)
                    //resultCallback可能同一帧返回，此时autoPlay值为true（默认值），所以当前帧调用autoPlay会一定播放，需要得等下一帧，css_autoPlay确定了，才去可能的播放。
                    post { tryAutoPlay() }
                }
            }
        }
    }

    /**
     * 根据cdnUrl返回唯一的Pag文件本地磁盘存储地址，用于判断文件是否已经存在
     * @param cdnUrl
     * @return
     */
    fun generateStorePathWithCdnUrl(cdnUrl: String): String {
        val codecModule = kuiklyRenderContext?.module<KRCodecModule>(KRCodecModule.MODULE_NAME)
        val fileBaseName = codecModule?.md5(cdnUrl) ?: cdnUrl

        val cacheDir = File(context.cacheDir.absolutePath + "/kuikly")
        if (!cacheDir.exists()) {
            cacheDir.mkdir()
        }
        return "${cacheDir.absolutePath}/kuikly_pag_$fileBaseName.pag"
    }

    private fun play(params: String?) {
        this.autoPlay = true
        hadStop = false
        pagView?.playPAGView()
    }

    private fun stop(params: String?) {
        this.autoPlay = false
        if (!hadStop) {
            hadStop = true
            pagView?.stopPAGView()
        }
    }

    private fun setSrc(params: Any): Boolean {
        val newSrc = params as String
        if (src == newSrc) {
            return true
        }
        src = newSrc
        // 判断是否为网络cdn url
        if (KRAPNGView.isCdnUrl(src)) {
            fetchPagFileIfNeedWithCdnUrl(src) {
                it?.also { filePath ->
                    setFilePath(filePath)
                }
            }
        } else {
            setFilePath(src)
            post {  tryAutoPlay() }
        }
        return true
    }

    private fun setFilePath(filePath: String) {
        pagView?.setFilePath(filePath)
        hadFilePath = true
    }

    private fun repeatCount(params: Any): Boolean {
        pagView?.setPAGViewRepeatCount((params as Int))
        return true
    }

    private fun setReplaceTextLayerContent(params: Any): Boolean {
        val string = params as? String
        val (layerName, text) = string?.split(",") ?: return false
        pagView?.replaceTextLayerContent(layerName, text)
        return true
    }

    private fun setReplaceImageLayerContent(params: Any): Boolean {
        val string = params as? String
        val (layerName, imageFilePath) = string?.split(",") ?: return false
        pagView?.replaceImageLayerContent(layerName, imageFilePath)
        return true
    }

    private fun autoPlay(propValue: Any): Boolean {
        this.autoPlay = propValue as Int == 1
        if (autoPlay) {
            tryAutoPlay()
        }
        return true
    }

    private fun tryAutoPlay() {
        if (this.autoPlay && hadFilePath) {
            pagView?.playPAGView()
        }
    }

    companion object {
        private const val SRC = "src"
        private const val REPEAT_COUNT = "repeatCount"
        private const val AUTO_PLAY = "autoPlay"
        private const val REPLACE_TEXT_LAYER_CONTENT = "replaceTextLayerContent"
        private const val REPLACE_IMAGE_LAYER_CONTENT = "replaceImageLayerContent"
        private const val LOAD_FAIL = "loadFailure"
        private const val ANIMATION_START = "animationStart"
        private const val ANIMATION_END = "animationEnd"
        private const val ANIMATION_CANCEL = "animationCancel"
        private const val ANIMATION_REPEAT = "animationRepeat"

        private const val METHOD_PLAY = "play"
        private const val METHOD_STOP = "stop"

        const val VIEW_NAME = "KRPAGView"
    }

}