/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension
import java.io.File
import java.io.FileInputStream
import java.util.Properties

/**
 * Created by kam on 2022/6/28.
 */
object Dependencies {
    val kotlinpoet by lazy {
        "com.squareup:kotlinpoet:${Version.KOTLINPOET_VERSION}"
    }

    val kspApi by lazy {
        "com.google.devtools.ksp:symbol-processing-api:${Version.getKSPVersion()}"
    }

    val androidxAppcompat by lazy {
        "androidx.appcompat:appcompat:${Version.ANDROIDX_APPCOMPAT_VERSION}"
    }

    val material by lazy {
        "com.google.android.material:material:${Version.MATERIAL_VERSION}"
    }

    val hippy by lazy {
        "com.tencent.hippy:hippy-common:2.14.1"
    }

    val androidXCoreKtx by lazy {
        "androidx.core:core-ktx:1.7.0"
    }

    val kspSymbolProcessingGradlePlugin by lazy {
        "com.google.devtools.ksp:symbol-processing-gradle-plugin:${Version.getKSPVersion()}"
    }
}

object BuildPlugin {
    val kotlin by lazy {
        "org.jetbrains.kotlin:kotlin-gradle-plugin:${Version.getKotlinVersion()}"
    }

    val android by lazy {
        "com.android.tools.build:gradle:${Version.getAGPVersion()}"
    }
}

object Publishing {
    const val kuiklyGroup = "com.tencent.kuikly"
}

object Output {
    const val name = "nativevue2"
    const val KEY_PACK_LOCAL_AAR_BUNDLE = "packLocalAarBundle"
}

object MavenConfig {
    const val GROUP = "com.tencent.kuikly-open"
    const val REPO_URL = "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2"
    const val SNAPSHOT_REPO_URL = "https://central.sonatype.com/repository/maven-snapshots/"
    const val RENDER_ANDROID_ARTIFACT_ID = "core-render-android"

    private const val KEY_USER_NAME = "username"
    private const val KEY_USER_PASSWORD = "password"

    fun getUsername(project: Project) : String {
        var username = ""
        if (project.hasProperty(KEY_USER_NAME)) {
            return project.property(KEY_USER_NAME) as String
        }
        if (username.isEmpty()) {
            val propertiesFile = File(project.rootDir, "local.properties")
            if (!propertiesFile.exists()) {
                return ""
            }
            val prop = Properties().apply {
                load(FileInputStream(propertiesFile))
            }
            username = prop.getProperty(KEY_USER_NAME) ?: ""
        }
        println("kuikly username is $username")
        return username
    }

    fun getPassword(project: Project) : String {
        var password = ""
        if (project.hasProperty(KEY_USER_PASSWORD)) {
            return project.property(KEY_USER_PASSWORD) as String
        }
        if (password.isEmpty()) {
            val propertiesFile = File(project.rootDir, "local.properties")
            if (!propertiesFile.exists()) {
                return ""
            }
            val prop = Properties().apply {
                load(FileInputStream(propertiesFile))
            }
            password = prop.getProperty(KEY_USER_PASSWORD) ?: ""
        }
        return password
    }

    fun getRepoUrl(version: String) : String {
        if (version.endsWith("-SNAPSHOT")) {
            return SNAPSHOT_REPO_URL
        }
        return REPO_URL
    }
}

object Version {

    const val KOTLINPOET_VERSION = "1.10.2"
    const val ANDROIDX_APPCOMPAT_VERSION = "1.3.1"
    const val MATERIAL_VERSION = "1.4.0"
    const val SNAPSHOT_SUFFIX = "-SNAPSHOT"

    private const val DEFAULT_KUIKLY_VERSION = "2.0.0"
    private const val DEFAULT_KOTLIN_VERSION = "2.0.21"
    private const val DEFAULT_AGP_VERSION = "7.4.2"

    private const val KEY_KUIKLY_VERSION = "KUIKLY_VERSION"
    private const val KEY_KOTLIN_VERSION = "KUIKLY_KOTLIN_VERSION"
    private const val KEY_AGP_VERSION = "KUIKLY_AGP_VERSION"
    private const val KEY_CI_BUILD_NUM = "KUIKLY_CI_BUILD_NUM"
    private const val KEY_RENDER_SUFFIX = "KUIKLY_RENDER_SUFFIX"
    private const val OHOS_KOTLIN_SUFFIX = "KBA-004"
    private const val KUIKLY_OHOS_KOTLIN_SUFFIX = "ohos"

    /**
     * 获取 Kuikly 版本号
     */
    fun getKuiklyVersion() : String {
        var kuiklyVersion = System.getenv(KEY_KUIKLY_VERSION)
        if (kuiklyVersion != null && kuiklyVersion.isNotEmpty()) {
            return kuiklyVersion
        }
        kuiklyVersion = DEFAULT_KUIKLY_VERSION
        // 流水席构建，版本号增加 snapshot
        val buildNum = System.getenv(KEY_CI_BUILD_NUM)
        if (buildNum != null && buildNum.isNotEmpty()) {
            kuiklyVersion += ".$buildNum"
        }
        return "$kuiklyVersion$SNAPSHOT_SUFFIX"
    }

    /**
     * 获取 kotlin 版本号
     */
    fun getKotlinVersion() : String {
        var kotlinVersion = System.getenv(KEY_KOTLIN_VERSION)
        if (kotlinVersion == null || kotlinVersion.isEmpty()) {
            kotlinVersion = DEFAULT_KOTLIN_VERSION
        }
        return kotlinVersion
    }

    /**
     * 获取 AGP 版本号
     */
    fun getAGPVersion() : String {
        var agpVersion = System.getenv(KEY_AGP_VERSION)
        if (agpVersion == null || agpVersion.isEmpty()) {
            agpVersion = DEFAULT_AGP_VERSION
        }
        return agpVersion
    }

    /**
     * 获取 render 后缀
     */
    fun getRenderSuffix() : String {
        var renderSuffix = System.getenv(KEY_RENDER_SUFFIX)
        if (renderSuffix == null || renderSuffix.isEmpty()) {
            renderSuffix = ""
        }
        return renderSuffix
    }

    /**
     * 获取 core 版本号，版本号规则：${shortVersion}-${kotlinVersion}
     * 适用于 core、core-ksp、core-annotation 三个库
     */
    fun getCoreVersion() : String{
        val kuiklyVersion = getKuiklyVersion()
        var coreVersion = "${getKuiklyVersion()}-${getKotlinVersion()}"
        if (kuiklyVersion.endsWith(SNAPSHOT_SUFFIX)) {
            // -SNAPSHOT 前插入 kotlin 版本号
            coreVersion = StringBuilder(kuiklyVersion).insert(
                kuiklyVersion.indexOf(SNAPSHOT_SUFFIX), "-${getKotlinVersion()}").toString()
        }
        if (coreVersion.contains(OHOS_KOTLIN_SUFFIX)) {
            coreVersion = coreVersion.replace(OHOS_KOTLIN_SUFFIX, KUIKLY_OHOS_KOTLIN_SUFFIX)
        }
        return coreVersion
    }

    /**
     * 获取 render 版本号
     */
    fun getRenderVersion() : String {
        val renderVersion = getCoreVersion()
        // 用于区分 androidx、support 版本
        val renderSuffix = getRenderSuffix()
        if (renderSuffix.isNotEmpty()) {
            return "${renderVersion}-${renderSuffix}"
        }
        return renderVersion
    }

    fun getKSPVersion() : String {
        return when (getKotlinVersion()) {
            "1.6.21" -> "1.6.21-1.0.6"
            "1.7.20" -> "1.7.20-1.0.7"
            "1.8.21" -> "1.8.21-1.0.11"
            "1.9.22" -> "1.9.22-1.0.16"
            "2.0.21" -> "2.0.21-1.0.27"
            "2.0.21-KBA-004" -> "2.0.21-1.0.27"
            else -> "${getKotlinVersion()}-1.0.7" // 默认版本
        }
    }

}

fun MavenPom.configureMavenCentralMetadata() {
    name.set("KuiklyUI")
    description.set("`Kuikly` is a comprehensive cross-platform solution for UI and logic based on Kotlin multi-platform. It was launched by Tencent's company-level Oteam in the front-end field. It aims to provide a `high-performance, full-platform development framework with unified codebase, ultimate ease of use, and dynamic flexibility`")
    url.set("https://github.com/Tencent-TDS/KuiklyUI")

    licenses {
        license {
            name.set("KuiklyUI")
            url.set("https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE")
        }
    }

    developers {
        developer {
            id.set("tds-Kuikly")
            name.set("tds-Kuikly Team")
        }
    }
    scm {
        url.set("https://github.com/Tencent-TDS/KuiklyUI")
    }
}

fun MavenPublication.signPublicationIfKeyPresent(project: Project) {
    val keyId = getSensitiveProperty(project, "signing.keyId")
    val secretKey = getSensitiveProperty(project, "signing.secretKey")
    val password = getSensitiveProperty(project, "signing.password")
    if (!secretKey.isNullOrBlank()) {
        project.extensions.configure<SigningExtension>("signing") {
            useInMemoryPgpKeys(keyId, secretKey, password)
            sign(this@signPublicationIfKeyPresent)
        }
    }
}

fun getSensitiveProperty(project: Project, name: String): String? {
    return project.findProperty(name) as? String ?: System.getenv(name)
}
