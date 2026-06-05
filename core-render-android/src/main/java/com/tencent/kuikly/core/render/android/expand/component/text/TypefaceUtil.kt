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

package com.tencent.kuikly.core.render.android.expand.component.text

import android.graphics.Typeface
import android.os.Build
import android.util.LruCache
import com.tencent.kuikly.core.render.android.KuiklyContextParams
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager

class TypeFaceLoader(private val contextParams: KuiklyContextParams? = null) {
    private class Key(val fontFamilyName: String, val italic: Boolean) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Key) return false
            return italic == other.italic && fontFamilyName == other.fontFamilyName
        }

        override fun hashCode(): Int {
            var result = fontFamilyName.hashCode()
            result = 31 * result + italic.hashCode()
            return result
        }
    }
    private val sFontCache: LruCache<Key, Typeface> = LruCache(10)

    /**
     * Parses a font family entry that may include a weight suffix in the format
     * "fontName:weight". Returns the font name and optional weight.
     */
    private data class FontFamilyEntry(val fontName: String, val weight: Int?) {
        companion object {
            fun parse(raw: String): FontFamilyEntry {
                val lastColon = raw.lastIndexOf(':')
                if (lastColon == -1) return FontFamilyEntry(raw, null)
                val weightStr = raw.substring(lastColon + 1)
                val weight = weightStr.toIntOrNull()
                return if (weight != null) {
                    FontFamilyEntry(raw.substring(0, lastColon), weight)
                } else {
                    FontFamilyEntry(raw, null)
                }
            }
        }
    }

    fun getTypeface(fontFamilyName: String, italic: Boolean): Typeface {
        val key = Key(fontFamilyName, italic)
        return sFontCache.get(key) ?: createTypeface(key)
    }

    private fun createTypeface(key: Key): Typeface {
        val rawNameList: List<String> = if (key.fontFamilyName.indexOf(',') == -1) {
            listOf(key.fontFamilyName)
        } else {
            key.fontFamilyName.split(',')
        }
        val entries = rawNameList.map { FontFamilyEntry.parse(it.trim()) }
        var typeface: Typeface? = null
        var systemDefault: Typeface? = null
        val style = if (key.italic) Typeface.ITALIC else Typeface.NORMAL
        for (entry in entries) {
            if (entry.fontName.isEmpty()) {
                continue
            }
            KuiklyRenderAdapterManager.krFontAdapter?.getTypeface(entry.fontName, contextParams) {
                typeface = it
            }
            if (typeface != null && typeface != Typeface.DEFAULT) {
                val weighted = applyWeight(typeface!!, entry.weight)
                sFontCache.put(key, weighted)
                return weighted
            }
            if (systemDefault == null) {
                systemDefault = Typeface.defaultFromStyle(style)
            }
            typeface = Typeface.create(entry.fontName, style)
            if (typeface != null && typeface != systemDefault) {
                val weighted = applyWeight(typeface!!, entry.weight)
                sFontCache.put(key, weighted)
                return weighted
            }
        }
        typeface = systemDefault ?: Typeface.defaultFromStyle(style)
        sFontCache.put(key, typeface)
        return typeface!!
    }

    private fun applyWeight(typeface: Typeface, weight: Int?): Typeface {
        if (weight == null) return typeface
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return typeface
        return Typeface.create(typeface, weight, false)
    }
}
