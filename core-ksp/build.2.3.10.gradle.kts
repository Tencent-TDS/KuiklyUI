// core-ksp/build.2.1.21.gradle.kts 的 Kotlin 2.3 兼容版本
// 改动：
//   1) kotlinOptions -> compilerOptions（Kotlin 2.3 硬错误）
//   2) symbol-processing-api: 2.0.21-1.0.27 -> 2.3.4（KSP 2.3）

plugins {
    kotlin("jvm")
    id("maven-publish")
    signing
}

group = MavenConfig.GROUP
version = Version.getCoreVersion()

publishing {
    repositories {
        val username = MavenConfig.getUsername(project)
        val password = MavenConfig.getPassword(project)
        if (username.isNotEmpty() && password.isNotEmpty()) {
            maven {
                credentials {
                    setUsername(username)
                    setPassword(password)
                }
                url = uri(MavenConfig.getRepoUrl(version as String))
            }
        } else {
            mavenLocal()
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }

    afterEvaluate {
        publications.withType<MavenPublication>().configureEach {
            pom.configureMavenCentralMetadata()
            signPublicationIfKeyPresent(project)
            artifact(emptyJavadocJar)
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        moduleName.set("${project.group}.${project.name}")
    }
}

dependencies {
    implementation(Dependencies.kotlinpoet)
    implementation("com.google.devtools.ksp:symbol-processing-api:2.3.4")
    implementation(project(":core-annotations"))
}

val emptyJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}
