object Version {

    private const val KUIKLY_VERSION = "2.7.0"
    private const val KOTLIN_VERSION = "2.1.21"
    private const val KOTLIN_OHOS_VERSION = "2.0.21-ohos"

    /**
     * 获取 Kuikly 版本号，版本号规则：${shortVersion}-${kotlinVersion}
     * 适用于 core、core-ksp、core-annotation、core-render-android
     */
    fun getKuiklyVersion(): String {
        return "$KUIKLY_VERSION-$KOTLIN_VERSION"
    }

    /**
     * 获取 Kuikly Ohos版本号
     */
    fun getKuiklyOhosVersion(): String {
        return "$KUIKLY_VERSION-$KOTLIN_OHOS_VERSION"
    }
}

object BuildPlugin {
    val kuikly by lazy {
        "com.tencent.kuikly-open:core-gradle-plugin:${Version.getKuiklyVersion()}"
    }
}