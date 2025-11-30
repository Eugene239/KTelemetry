pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "KTelemetry"

include("core-model")
include("server-ktor")
include("android:android-library")
include("android:android-demo-app")

