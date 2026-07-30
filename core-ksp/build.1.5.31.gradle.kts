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
    kotlinOptions {
        moduleName = "${project.group}.${project.name}"
    }
}

dependencies {
    implementation(Dependencies.kotlinpoet)
    implementation("com.google.devtools.ksp:symbol-processing-api:1.5.31-1.0.0")
    implementation(project(":core-annotations"))
}

val emptyJavadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}
