import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}


val localPropsFile = rootProject.file("local.properties")
val localProps = Properties().apply {
    if (localPropsFile.exists()) load(localPropsFile.inputStream())
}
val backendUrl = localProps.getProperty("backend.url")
    ?: throw IllegalStateException("Property 'backend.url' not found in local.properties")

android {
    namespace = "io.ktelemetry.android.demo"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.ktelemetry.android.demo"
        minSdk = 21
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "KTELEMETRY_SERVER_URL", "\"$backendUrl\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        buildConfig = true
    }

}

dependencies {
    implementation(project(":android:android-library"))
    implementation(project(":core-model"))
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation(libs.kotlinx.serialization.json)
}

