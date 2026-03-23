pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("androidLibs") {
            from(files("gradle/android-libs.versions.toml"))
        }
        create("serverLibs") {
            from(files("gradle/server-libs.versions.toml"))
        }
    }
}

rootProject.name = "KTelemetry"

include("core-model")
include("server-ktor")
include("android:android-library")
include("android:android-demo-app")

