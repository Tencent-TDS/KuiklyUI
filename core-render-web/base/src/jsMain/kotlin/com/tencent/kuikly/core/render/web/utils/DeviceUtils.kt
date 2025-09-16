package com.tencent.kuikly.core.render.web.utils

import com.tencent.kuikly.core.render.web.ktx.kuiklyWindow

/**
 * Device type enumeration
 */
enum class DeviceType {
    MOBILE,      // Mobile device with touch support
    DESKTOP,     // Desktop/PC with mouse support  
    MINIPROGRAM  // Mini-program environment (WeChat, etc.)
}

/**
 * Device detection utilities
 */
object DeviceUtils {
    
    /**
     * Detect current device type
     */
    fun detectDeviceType(): DeviceType {
        // Check mini-program WeChat environment 
        val isInMiniProgram =
            js("typeof window === 'undefined' || typeof wx !== 'undefined'").unsafeCast<Boolean>()

        if (isInMiniProgram) {
            // In mini-program environment, return specific type
            return DeviceType.MINIPROGRAM
        }
        
        val hasTouchSupport =
            js("typeof window !== 'undefined' && ('ontouchstart' in window || (navigator.maxTouchPoints && navigator.maxTouchPoints > 0))").unsafeCast<Boolean>()

        // Check for mobile user agents
        val isMobile =
            js("""
                typeof navigator !== 'undefined' && (
                    /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent) ||
                    (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)
                )
            """).unsafeCast<Boolean>()

        // Prefer touch support detection, fallback to user agent
        return if (hasTouchSupport || isMobile) DeviceType.MOBILE else DeviceType.DESKTOP
    }
}
