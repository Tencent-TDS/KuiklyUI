import org.gradle.kotlin.dsl.`kotlin-dsl`

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("org.json:json:20231013")
    testImplementation(kotlin("test"))
}

repositories {
    mavenCentral()
}