# 1.记录原始url
ORIGIN_DISTRIBUTION_URL=$(grep "distributionUrl" gradle/wrapper/gradle-wrapper.properties | cut -d "=" -f 2)
echo "origin gradle url: $ORIGIN_DISTRIBUTION_URL"
# 2.切换gradle版本
NEW_DISTRIBUTION_URL="https\:\/\/services.gradle.org\/distributions\/gradle-7.5.1-bin.zip"
sed -i.bak "s/distributionUrl=.*$/distributionUrl=$NEW_DISTRIBUTION_URL/" gradle/wrapper/gradle-wrapper.properties

# 3.语法兼容修改
current_dir=$PWD
core_convert_util_file=$current_dir/core/src/commonMain/kotlin/com/tencent/kuikly/core/utils/ConvertUtil.kt
core_pager_manager=$current_dir/core/src/commonMain/kotlin/com/tencent/kuikly/core/manager/PagerManager.kt

## 替换 lowercase → toLowerCase
echo "$core_convert_util_file"
sed -i.bak 's/lowercase/toLowerCase/g' "$core_convert_util_file"
echo "$core_pager_manager"
sed -i.bak 's/lowercase/toLowerCase/g' "$core_pager_manager"

# 4.开始发布
KUIKLY_AGP_VERSION="4.2.1" KUIKLY_KOTLIN_VERSION="1.4.20" ./gradlew -c settings.1.4.20.gradle.kts :core-annotations:publishToMavenLocal --stacktrace
KUIKLY_AGP_VERSION="4.2.1" KUIKLY_KOTLIN_VERSION="1.4.20" ./gradlew -c settings.1.4.20.gradle.kts :core-kapt:publishToMavenLocal --stacktrace
KUIKLY_AGP_VERSION="4.2.1" KUIKLY_KOTLIN_VERSION="1.4.20" ./gradlew -c settings.1.4.20.gradle.kts :core:publishToMavenLocal --stacktrace
KUIKLY_AGP_VERSION="4.2.1" KUIKLY_KOTLIN_VERSION="1.4.20" ./gradlew -c settings.1.4.20.gradle.kts :core-render-android:publishToMavenLocal --stacktrace

## 5.还原文件
mv gradle/wrapper/gradle-wrapper.properties.bak gradle/wrapper/gradle-wrapper.properties
mv "$core_convert_util_file.bak" "$core_convert_util_file"
mv "$core_pager_manager.bak" "$core_pager_manager"