plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.ktlint)
    `maven-publish`
}

ktlint {
    android.set(true)
}

group = "io.epavlov.ktelemetry"
version = System.getenv("LIB_VERSION") ?: "0.0.1-SNAPSHOT"

android {
    namespace = "io.epavlov.ktelemetry.android"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)

    androidTarget {
        publishLibraryVariants("release")
    }

    sourceSets {
        androidMain.dependencies {
            implementation(project(":core-model"))
            implementation(androidLibs.core.ktx)
            implementation(androidLibs.kotlinx.coroutines.android)
            implementation(androidLibs.lifecycle.process)
            implementation(androidLibs.workmanager.runtime)
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/${System.getenv("GITHUB_REPOSITORY") ?: "eugene239/KTelemetry"}")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
    }
}
