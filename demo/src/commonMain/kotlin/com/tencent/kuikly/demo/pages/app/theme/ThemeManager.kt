package com.tencent.kuikly.demo.pages.app.theme

import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

object ThemeManager {

    // 主题配置
    var colorScheme: ThemeColors = lightColorScheme
    var assetScheme: String = "default"
    var typoScheme: ThemeTypography = defaultTypography
    private var assetJSON = JSONObject()

    // 主题常量
    private const val ASSET_PATH_PREFIX = "themes/"
    val COLOR_SCHEME_MAP = mapOf(
        "light" to lightColorScheme,
        "dark" to darkColorScheme,
        "blue" to blueColorScheme,
    )
    val TYPO_SCHEME_MAP = mapOf("default" to defaultTypography)
    val ASSET_SCHEME_LIST = listOf("default", "game")
    const val SKIN_CHANGED_EVENT = "skinChanged"

    enum class ThemeType { COLOR, ASSET, TYPOGRAPHY }

    // 公共API

    fun changeColorScheme(theme: String) {
        changeTheme(ThemeType.COLOR, theme)
    }

    fun changeAssetScheme(theme: String) {
        changeTheme(ThemeType.ASSET, theme)
    }

    fun changeTypoScheme(theme: String) {
        changeTheme(ThemeType.TYPOGRAPHY, theme)
    }

    fun loadColorFromJson(json: JSONObject) {
        colorScheme = ThemeColors.fromJson(json)
    }

    fun getAssetUri(theme: String = assetScheme, asset: String): ImageUri {
        return ImageUri.commonAssets("$ASSET_PATH_PREFIX$theme/$asset")
    }

    fun getAssetUrl(theme: String = assetScheme, asset: String): String {
        return assetJSON.optString("")
    }

    private fun changeTheme(type: ThemeType, theme: String) {
        when (type) {
            ThemeType.COLOR -> {
                colorScheme = COLOR_SCHEME_MAP[theme] ?: run {
                    // TODO: 业务自定义主题加载（如远程下载）
                    ThemeColors.fromJson(JSONObject())
                }
            }
            ThemeType.ASSET -> {
                assetScheme = theme
                if (theme !in ASSET_SCHEME_LIST) {
                    // TODO: 业务自定义资源加载（如远程下载）
                    loadAssetFromJson(JSONObject())
                }
            }
            ThemeType.TYPOGRAPHY -> {
                // TODO: 业务自定义排版加载（如远程下载）
                typoScheme = TYPO_SCHEME_MAP[theme] ?: run {
                    ThemeTypography.fromJson(JSONObject())
                }
            }
        }
    }

    private fun loadAssetFromJson(json: JSONObject) {
        assetJSON = json
    }

}