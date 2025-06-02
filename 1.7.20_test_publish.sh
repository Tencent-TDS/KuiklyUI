# 1.记录原始url
ORIGIN_DISTRIBUTION_URL=$(grep "distributionUrl" gradle/wrapper/gradle-wrapper.properties | cut -d "=" -f 2)
echo "origin gradle url: $ORIGIN_DISTRIBUTION_URL"
# 2.切换gradle版本
NEW_DISTRIBUTION_URL="https\:\/\/services.gradle.org\/distributions\/gradle-7.5.1-bin.zip"
sed -i.bak "s/distributionUrl=.*$/distributionUrl=$NEW_DISTRIBUTION_URL/" gradle/wrapper/gradle-wrapper.properties

# 3.语法兼容修改
ios_main_dir="core/src/iosMain/kotlin/com/tencent/kuikly"

ios_platform_impl="$ios_main_dir/core/module/PlatformImp.kt"
sed -i.bak '/@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)/d' "$ios_platform_impl"

ios_exception_tracker="$ios_main_dir/core/exception/ExceptionTracker.kt"
sed -i.bak \
    -e '/@file:OptIn(kotlin\.experimental\.ExperimentalNativeApi::class)/d' \
    -e 's/import kotlin\.concurrent\.AtomicReference/import kotlin.native.concurrent.AtomicReference/g' \
    "$ios_exception_tracker"


KUIKLY_KOTLIN_VERSION="1.7.20" ./gradlew -c settings.1.7.20.gradle.kts :core:publishToMavenLocal
KUIKLY_KOTLIN_VERSION="1.7.20" ./gradlew -c settings.1.7.20.gradle.kts :core-annotations:publishToMavenLocal
KUIKLY_KOTLIN_VERSION="1.7.20" ./gradlew -c settings.1.7.20.gradle.kts :core-ksp:publishToMavenLocal
KUIKLY_KOTLIN_VERSION="1.7.20" ./gradlew -c settings.1.7.20.gradle.kts :core-render-android:publishToMavenLocal

# 5.还原文件
mv gradle/wrapper/gradle-wrapper.properties.bak gradle/wrapper/gradle-wrapper.properties
mv "$ios_platform_impl.bak" "$ios_platform_impl"
mv "$ios_exception_tracker.bak" "$ios_exception_tracker"