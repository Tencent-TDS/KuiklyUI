# 1.记录原始url
ORIGIN_DISTRIBUTION_URL=$(grep "distributionUrl" gradle/wrapper/gradle-wrapper.properties | cut -d "=" -f 2)
echo "origin gradle url: $ORIGIN_DISTRIBUTION_URL"
# 2.切换gradle版本
NEW_DISTRIBUTION_URL="https\:\/\/services.gradle.org\/distributions\/gradle-8.0-bin.zip"
sed -i.bak "s/distributionUrl=.*$/distributionUrl=$NEW_DISTRIBUTION_URL/" gradle/wrapper/gradle-wrapper.properties
echo "new gradle url: " $(grep "distributionUrl" gradle/wrapper/gradle-wrapper.properties | cut -d "=" -f 2)

# 3.开始发布
KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="2.0.21-KBA-004" ./gradlew -c settings.2.0.ohos.gradle.kts :core-annotations:publishToMavenLocal --stacktrace
KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="2.0.21-KBA-004" ./gradlew -c settings.2.0.ohos.gradle.kts :core:publishToMavenLocal --stacktrace
KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="2.0.21-KBA-004" ./gradlew -c settings.2.0.ohos.gradle.kts :core-ksp:publishToMavenLocal --stacktrace
KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="2.0.21-KBA-004" ./gradlew -c settings.2.0.ohos.gradle.kts :core-render-android:publishToMavenLocal --stacktrace
KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="2.0.21-KBA-004" ./gradlew -c settings.2.0.ohos.gradle.kts :compose:publishToMavenLocal --stacktrace
KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="2.0.21-KBA-004" ./gradlew -c settings.2.0.ohos.gradle.kts :demo:linkSharedReleaseSharedOhosArm64  --stacktrace


# 4.还原文件
mv gradle/wrapper/gradle-wrapper.properties.bak gradle/wrapper/gradle-wrapper.properties
mv gradle.properties.bak gradle.properties


# 5.拷贝so
echo "Copying artifact files:"
OHOS_RENDER_PROJECT_DIR=./ohosApp

TARGET_SO_PATH=$PWD/demo/build/bin/ohosArm64/sharedReleaseShared/libshared.so
OHO_SO_PROJECT_PATH=$OHOS_RENDER_PROJECT_DIR/entry/libs/arm64-v8a
cp $TARGET_SO_PATH $OHO_SO_PROJECT_PATH
echo "libshared.so: copied from $TARGET_SO_PATH to ohos demo directory: $OHO_SO_PROJECT_PATH"

TARGET_SO_HEADER_PATH=$PWD/demo/build/bin/ohosArm64/sharedReleaseShared/libshared_api.h
OHO_SO_HEADER_PATH=$OHOS_RENDER_PROJECT_DIR/entry/src/main/cpp/thirdparty/biz_entry
cp $TARGET_SO_HEADER_PATH $OHO_SO_HEADER_PATH
echo "libshared_api.h: copied from $TARGET_SO_HEADER_PATH to ohos demo directory: $OHO_SO_HEADER_PATH"
echo "Copy ops done!"