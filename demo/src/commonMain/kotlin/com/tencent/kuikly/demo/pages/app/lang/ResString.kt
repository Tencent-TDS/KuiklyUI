package com.tencent.kuikly.demo.pages.app.lang

import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

data class ResStrings (
    val language: String,

    // Top navigation bar
    val topBarFollow: String,
    val topBarTrend: String,
    val topBarRecommend: String,
    val topBarNearby: String,
    val topBarRanking: String,
    val topBarCelebrity: String,
    val topBarEntertain: String,
    val topBarSociety: String,
    val topBarTest: String,

    // Bottom navigation bar
    val btmBarHome: String,
    val btmBarVideo: String,
    val btmBarDiscover: String,
    val btmBarMessage: String,
    val btmBarMe: String,

    // Loading States
    val loading: String,
    val refreshing: String,
    val refreshDone: String,
    val pullToRefresh: String,
    val releaseToRefresh: String,
    val loadMore: String,
    val noMoreData: String,
    val tapToRetry: String,
    val loadingFailed: String,

    // Setting page
    val setting: String,
    val chosen: String,
    val themeHint: String,
    val languageHint: String,
) {
    companion object {
        private const val KEY_LANGUAGE = "language"
        private const val KEY_TOP_BAR_FOLLOW = "topBarFollow"
        private const val KEY_TOP_BAR_TREND = "topBarTrend"
        private const val KEY_TOP_BAR_RECOMMEND = "topBarRecommend"
        private const val KEY_TOP_BAR_NEARBY = "topBarNearby"
        private const val KEY_TOP_BAR_RANKING = "topBarRanking"
        private const val KEY_TOP_BAR_CELEBRITY = "topBarCelebrity"
        private const val KEY_TOP_BAR_ENTERTAIN = "topBarEntertain"
        private const val KEY_TOP_BAR_SOCIETY = "topBarSociety"
        private const val KEY_TOP_BAR_TEST = "topBarTest"
        private const val KEY_BTM_BAR_HOME = "btmBarHome"
        private const val KEY_BTM_BAR_VIDEO = "btmBarVideo"
        private const val KEY_BTM_BAR_DISCOVER = "btmBarDiscover"
        private const val KEY_BTM_BAR_MESSAGE = "btmBarMessage"
        private const val KEY_BTM_BAR_ME = "btmBarMe"
        private const val KEY_LOADING = "loading"
        private const val KEY_REFRESHING = "refreshing"
        private const val KEY_REFRESH_DONE = "refreshDone"
        private const val KEY_PULL_TO_REFRESH = "pullToRefresh"
        private const val KEY_RELEASE_TO_REFRESH = "releaseToRefresh"
        private const val KEY_LOAD_MORE = "loadMore"
        private const val KEY_NO_MORE_DATA = "noMoreData"
        private const val KEY_TAP_TO_RETRY = "tapToRetry"
        private const val KEY_LOADING_FAILED = "loadingFailed"
        private const val KEY_SETTING = "setting"
        private const val KEY_CHOSEN = "chosen"
        private const val KEY_THEME_HINT = "themeHint"
        private const val KEY_LANGUAGE_HINT = "languageHint"

        fun fromJson(json: JSONObject): ResStrings {
            return ResStrings(
                language = json.optString(KEY_LANGUAGE),
                topBarFollow = json.optString(KEY_TOP_BAR_FOLLOW),
                topBarTrend = json.optString(KEY_TOP_BAR_TREND),
                topBarRecommend = json.optString(KEY_TOP_BAR_RECOMMEND),
                topBarNearby = json.optString(KEY_TOP_BAR_NEARBY),
                topBarRanking = json.optString(KEY_TOP_BAR_RANKING),
                topBarCelebrity = json.optString(KEY_TOP_BAR_CELEBRITY),
                topBarEntertain = json.optString(KEY_TOP_BAR_ENTERTAIN),
                topBarSociety = json.optString(KEY_TOP_BAR_SOCIETY),
                topBarTest = json.optString(KEY_TOP_BAR_TEST),
                btmBarHome = json.optString(KEY_BTM_BAR_HOME),
                btmBarVideo = json.optString(KEY_BTM_BAR_VIDEO),
                btmBarDiscover = json.optString(KEY_BTM_BAR_DISCOVER),
                btmBarMessage = json.optString(KEY_BTM_BAR_MESSAGE),
                btmBarMe = json.optString(KEY_BTM_BAR_ME),
                loading = json.optString(KEY_LOADING),
                refreshing = json.optString(KEY_REFRESHING),
                refreshDone = json.optString(KEY_REFRESH_DONE),
                pullToRefresh = json.optString(KEY_PULL_TO_REFRESH),
                releaseToRefresh = json.optString(KEY_RELEASE_TO_REFRESH),
                loadMore = json.optString(KEY_LOAD_MORE),
                noMoreData = json.optString(KEY_NO_MORE_DATA),
                tapToRetry = json.optString(KEY_TAP_TO_RETRY),
                loadingFailed = json.optString(KEY_LOADING_FAILED),
                setting = json.optString(KEY_SETTING),
                chosen = json.optString(KEY_CHOSEN),
                themeHint = json.optString(KEY_THEME_HINT),
                languageHint = json.optString(KEY_LANGUAGE_HINT)
            )
        }
    }
}

val SimplifiedChinese = ResStrings(
    language = "zh-Hans",
    topBarFollow = "关注",
    topBarTrend = "热门",
    topBarRecommend = "推荐",
    topBarNearby = "附近",
    topBarRanking = "榜单",
    topBarCelebrity = "明星",
    topBarEntertain = "搞笑",
    topBarSociety = "社会",
    topBarTest = "测试",
    btmBarHome = "首页",
    btmBarVideo = "视频",
    btmBarDiscover = "发现",
    btmBarMessage = "消息",
    btmBarMe = "我",
    loading = "加载中...",
    refreshing = "正在刷新",
    refreshDone = "刷新成功",
    pullToRefresh = "下拉刷新",
    releaseToRefresh = "松手即可刷新",
    loadMore = "加载更多",
    noMoreData = "无更多数据",
    tapToRetry = "点击重试",
    loadingFailed = "加载失败",
    setting = "设置",
    chosen = "已选中",
    themeHint = "选择资源主题",
    languageHint = "请选择语言"
)

val TraditionalChinese = ResStrings(
    language = "zh-Hant",
    topBarFollow = "關注",
    topBarTrend = "熱門",
    topBarRecommend = "推薦",
    topBarNearby = "附近",
    topBarRanking = "榜單",
    topBarCelebrity = "明星",
    topBarEntertain = "搞笑",
    topBarSociety = "社會",
    topBarTest = "測試",
    btmBarHome = "首頁",
    btmBarVideo = "視頻",
    btmBarDiscover = "發現",
    btmBarMessage = "消息",
    btmBarMe = "我",
    loading = "加載中...",
    refreshing = "正在刷新",
    refreshDone = "刷新成功",
    pullToRefresh = "下拉刷新",
    releaseToRefresh = "松手即可刷新",
    loadMore = "加載更多",
    noMoreData = "無更多數據",
    tapToRetry = "點擊重試",
    loadingFailed = "加載失敗",
    setting = "設置",
    chosen = "已選中",
    themeHint = "選擇資源主題",
    languageHint = "請選擇語言"
)

val enJsonString = """
    {
      "language": "en",
      "topBarFollow": "Following",
      "topBarTrend": "Trending",
      "topBarRecommend": "Recommend",
      "topBarNearby": "Nearby",
      "topBarRanking": "Ranking",
      "topBarCelebrity": "Celebrity",
      "topBarEntertain": "Entertainment",
      "topBarSociety": "Society",
      "topBarTest": "Test",
      "btmBarHome": "Home",
      "btmBarVideo": "Video",
      "btmBarDiscover": "Discover",
      "btmBarMessage": "Messages",
      "btmBarMe": "Me",
      "loading": "Loading...",
      "refreshing": "Refreshing",
      "refreshDone": "Refresh successful",
      "pullToRefresh": "Pull to refresh",
      "releaseToRefresh": "Release to refresh",
      "loadMore": "Load more",
      "noMoreData": "No more data",
      "tapToRetry": "Tap to retry",
      "loadingFailed": "Loading failed",
      "setting": "Settings",
      "chosen": "Selected",
      "themeHint": "Select theme",
      "languageHint": "Please select language"
    }
""".trimIndent()

val deJsonString = """
    {
      "language": "de",
      "topBarFollow": "Folgen",
      "topBarTrend": "Trends",
      "topBarRecommend": "Empfehlungen",
      "topBarNearby": "In der Nähe",
      "topBarRanking": "Rangliste",
      "topBarCelebrity": "Prominente",
      "topBarEntertain": "Unterhaltung",
      "topBarSociety": "Gesellschaft",
      "topBarTest": "Test",
      "btmBarHome": "Startseite",
      "btmBarVideo": "Videos",
      "btmBarDiscover": "Entdecken",
      "btmBarMessage": "Nachrichten",
      "btmBarMe": "Ich",
      "loading": "Wird geladen...",
      "refreshing": "Aktualisierung",
      "refreshDone": "Aktualisierung erfolgreich",
      "pullToRefresh": "Zum Aktualisieren ziehen",
      "releaseToRefresh": "Zum Aktualisieren loslassen",
      "loadMore": "Mehr laden",
      "noMoreData": "Keine weiteren Daten",
      "tapToRetry": "Zum Wiederholen tippen",
      "loadingFailed": "Laden fehlgeschlagen",
      "setting": "Einstellungen",
      "chosen": "Ausgewählt",
      "themeHint": "Thema auswählen",
      "languageHint": "Sprache auswählen"
    }
""".trimIndent()
