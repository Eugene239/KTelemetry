import java.io.File
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    application
}

application {
    mainClass.set("io.epavlov.ktelemetry.backend.ApplicationKt")
}

kotlin {
    jvmToolchain(17)
}

fun gitShortRev(dir: File): String =
    try {
        val p =
            ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                .directory(dir)
                .redirectErrorStream(true)
                .start()
        p.inputStream.bufferedReader().readText().trim().ifEmpty { "unknown" }
    } catch (_: Exception) {
        "unknown"
    }

tasks.register("generateBuildInfo") {
    val out = layout.buildDirectory.dir("generated/resources/main")
    outputs.dir(out)
    doLast {
        val dir = out.get().asFile
        dir.mkdirs()
        var sha =
            (project.findProperty("buildInfoSha") as String?)
                ?: System.getenv("GIT_SHA")
                ?: gitShortRev(rootDir)
        if (sha.length > 7) sha = sha.take(7)
        val dateStr =
            (project.findProperty("buildInfoDate") as String?)
                ?: System.getenv("BUILD_DATE")
                ?: ZonedDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMddHH"))
        val version = "$dateStr-$sha"
        File(dir, "build-info.properties").writeText("version=$version\n")
    }
}

tasks.named("processResources") {
    dependsOn("generateBuildInfo")
}

sourceSets {
    main {
        resources {
            srcDir(layout.buildDirectory.dir("generated/resources/main"))
        }
    }
}

dependencies {
    implementation(project(":core-model"))

    implementation(serverLibs.bundles.ktor.server)
    implementation(serverLibs.bundles.ktor.client)

    implementation(serverLibs.logback.classic)
    implementation(libs.kotlinx.serialization.json)
    implementation(serverLibs.clickhouse.client.v2)

    testImplementation(kotlin("test"))
}

tasks.register<Jar>("fatJar") {
    archiveClassifier.set("all")
    manifest {
        attributes["Main-Class"] = "io.epavlov.ktelemetry.backend.ApplicationKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get() as CopySpec)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.build {
    dependsOn("fatJar")
}
