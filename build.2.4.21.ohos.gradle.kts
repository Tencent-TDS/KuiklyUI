/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

plugins {
    kotlin("multiplatform") version "2.4.21-KBA-001" apply false
    kotlin("plugin.compose") version "2.4.21-KBA-001" apply false
    id("com.android.application") version "8.6.0" apply false
    id("com.android.library") version "8.6.0" apply false
    id("org.jetbrains.compose") version "1.7.3" apply false
    id("com.google.devtools.ksp") version "2.3.13" apply false
    id("com.tencent.kuiklybase.knoi.plugin") version("0.0.4") apply false
}

allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
        jvmTargetValidationMode.set(org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode.WARNING)
    }
    configurations.all {
        resolutionStrategy.dependencySubstitution {
            substitute(module("${MavenConfig.GROUP}:compose")).using(project(":compose"))
        }
    }
}
