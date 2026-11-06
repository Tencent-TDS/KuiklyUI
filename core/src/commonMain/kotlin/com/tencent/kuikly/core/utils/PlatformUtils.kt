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
        val pageData = getCurrentPageData() ?: return false
        return isLiquidGlassSupported(pageData)
    }

    /**
     * 判断指定页面所在平台是否支持LiquidGlass功能（iOS 26.0+ / macOS 26.0+）
     */
    fun isLiquidGlassSupported(pageData: PageData): Boolean {
        if (!pageData.isIOS && !pageData.isMacOS) {
            return false
        }
        
        return try {
            var osVersion = pageData.osVersion
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

    /** 应用级沉浸光感材质状态（MaterialState），由系统/用户设置控制、应用只读。 */
    object ImmersiveMaterialState {
        /** 默认：仅 Dialog/Toast/AlphabetIndexer 默认开，其余组件需显式设置 */
        const val DEFAULT = 0

        /** 开启：Toast/Menu/Dialog 等浮层默认带材质，且材质样式优先于背景/模糊/阴影 */
        const val ENABLE = 1

        /** 关闭：任何组件的沉浸光感都不生效 */
        const val DISABLE = 2
    }

    /**
     * 读取应用级材质状态（鸿蒙专属）。
     *
     * 决定 Toast/Menu 等系统浮层是否**默认带材质**：
     * - [ImmersiveMaterialState.ENABLE]：浮层默认带 THICK 材质，业务无需逐个设置；
     * - [ImmersiveMaterialState.DISABLE]：所有材质不生效（业务应回退普通样式）；
     * - [ImmersiveMaterialState.DEFAULT]：仅 Dialog/Toast 等少数浮层默认开。
     *
     * 该状态由系统/用户设置控制（系统设置中的沉浸光感开关），应用只读不可写；
     * 低版本系统或非鸿蒙平台返回 [ImmersiveMaterialState.DEFAULT]。
     */
    fun getImmersiveMaterialState(): Int {
        if (!isOhOs()) {
            return ImmersiveMaterialState.DEFAULT
        }
        return getCurrentPageData()?.getFeatureInt("immersive_material_state")
            ?: ImmersiveMaterialState.DEFAULT
    }

}