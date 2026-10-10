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

    /**
     * 判断当前平台是否支持鸿蒙「沉浸光感」（Immersive Light Sense）。
     *
     * 与 iOS 的 [isLiquidGlassSupported] 是两个独立赛道：沉浸光感的视觉表现、生效范围
     * 与 API 都与液态玻璃不同，因此不做跨端统一，鸿蒙单独提供一套能力。
     *
     * 取值来源是渲染层上报的 feature 标记（由系统接口判定真实设备能力），
     * 而不是 OS 版本号——同一版本号的机型也可能因算力档不支持。
     *
     * 注意：即使本接口返回 true，材质也只在系统规定的生效区域内渲染
     * （弹窗类接口、原生 Slider/Toggle/Select，或 Navigation 标题栏 / 底部 TabBar 内）。
     */
    fun isImmersiveMaterialSupported(): Boolean {
        if (!isOhOs()) {
            return false
        }
        return getCurrentPageData()?.hasFeature("immersive_material") ?: false
    }

}