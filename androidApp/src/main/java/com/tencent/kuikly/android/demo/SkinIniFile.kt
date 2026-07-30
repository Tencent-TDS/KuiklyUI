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

import android.content.Context
import android.util.Log
import com.tencent.kuikly.android.demo.adapter.execOnSubThread
import java.io.IOException
import java.util.regex.Pattern

typealias OnLoadFinishListener = SkinIniFile.(Boolean) -> Unit

/**
 * ini 文件解析类，解析 .ini 文件的内容，将文件的结点读取到一个 [Section] Map 中，
 * 外部可通过 [load] 方法加载文件，通过 [get] 方法获取指定的结点
 */
class SkinIniFile(private val context: Context) {

    private val sections: MutableMap<String, Section> = mutableMapOf()

    data class Section(val name: String, val values: MutableMap<String, String> = mutableMapOf())

    @Volatile
    private var hasLoaded = false

    /**
     * 加载 [assetsPath] 对应的 ini 文件到内存，这是个 IO 操作, 内部切换到了子线程处理
     * 加载完成后，会调用 [onLoadFinishListener] 回调; 如果已经加载过不会重复加载
     */
    fun load(assetsPath: String, onLoadFinishListener: OnLoadFinishListener? = null) {
        execOnSubThread {
            loadInternal(assetsPath, onLoadFinishListener)
        }
    }

    private fun loadInternal(assetsPath: String, onLoadFinishListener: OnLoadFinishListener? = null) {
        if (hasLoaded) {
            onLoadFinishListener?.invoke(this, true)
            return
        }
        synchronized(this) {
            if (hasLoaded) {
                onLoadFinishListener?.invoke(this, true)
                return
            }
            try {
                context.assets.open(assetsPath).use { inputStream ->
                    inputStream.bufferedReader().use { bufferReader ->
                        var curSectionName = ""
                        bufferReader.forEachLine {
                            val cleanLineStr = it.trim { char -> char <= ' ' }
                            curSectionName = readEachLine(cleanLineStr, curSectionName)
                        }
                    }
                }
                hasLoaded = true
                onLoadFinishListener?.invoke(this@SkinIniFile, true)
            } catch (e: IOException) {
                Log.e(TAG, "加载 assets 失败 ${e.message}")
                hasLoaded = false
                onLoadFinishListener?.invoke(this@SkinIniFile, false)
            }
        }
    }

    /**
     * 同步获取 [sectionName] 对应的结点中的 [sectionKey] 对应的值, 如果没有找到，返回 [defaultValue]
     * 注意: 在 [load] 方法未完成之前，这个方法也会返回 [defaultValue], 可以在 [load] 方法完成后，再调用此方法，见 [OnLoadFinishListener]
     */
    fun get(sectionName: String, sectionKey: String, defaultValue: String? = null): String? {
        val section = sections[sectionName] ?: return defaultValue
        return section.values[sectionKey] ?: defaultValue
    }

    /**
     * 同步获取 [sectionName] 对应的结点中的所有的 key 列表, 如果没有找到，返回空列表
     */
    fun getAllSectionsKey(sectionName: String): List<String> {
        return sections[sectionName]?.values?.keys?.toList() ?: emptyList()
    }

    private fun readEachLine(cleanLineStr: String, sectionName: String): String {
        var curSectionName = sectionName
        if (sectionPattern.matcher(cleanLineStr).matches()) {
            curSectionName = cleanLineStr.substring(1, cleanLineStr.length - 1)
            val section = sections[curSectionName] ?: Section(curSectionName)
            sections[section.name] = section
        } else {
            val keyValue = cleanLineStr.split("=", limit = 2)
            if (keyValue.size == 2) {
                val section = sections[curSectionName] ?: Section(curSectionName)
                section.values[keyValue[0]] = keyValue[1]
                sections[section.name] = section
            }
        }
        return curSectionName
    }

    companion object {
        private val sectionPattern = Pattern.compile("^\\[.*\\]$")
        private const val TAG = "SkinIniFile"
    }

}