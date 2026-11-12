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
