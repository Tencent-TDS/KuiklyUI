import org.gradle.kotlin.dsl.`kotlin-dsl`

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("org.json:json:20231013")
}

repositories {
    maven {
        url = uri("https://mirrors.tencent.com/nexus/repository/maven-public")
    }
    mavenCentral()
}