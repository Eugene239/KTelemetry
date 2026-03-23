package io.epavlov.ktelemetry.backend.config

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

    fun getProperty(
        properties: Properties,
        key: String,
        envKey: String,
        default: String,
    ): String {
        return properties.getProperty(key) ?: System.getenv(envKey) ?: default
    }

    fun getIntProperty(
        properties: Properties,
        key: String,
        envKey: String,
        default: Int,
    ): Int {
        val raw = properties.getProperty(key) ?: System.getenv(envKey) ?: return default
        return raw.toIntOrNull()
            ?: throw IllegalStateException("Property '$key' / env '$envKey' must be an integer, got: '$raw'")
    }

    fun getRequiredProperty(
        properties: Properties,
        key: String,
        envKey: String,
    ): String {
        val value = properties.getProperty(key) ?: System.getenv(envKey)
        if (value == null || value.isBlank()) {
            throw IllegalStateException(
                "Required property '$key' (or env '$envKey') is not set.\n" +
                    "Copy local.properties.example → local.properties and fill in the values.",
            )
        }
        return value
    }
}
