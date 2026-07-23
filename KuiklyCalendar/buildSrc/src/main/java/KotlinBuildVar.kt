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

object Version {
    private const val KUIKLY_VERSION = "2.15.0"
    private const val KOTLIN_VERSION = "2.1.21"
    private const val AGP_VERSION = "7.4.2"
    private const val KSP_VERSION = "2.1.21-2.0.1"

    fun getKuiklyVersion(): String = "$KUIKLY_VERSION-$KOTLIN_VERSION"
    fun getKotlinVersion(): String = KOTLIN_VERSION
    fun getAGPVersion(): String = AGP_VERSION
    fun getKSPVersion(): String = KSP_VERSION
}

object BuildPlugin {
    val kotlin by lazy {
        "org.jetbrains.kotlin:kotlin-gradle-plugin:${Version.getKotlinVersion()}"
    }
    val android by lazy {
        "com.android.tools.build:gradle:${Version.getAGPVersion()}"
    }
    val kuikly by lazy {
        "com.tencent.kuikly-open:core-gradle-plugin:${Version.getKuiklyVersion()}"
    }
}

object Dependencies {
    val material by lazy { "com.google.android.material:material:1.4.0" }
    val androidxAppcompat by lazy { "androidx.appcompat:appcompat:1.3.1" }
    val androidXCoreKtx by lazy { "androidx.core:core-ktx:1.7.0" }
}

object Publishing {
    const val kuiklyGroup = "com.tencent.kuikly.calendar"
}
