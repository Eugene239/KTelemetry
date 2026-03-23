import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(androidLibs.plugins.compose.compiler)
    alias(libs.plugins.ktlint)
}

ktlint {
    android.set(true)
}

val localPropsFile = rootProject.file("local.properties")
val localProps =
    Properties().apply {
        if (localPropsFile.exists()) load(localPropsFile.inputStream())
    }
val backendUrl = localProps.getProperty("backend.url") ?: "http://localhost:8080"
val backendApiKey = localProps.getProperty("backend.apiKey") ?: ""

android {
    namespace = "io.epavlov.ktelemetry.android.demo"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.epavlov.ktelemetry.android.demo"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "KTELEMETRY_SERVER_URL", "\"$backendUrl\"")
        buildConfigField("String", "KTELEMETRY_API_KEY", "\"$backendApiKey\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":android:android-library"))
    implementation(project(":core-model"))

    implementation(platform(androidLibs.compose.bom))
    implementation(androidLibs.bundles.compose)
}
