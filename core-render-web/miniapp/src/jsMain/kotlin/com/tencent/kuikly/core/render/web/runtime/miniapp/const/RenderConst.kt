package com.tencent.kuikly.core.render.web.runtime.miniapp.const

/**
 * Values that can be set in the mini program page's index.js,
 * used to micro-control runtime behavior under a single page
 */
internal object RenderConst {
    // Set pageName, this setting has the highest priority. If not set, it will try to get
    // page_name from mini program jump parameters. If neither exists, an error will be reported
    const val PAGE_NAME = "pageName"

    // Status bar height, set to 0 for fullscreen, defaults to actual status bar height
    const val STATUS_BAR_HEIGHT = "statusBarHeight"
    const val PLATFORM = "platform"
    const val IOS_TEXT_FONT = "'San Francisco', 'PingFang SC', 'Helvetica Neue', sans-serif"
    const val ANDROID_TEXT_FONT = "Roboto, 'Noto Sans CJK SC', sans-serif"
}
