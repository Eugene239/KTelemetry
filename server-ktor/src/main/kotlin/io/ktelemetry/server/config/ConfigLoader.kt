package io.ktelemetry.server.config

import org.slf4j.LoggerFactory
import java.io.File
import java.util.Properties

object ConfigLoader {
    private val logger = LoggerFactory.getLogger(ConfigLoader::class.java)

    fun loadLocalProperties(): Properties {
        val properties = Properties()
        val localPropertiesFile = findLocalPropertiesFile()

        if (localPropertiesFile != null && localPropertiesFile.exists()) {
            try {
                localPropertiesFile.inputStream().use { input ->
                    properties.load(input)
                }
                logger.info("Loaded configuration from ${localPropertiesFile.absolutePath}")
            } catch (e: Exception) {
                logger.warn("Failed to load local.properties: ${e.message}")
            }
        } else {
            logger.debug("local.properties not found, using environment variables")
        }

        return properties
    }

    private fun findLocalPropertiesFile(): File? {
        val currentDir = File(System.getProperty("user.dir"))
        val currentFile = File(currentDir, "local.properties")
        if (currentFile.exists()) return currentFile

        val parentFile = File(currentDir.parent, "local.properties")
        if (parentFile.exists()) return parentFile

        return null
    }

    fun getRequiredProperty(properties: Properties, key: String, envKey: String): String {
        val value = properties.getProperty(key) ?: System.getenv(envKey)
        if (value == null || value.isBlank()) {
            throw IllegalStateException(
                "Required configuration property '$key' (or environment variable '$envKey') is not set. " +
                        "Please set it in local.properties or as an environment variable."
            )
        }
        return value
    }

    fun getRequiredIntProperty(properties: Properties, key: String, envKey: String): Int {
        val value = getRequiredProperty(properties, key, envKey)
        return value.toIntOrNull()
            ?: throw IllegalStateException(
                "Configuration property '$key' (or environment variable '$envKey') must be a valid integer, got: '$value'"
            )
    }
}

