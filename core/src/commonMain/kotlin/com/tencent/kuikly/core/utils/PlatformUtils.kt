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

package com.tencent.kuikly.core.utils

import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.pager.PageData

/**
 * 平台工具类，提供统一的平台判断接口
 * 用于替代重复的 getPager().pageData.isIOS 调用
 */
object PlatformUtils {
    
    /**
     * 获取当前页面数据
     */
    private fun getCurrentPageData(): PageData? {
        return try {
            PagerManager.getCurrentPager().pageData
        } catch (e: Exception) {
            // 如果获取失败，返回null（非iOS平台）
            null
        }
    }
    
    /**
     * 判断当前是否为iOS平台
     */
    fun isIOS(): Boolean {
        return getCurrentPageData()?.isIOS ?: false
    }

    /**
     * 判断当前是否为macOS平台
     */
    fun isMacOS(): Boolean {
        return getCurrentPageData()?.isMacOS ?: false
    }

    /**
     * 判断当前是否为iOS或macOS平台
     */
    fun isIOSOrMacOS(): Boolean {
        return isIOS() || isMacOS()
    }
    
    /**
     * 判断当前是否为Android平台
     */
    fun isAndroid(): Boolean {
        return getCurrentPageData()?.isAndroid ?: false
    }
    
    /**
     * 判断当前是否为HarmonyOS平台
     */
    fun isOhOs(): Boolean {
        return getCurrentPageData()?.isOhOs ?: false
    }
    
    /**
     * 获取当前平台名称
     */
    fun getPlatform(): String {
        return getCurrentPageData()?.platform ?: "unknown"
    }
    
    /**
     * 获取当前系统版本
     */
    fun getOSVersion(): String {
        return getCurrentPageData()?.osVersion ?: ""
    }
    
    /**
     * 判断当前平台是否支持LiquidGlass功能
     * 目前只有iOS 26.0+和macOS 26.0+支持
     */
    fun isLiquidGlassSupported(): Boolean {
        if (!isIOSOrMacOS()) {
            return false
        }
        
        return try {
            var osVersion = getOSVersion()
            if (osVersion.isEmpty()) {
                return false
            }
            
            // 适配Mac版本信息格式 "Version 26.1 (Build 25B5042k)"
            if (osVersion.startsWith("Version ")) {
                // 提取 "Version 26.1 (Build ...)" 中的版本号部分
                val versionMatch = Regex("""Version\s+([\d.]+)""").find(osVersion)
                osVersion = versionMatch?.groupValues?.getOrNull(1) ?: ""
                if (osVersion.isEmpty()) {
                    return false
                }
            }
            
            // 解析版本号，支持格式如 "26.0", "26.1.2" 等
            val versionParts = osVersion.split(".")
            if (versionParts.isEmpty()) {
                return false
            }
            
            val majorVersion = versionParts[0].toIntOrNull() ?: return false
            majorVersion >= 26
        } catch (e: Exception) {
            false
        }
    }

}