package com.tencent.kuikly.demo.pages.app.theme

import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

data class ThemeColors (
    // 主题色
    val primary: Color,
    val background: Color,
    val backgroundElement: Color,

    // 顶部栏
    val topBarBackground: Color,
    val topBarTextUnfocused: Color,
    val topBarTextFocused: Color,
    val topBarIndicator: Color,
    val topBarNestedBackground: Color,
    val topBarNestedTextUnfocused: Color,
    val topBarNestedTextFocused: Color,

    // 底部栏
    val tabBarBackground: Color,
    val tabBarTextUnfocused: Color,
    val tabBarTextFocused: Color,
    val tabBarIconUnfocused: Color,
    val tabBarIconFocused: Color,

    val feedBackground: Color,

    // 作者信息
    val feedUserNameNormal: Color,
    val feedUserNameMember: Color,
    val feedUserSignature: Color,
    val feedUserDevice: Color,
    val feedUserFollowButton: Color,

    // 卡片内容
    val feedContentText: Color,
    val feedContentQuoteText: Color,
    val feedContentQuoteBackground: Color,
    val feedContentDivider: Color,

    // 卡片底部（转发、评论、点赞颜色）
    val feedBottomText: Color,
    val feedBottomIcon: Color
) {
    companion object {
        // 定义所有键名常量
        private const val KEY_PRIMARY = "primary"
        private const val KEY_BACKGROUND = "background"
        private const val KEY_BACKGROUND_ELEMENT = "backgroundElement"
        private const val KEY_TOP_BAR_BACKGROUND = "topBarBackground"
        private const val KEY_TOP_BAR_TEXT_UNFOCUSED = "topBarTextUnfocused"
        private const val KEY_TOP_BAR_TEXT_FOCUSED = "topBarTextFocused"
        private const val KEY_TOP_BAR_INDICATOR = "topBarIndicator"
        private const val KEY_TOP_BAR_NESTED_BACKGROUND = "topBarNestedBackground"
        private const val KEY_TOP_BAR_NESTED_TEXT_UNFOCUSED = "topBarNestedTextUnfocused"
        private const val KEY_TOP_BAR_NESTED_TEXT_FOCUSED = "topBarNestedTextFocused"
        private const val KEY_TAB_BAR_BACKGROUND = "tabBarBackground"
        private const val KEY_TAB_BAR_TEXT_UNFOCUSED = "tabBarTextUnfocused"
        private const val KEY_TAB_BAR_TEXT_FOCUSED = "tabBarTextFocused"
        private const val KEY_TAB_BAR_ICON_UNFOCUSED = "tabBarIconUnfocused"
        private const val KEY_TAB_BAR_ICON_FOCUSED = "tabBarIconFocused"
        private const val KEY_FEED_BACKGROUND = "feedBackground"
        private const val KEY_FEED_USER_NAME_NORMAL = "feedUserName"
        private const val KEY_FEED_USER_NAME_MEMBER = "feedUserNameMembership"
        private const val KEY_FEED_USER_SIGNATURE = "feedUserPostTime"
        private const val KEY_FEED_USER_DEVICE = "feedUserDevice"
        private const val KEY_FEED_USER_FOLLOW_BUTTON = "feedUserFollowButton"
        private const val KEY_FEED_CONTENT_TEXT = "feedContentText"
        private const val KEY_FEED_CONTENT_QUOTE_TEXT = "feedContentQuoteText"
        private const val KEY_FEED_CONTENT_QUOTE_BACKGROUND = "feedContentQuoteBackground"
        private const val KEY_FEED_CONTENT_DIVIDER = "feedContentDivider"
        private const val KEY_FEED_BOTTOM_TEXT = "feedBottomText"
        private const val KEY_FEED_BOTTOM_ICON = "feedBottomIcon"

        fun fromJson(json: JSONObject): ThemeColors {
            return ThemeColors(
                primary = Color(json.optString(KEY_PRIMARY)),
                background = Color(json.optString(KEY_BACKGROUND)),
                backgroundElement = Color(json.optString(KEY_BACKGROUND_ELEMENT)),
                topBarBackground = Color(json.optString(KEY_TOP_BAR_BACKGROUND)),
                topBarTextUnfocused = Color(json.optString(KEY_TOP_BAR_TEXT_UNFOCUSED)),
                topBarTextFocused = Color(json.optString(KEY_TOP_BAR_TEXT_FOCUSED)),
                topBarIndicator = Color(json.optString(KEY_TOP_BAR_INDICATOR)),
                topBarNestedBackground = Color(json.optString(KEY_TOP_BAR_NESTED_BACKGROUND)),
                topBarNestedTextUnfocused = Color(json.optString(KEY_TOP_BAR_NESTED_TEXT_UNFOCUSED)),
                topBarNestedTextFocused = Color(json.optString(KEY_TOP_BAR_NESTED_TEXT_FOCUSED)),
                tabBarBackground = Color(json.optString(KEY_TAB_BAR_BACKGROUND)),
                tabBarTextUnfocused = Color(json.optString(KEY_TAB_BAR_TEXT_UNFOCUSED)),
                tabBarTextFocused = Color(json.optString(KEY_TAB_BAR_TEXT_FOCUSED)),
                tabBarIconUnfocused = Color(json.optString(KEY_TAB_BAR_ICON_UNFOCUSED)),
                tabBarIconFocused = Color(json.optString(KEY_TAB_BAR_ICON_FOCUSED)),
                feedBackground = Color(json.optString(KEY_FEED_BACKGROUND)),
                feedUserNameNormal = Color(json.optString(KEY_FEED_USER_NAME_NORMAL)),
                feedUserNameMember = Color(json.optString(KEY_FEED_USER_NAME_MEMBER)),
                feedUserSignature = Color(json.optString(KEY_FEED_USER_SIGNATURE)),
                feedUserDevice = Color(json.optString(KEY_FEED_USER_DEVICE)),
                feedUserFollowButton = Color(json.optString(KEY_FEED_USER_FOLLOW_BUTTON)),
                feedContentText = Color(json.optString(KEY_FEED_CONTENT_TEXT)),
                feedContentQuoteText = Color(json.optString(KEY_FEED_CONTENT_QUOTE_TEXT)),
                feedContentQuoteBackground = Color(json.optString(KEY_FEED_CONTENT_QUOTE_BACKGROUND)),
                feedContentDivider = Color(json.optString(KEY_FEED_CONTENT_DIVIDER)),
                feedBottomText = Color(json.optString(KEY_FEED_BOTTOM_TEXT)),
                feedBottomIcon = Color(json.optString(KEY_FEED_BOTTOM_ICON))
            )
        }
    }
}

val lightColorScheme = ThemeColors(
    primary = Color.WHITE,
    background = Color(0xFFEFEFEF),
    backgroundElement = Color.BLACK,
    topBarBackground = Color(0xFFF9F9F9),
    topBarTextUnfocused = Color.GRAY,
    topBarTextFocused = Color.BLACK,
    topBarIndicator = Color.RED,
    topBarNestedBackground = Color.WHITE,
    topBarNestedTextUnfocused = Color.GRAY,
    topBarNestedTextFocused = Color.RED,
    tabBarBackground = Color(0xFFFAFAFA),
    tabBarTextUnfocused = Color.BLACK,
    tabBarTextFocused = Color.RED,
    tabBarIconUnfocused = Color.GRAY,
    tabBarIconFocused = Color.RED,
    feedBackground = Color.WHITE,
    feedUserNameNormal = Color.BLACK,
    feedUserNameMember = Color(0xFFF86119),
    feedUserSignature = Color(0xFF808080),
    feedUserDevice = Color(0xFF5B778D),
    feedUserFollowButton = Color(0xFFFB8C00),
    feedContentText = Color.BLACK,
    feedContentQuoteText = Color(0xFF5B778D),
    feedContentQuoteBackground = Color(0xFFF7F7F7),
    feedContentDivider = Color(0xFFDBDBDB),
    feedBottomText = Color.BLACK,
    feedBottomIcon = Color.GRAY
)

val darkColorScheme = ThemeColors(
    primary = Color(0xFF111318),
    background = Color(0xFF191C20),
    backgroundElement = Color(0xFFE2E2E9),
    topBarBackground = Color(0xFF111318),
    topBarTextUnfocused = Color(0xFFC4C6D0),
    topBarTextFocused = Color(0xFFAAC7FF),
    topBarIndicator = Color(0xFFAAC7FF),
    topBarNestedBackground = Color(0xFF111318),
    topBarNestedTextUnfocused = Color(0xFFC4C6D0),
    topBarNestedTextFocused = Color(0xFFAAC7FF),
    tabBarBackground = Color(0xFF111318),
    tabBarTextUnfocused = Color(0xFFC4C6D0),
    tabBarTextFocused = Color(0xFFAAC7FF),
    tabBarIconUnfocused = Color(0xFFC4C6D0),
    tabBarIconFocused = Color(0xFFAAC7FF),
    feedBackground = Color(0xFF111318),
    feedUserNameNormal = Color(0xFFE2E2E9),
    feedUserNameMember = Color(0xFFF86119),
    feedUserSignature = Color(0xFFC4C6D0),
    feedUserDevice = Color(0xFF5B778D),
    feedUserFollowButton = Color(0xFFFB8C00),
    feedContentText = Color(0xFFE2E2E9),
    feedContentQuoteText = Color(0xFF5B778D),
    feedContentQuoteBackground = Color(0xFF191C20),
    feedContentDivider = Color(0xFFC4C6D0),
    feedBottomText = Color(0xFFC4C6D0),
    feedBottomIcon = Color(0xFFC4C6D0)
)

val blueColorScheme = ThemeColors(
    primary = Color(0xFF415F91),
    background = Color(0xFFF3F3FA),
    backgroundElement = Color(0xFF191C20),
    topBarBackground = Color(0xFF415F91),
    topBarTextUnfocused = Color(0xFFD6E3FF),
    topBarTextFocused = Color(0xFFFFFFFF),
    topBarIndicator = Color(0xFFFFFFFF),
    topBarNestedBackground = Color(0xFF415F91),
    topBarNestedTextUnfocused = Color(0xFFD6E3FF),
    topBarNestedTextFocused = Color(0xFFFFFFFF),
    tabBarBackground = Color(0xFF415F91),
    tabBarTextUnfocused = Color(0xFFD6E3FF),
    tabBarTextFocused = Color(0xFFFFFFFF),
    tabBarIconUnfocused = Color(0xFFD6E3FF),
    tabBarIconFocused = Color(0xFFFFFFFF),
    feedBackground = Color(0xFFD6E3FF),
    feedUserNameNormal = Color(0xFF191C20),
    feedUserNameMember = Color(0xFFF86119),
    feedUserSignature = Color(0xFF44474E),
    feedUserDevice = Color(0xFF5B778D),
    feedUserFollowButton = Color(0xFFFB8C00),
    feedContentText = Color(0xFF191C20),
    feedContentQuoteText = Color(0xFF5B778D),
    feedContentQuoteBackground = Color(0xFFDAE2F9),
    feedContentDivider = Color(0xFF284777),
    feedBottomText = Color(0xFF284777),
    feedBottomIcon = Color(0xFF284777)
)
