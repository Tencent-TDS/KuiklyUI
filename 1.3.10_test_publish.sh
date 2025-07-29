# 1.记录原始url
ORIGIN_DISTRIBUTION_URL=$(grep "distributionUrl" gradle/wrapper/gradle-wrapper.properties | cut -d "=" -f 2)
echo "origin gradle url: $ORIGIN_DISTRIBUTION_URL"
# 2.切换gradle版本
NEW_DISTRIBUTION_URL="https\:\/\/services.gradle.org\/distributions\/gradle-5.4.1-bin.zip"
sed -i.bak "s/distributionUrl=.*$/distributionUrl=$NEW_DISTRIBUTION_URL/" gradle/wrapper/gradle-wrapper.properties

# 是否兼容 support
KUIKLY_ENABLE_ANDROID_SUPPORT_COMPATIBLE=0

current_dir=$PWD
core_render_android_dir=$current_dir/core-render-android/src/main/java
core_convert_util_file=$current_dir/core/src/commonMain/kotlin/com/tencent/kuikly/core/utils/ConvertUtil.kt
core_pager_manager=$current_dir/core/src/commonMain/kotlin/com/tencent/kuikly/core/manager/PagerManager.kt
kuikly_kotlin_build_var=$current_dir/buildSrc/src/main/java/KuiklyKotlinBuildVar.kt

# 关闭androidx开关、将androidx包名替换成support包包名
if [ "$KUIKLY_ENABLE_ANDROID_SUPPORT_COMPATIBLE" -eq 1 ]; then
  # 修改 gradle.properties，关闭 androidx
  sed -i.bak -e "s/android.useAndroidX=true/android.useAndroidX=false/g" -e "s/android.enableJetifier=true/android.enableJetifier=false/g" gradle.properties

  # 替换所有 androidx
  echo $core_render_android_dir
  for file in $(find $core_render_android_dir -type f -name "*.kt")
  do
      sed -i -depth -e 's/import androidx.recyclerview\./import android.support.v7\./g' -e 's/import androidx.dynamicanimation\./import android.support\./g' -e 's/import androidx\./import android.support\./g' "$file"
  done

fi

# ConvertUtil的encodeToByteArray替换成toByteArray、lowercase → toLowerCase
echo $core_convert_util_file
sed -i.bak -e 's/md5L16\.encodeToByteArray()/md5L16\.toByteArray(Charsets.UTF_8)/g' \
           -e 's/lowercase/toLowerCase/g' $core_convert_util_file

# PagerManager的lowercase替换成toLowerCase
echo "$core_pager_manager"
sed -i.bak 's/lowercase/toLowerCase/g' "$core_pager_manager"

# buildSrc替换useInMemoryPgpKeys方法
sed -i.bak 's/useInMemoryPgpKeys(keyId, secretKey, password)/useInMemoryPgpKeys(secretKey, password)/g' "$kuikly_kotlin_build_var"

# 构建
KUIKLY_AGP_VERSION="3.5.4" KUIKLY_KOTLIN_VERSION="1.3.10" ./gradlew -c settings.1.3.10.gradle.kts :core-annotations:publishToMavenLocal --stacktrace
KUIKLY_AGP_VERSION="3.5.4" KUIKLY_KOTLIN_VERSION="1.3.10" ./gradlew -c settings.1.3.10.gradle.kts :core-kapt:publishToMavenLocal --stacktrace
KUIKLY_AGP_VERSION="3.5.4" KUIKLY_KOTLIN_VERSION="1.3.10" ./gradlew -c settings.1.3.10.gradle.kts :core:publishToMavenLocal --stacktrace
KUIKLY_AGP_VERSION="3.5.4" KUIKLY_KOTLIN_VERSION="1.3.10" ./gradlew -c settings.1.3.10.gradle.kts :core-render-android:publishToMavenLocal --stacktrace
KUIKLY_AGP_VERSION="3.5.4" KUIKLY_KOTLIN_VERSION="1.3.10" KUIKLY_RENDER_SUFFIX="androidx" ./gradlew -c settings.1.3.10.gradle.kts :core-render-android:publishToMavenLocal --stacktrace

# 还原androidx
if [ "$KUIKLY_ENABLE_ANDROID_SUPPORT_COMPATIBLE" -eq 1 ]; then
  # 修改 gradle.properties
  mv gradle.properties.bak gradle.properties
  # 恢复 androidx
  for file in $(find $core_render_android_dir -type f -name "*.kt")
  do
      mv "$file-depth" "$file"
  done
fi

# 还原其他文件
mv gradle/wrapper/gradle-wrapper.properties.bak gradle/wrapper/gradle-wrapper.properties
mv "$core_convert_util_file.bak" $core_convert_util_file
mv "$core_pager_manager.bak" "$core_pager_manager"
mv "$kuikly_kotlin_build_var.bak" $kuikly_kotlin_build_var