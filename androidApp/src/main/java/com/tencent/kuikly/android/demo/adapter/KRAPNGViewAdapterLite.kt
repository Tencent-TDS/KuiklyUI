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
 * Zero-dependency APNG host adapter (reference implementation).
 *
 * Unlike [KRAPNGViewAdapter] (which relies on the APNG4Android library), this
 * implementation pulls in no third-party dependency: it parses the APNG chunk
 * structure (IHDR/acTL/fcTL/IDAT/fdAT) by hand, rebuilds each frame as a
 * standard PNG for BitmapFactory to decode, and switches frames on a Handler
 * using per-frame delays — stable Android APIs only.
 *
 * Scope: only "full-frame" APNG files are supported (every frame matches the
 * canvas size, offset = 0, blend/dispose = 0). Most export tools produce
 * full-frame output by default, which covers typical icon/loading animations.
 * On parse failure or missing animation frames, falls back to displaying a
 * static image instead of crashing.
 *
 * Usage: register in Application.onCreate
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
    private var numPlays = 0 // acTL num_plays: 0 = loop forever
    private var repeatCount = 0 // Kuikly repeatCount: 0 = fall back to file num_plays, N = play N times
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
                    // Finished: hold on the last frame
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
            // Parse failure or non-animated image: fall back to a static bitmap
            // (plain PNG files also render correctly this way) instead of crashing.
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

    // ---------- Minimal APNG parser (full-frame only: offset = 0, blend/dispose = 0) ----------

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
        buf.position(8) // skip PNG signature
        var ihdr: ByteArray? = null
        var pendingDelayMs = 40L
        var pendingFirstFrame = false
        val result = arrayListOf<Frame>()
        while (buf.remaining() >= 12) {
            val len = buf.int
            if (len < 0 || buf.remaining() < len + 4) break // truncated file, bail out
            val typeBytes = ByteArray(4)
            buf.get(typeBytes)
            val type = String(typeBytes, Charsets.US_ASCII)
            val body = ByteArray(len)
            buf.get(body)
            buf.int // skip crc
            when (type) {
                "IHDR" -> ihdr = body
                "acTL" -> numPlays = ByteBuffer.wrap(body).getInt(4)
                "fcTL" -> {
                    val dn = ByteBuffer.wrap(body).getShort(20).toInt()
                    val dd = ByteBuffer.wrap(body).getShort(22).toInt()
                    // Spec: delay_den = 0 means 100; clamp to >= 10ms to avoid a zero-delay spin
                    pendingDelayMs = (dn * 1000L / (if (dd == 0) 100 else dd)).coerceAtLeast(10)
                    pendingFirstFrame = result.isEmpty()
                }
                "IDAT" -> {
                    // IDAT following a fcTL is the first animation frame (the common layout
                    // where the default image is part of the animation)
                    if (pendingFirstFrame && ihdr != null) {
                        result.add(Frame(rebuildPng(ihdr!!, body), pendingDelayMs))
                        pendingFirstFrame = false
                    }
                }
                "fdAT" -> {
                    if (ihdr != null && body.size >= 4) {
                        // fdAT payload: 4-byte sequence number followed by IDAT data
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
