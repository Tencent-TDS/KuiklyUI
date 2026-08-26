package com.tencent.kuikly.core.render.web.utils



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
        // Detect mini-program runtime. Do NOT rely on `typeof wx !== 'undefined'` alone,
        // because WeChat JS-SDK in normal H5 also injects `wx`.
        val isNoWindow = js(
            "typeof window === 'undefined'"
        ).unsafeCast<Boolean>()
        val hasWxRuntimeApi = js(
            "typeof wx !== 'undefined' && typeof wx.getSystemInfoSync === 'function'"
        ).unsafeCast<Boolean>()
        val hasMiniProgramGlobals = js(
            "typeof getApp === 'function' && typeof Page === 'function'"
        ).unsafeCast<Boolean>()

        if (isNoWindow || hasWxRuntimeApi || hasMiniProgramGlobals) {
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
