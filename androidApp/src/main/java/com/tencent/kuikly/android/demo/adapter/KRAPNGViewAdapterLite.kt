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

package com.tencent.kuikly.android.demo.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import com.tencent.kuikly.core.render.android.adapter.IAPNGView
import com.tencent.kuikly.core.render.android.adapter.IAPNGViewAnimationListener
import com.tencent.kuikly.core.render.android.adapter.IKRAPNGViewAdapter
import java.io.File
import java.nio.ByteBuffer
import java.util.zip.CRC32

/**
 * 零第三方依赖的 APNG 宿主适配器（参考实现）。
 *
 * 与 [KRAPNGViewAdapter]（依赖 APNG4Android 库）不同，本实现不引入任何第三方库：
 * 手工解析 APNG 块结构（IHDR/acTL/fcTL/IDAT/fdAT），逐帧重建标准 PNG 交给
 * BitmapFactory 解码，Handler 按帧延时切帧，只依赖 Android 稳定 API。
 *
 * 适用边界：仅支持「全帧」APNG（每帧尺寸=画布尺寸、offset=0、blend/dispose=0），
 * 这类文件覆盖了大多数图标/加载动效场景（常见导出工具默认产出全帧）。
 * 解析失败或不含动画帧时降级为静态图显示，不崩溃。
 *
 * 使用：在 Application.onCreate 注册
 *   KuiklyRenderAdapterManager.krAPNGViewAdapter = KRAPNGViewAdapterLite()
 */
class KRAPNGViewAdapterLite : IKRAPNGViewAdapter {
    override fun createAPNGView(context: Context): IAPNGView = KRAPNGLiteImageView(context)
}

private class KRAPNGLiteImageView(context: Context) : IAPNGView {

    private class Frame(val png: ByteArray, val delayMs: Long)

    private val imageView = ImageView(context)
    private val handler = Handler(Looper.getMainLooper())
    private var frames: List<Frame> = emptyList()
    private var numPlays = 0 // acTL num_plays：0=无限
    private var repeatCount = 0 // Kuikly repeatCount：0=不限制（回退到文件 num_plays），N=播 N 次
    private var frameIndex = 0
    private var playedLoops = 0
    private var playing = false
    private val listeners = arrayListOf<IAPNGViewAnimationListener>()

    private val tick = object : Runnable {
        override fun run() {
            if (!playing || frames.isEmpty()) return
            val f = frames[frameIndex]
            imageView.setImageBitmap(BitmapFactory.decodeByteArray(f.png, 0, f.png.size))
            frameIndex++
            if (frameIndex >= frames.size) {
                playedLoops++
                val limit = if (repeatCount > 0) repeatCount else numPlays
                if (limit > 0 && playedLoops >= limit) {
                    // 播完停在最末帧并保持显示
                    playing = false
                    frameIndex = frames.size - 1
                    listeners.toList().forEach { it.onAnimationEnd(imageView) }
                    return
                }
                frameIndex = 0
            }
            handler.postDelayed(this, frames[frameIndex.coerceAtMost(frames.size - 1)].delayMs)
        }
    }

    override fun setFilePath(filePath: String) {
        frames = try {
            parseApng(File(filePath).readBytes())
        } catch (t: Throwable) {
            emptyList()
        }
        if (frames.isEmpty()) {
            // 解析失败/非动画：降级为静态图兜底（普通 PNG 也能正常显示）
            imageView.setImageBitmap(BitmapFactory.decodeFile(filePath))
        }
    }

    override fun asView(): View = imageView

    override fun setRepeatCount(count: Int) {
        repeatCount = count
    }

    override fun playAnimation() {
        if (frames.isEmpty()) return
        handler.removeCallbacks(tick)
        frameIndex = 0
        playedLoops = 0
        playing = true
        handler.post(tick)
    }

    override fun stopAnimation() {
        playing = false
        handler.removeCallbacks(tick)
    }

    override fun addAnimationListener(listener: IAPNGViewAnimationListener) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    override fun removeAnimationListener(listener: IAPNGViewAnimationListener) {
        listeners.remove(listener)
    }

    override fun setKRProp(propKey: String, value: Any): Boolean = false

    // ---------- 极简 APNG 解析（仅支持全帧：offset=0、blend/dispose=0） ----------

    private fun chunk(type: String, body: ByteArray): ByteArray {
        val crc = CRC32()
        crc.update(type.toByteArray(Charsets.US_ASCII))
        crc.update(body)
        val out = ByteBuffer.allocate(12 + body.size)
        out.putInt(body.size)
        out.put(type.toByteArray(Charsets.US_ASCII))
        out.put(body)
        out.putInt(crc.value.toInt())
        return out.array()
    }

    private fun parseApng(data: ByteArray): List<Frame> {
        val sig = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)
        if (data.size < 8 || !data.copyOfRange(0, 8).contentEquals(sig)) return emptyList()
        val buf = ByteBuffer.wrap(data)
        buf.position(8) // 跳过 PNG 签名
        var ihdr: ByteArray? = null
        var pendingDelayMs = 40L
        var pendingFirstFrame = false
        val result = arrayListOf<Frame>()
        while (buf.remaining() >= 12) {
            val len = buf.int
            if (len < 0 || buf.remaining() < len + 4) break // 截断文件，止损
            val typeBytes = ByteArray(4)
            buf.get(typeBytes)
            val type = String(typeBytes, Charsets.US_ASCII)
            val body = ByteArray(len)
            buf.get(body)
            buf.int // crc 跳过
            when (type) {
                "IHDR" -> ihdr = body
                "acTL" -> numPlays = ByteBuffer.wrap(body).getInt(4)
                "fcTL" -> {
                    val dn = ByteBuffer.wrap(body).getShort(20).toInt()
                    val dd = ByteBuffer.wrap(body).getShort(22).toInt()
                    // 规范：delay_den=0 按 100 计
                    pendingDelayMs = (dn * 1000L / (if (dd == 0) 100 else dd)).coerceAtLeast(10)
                    pendingFirstFrame = result.isEmpty()
                }
                "IDAT" -> {
                    // fcTL 之后的 IDAT = 动画首帧（首帧参与动画的常见结构）
                    if (pendingFirstFrame && ihdr != null) {
                        result.add(Frame(rebuildPng(ihdr!!, body), pendingDelayMs))
                        pendingFirstFrame = false
                    }
                }
                "fdAT" -> {
                    if (ihdr != null && body.size >= 4) {
                        // fdAT 数据前 4 字节为序号，其后为 IDAT 数据
                        result.add(Frame(rebuildPng(ihdr!!, body.copyOfRange(4, body.size)), pendingDelayMs))
                    }
                }
                "IEND" -> break
            }
        }
        return result
    }

    private fun rebuildPng(ihdr: ByteArray, idat: ByteArray): ByteArray {
        val sig = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)
        return sig + chunk("IHDR", ihdr) + chunk("IDAT", idat) + chunk("IEND", ByteArray(0))
    }
}
