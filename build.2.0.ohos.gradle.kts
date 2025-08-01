plugins {
    kotlin("multiplatform") version "2.0.21-KBA-004" apply false
    kotlin("plugin.compose") version "2.0.21-KBA-004" apply false
    id("com.android.application") version "7.4.2" apply false
    id("com.android.library") version "7.4.2" apply false
    id("org.jetbrains.compose") version "1.7.3" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
}

allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
        jvmTargetValidationMode.set(org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode.WARNING)
    }
    configurations.all {
        resolutionStrategy.dependencySubstitution {
            substitute(module("${MavenConfig.GROUP}:core")).using(project(":core"))
            substitute(module("${MavenConfig.GROUP}:core-annotations")).using(project(":core-annotations"))
            substitute(module("${MavenConfig.GROUP}:compose")).using(project(":compose"))
        }
    }
}