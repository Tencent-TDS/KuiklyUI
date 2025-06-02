# 1.记录原始url
ORIGIN_DISTRIBUTION_URL=$(grep "distributionUrl" gradle/wrapper/gradle-wrapper.properties | cut -d "=" -f 2)
echo "origin gradle url: $ORIGIN_DISTRIBUTION_URL"
# 2.切换gradle版本
NEW_DISTRIBUTION_URL="https\:\/\/services.gradle.org\/distributions\/gradle-7.5.1-bin.zip"
sed -i.bak "s/distributionUrl=.*$/distributionUrl=$NEW_DISTRIBUTION_URL/" gradle/wrapper/gradle-wrapper.properties

# 3.开始发布
KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="1.9.22" ./gradlew -c settings.1.9.22.gradle.kts :core-annotations:publishToMavenLocal --stacktrace
KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="1.9.22" ./gradlew -c settings.1.9.22.gradle.kts :core-ksp:publishToMavenLocal --stacktrace
KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="1.9.22" ./gradlew -c settings.1.9.22.gradle.kts :core:publishToMavenLocal --stacktrace
KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="1.9.22" ./gradlew -c settings.1.9.22.gradle.kts :core-render-android:publishToMavenLocal --stacktrace
KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="1.9.22" ./gradlew -c settings.1.9.22.gradle.kts :compose:publishToMavenLocal --stacktrace

# 4.还原文件
mv gradle/wrapper/gradle-wrapper.properties.bak gradle/wrapper/gradle-wrapper.properties