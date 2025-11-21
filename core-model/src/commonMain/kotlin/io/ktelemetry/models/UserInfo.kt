package io.ktelemetry.models

import kotlinx.serialization.Serializable

@Serializable
data class UserInfo(
    val userId: String? = null,
    val anonymousId: String? = null,
    val userName: String? = null,
    val userEmail: String? = null,
    val userRoles: List<String> = emptyList()
)

