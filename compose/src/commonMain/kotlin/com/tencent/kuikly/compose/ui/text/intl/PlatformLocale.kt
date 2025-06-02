/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.compose.ui.text.intl


// 实现 PlatformLocale 类
class PlatformLocale : Locale {
    // 提供 ISO 639 兼容的语言代码
    internal val language1: String

    // 提供 ISO 15924 兼容的 4 字母脚本代码
    internal val script1: String

    // 提供 ISO 3166 兼容的区域代码
    internal val region1: String

    // 构造函数，初始化语言、脚本和区域代码
    constructor(language: String, script: String, region: String) : super("") {
        this.language1 = language
        this.script1 = script
        this.region1 = region
    }

    // 返回符合 IETF BCP47 的语言标签表示
    internal fun getLanguageTag(): String {
        return if (script.isNotEmpty()) {
            "$language-$script-$region"
        } else {
            "$language-$region"
        }
    }
}

// 实现 PlatformLocaleDelegate 接口
internal class PlatformLocaleDelegateImpl : PlatformLocaleDelegate {
    // 返回当前语言环境列表
    override val current: LocaleList
        get() {
            // 这里简单示例，假设创建一个包含一个语言环境的列表
            val locale = PlatformLocale("en", "Latn", "US")
            return LocaleList(listOf(locale))
        }

    // 解析符合 IETF BCP47 的语言标签
    override fun parseLanguageTag(languageTag: String): PlatformLocale {
        val parts = languageTag.split("-")
        return when (parts.size) {
            2 -> PlatformLocale(parts[0], "", parts[1])
            3 -> PlatformLocale(parts[0], parts[1], parts[2])
            else -> throw IllegalArgumentException("Invalid language tag: $languageTag")
        }
    }
}

internal interface PlatformLocaleDelegate {
    /**
     * Returns the list of current locales.
     *
     * The implementation must return at least one locale.
     */
    val current: LocaleList

    /**
     * Parse the IETF BCP47 compliant language tag.
     *
     * @return The locale
     */
    fun parseLanguageTag(languageTag: String): PlatformLocale
}

// 创建 PlatformLocaleDelegate 实例的函数
internal fun createPlatformLocaleDelegate(): PlatformLocaleDelegate {
    return PlatformLocaleDelegateImpl()
}

internal val platformLocaleDelegate = createPlatformLocaleDelegate()
