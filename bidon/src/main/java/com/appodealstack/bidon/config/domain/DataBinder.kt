package com.appodealstack.bidon.config.domain

import kotlinx.serialization.json.JsonElement

internal enum class DataBinderType {
    Device,
    App,
    Geo,
    Session,
    User,
    Token,
    Placement
}

/**
 * Scheme @see https://appodeal.atlassian.net/wiki/spaces/SX/pages/4490264831/SDK+Server+Schema#SDK%3C%3EServerSchema-Session
 */
internal interface DataBinder {
    val fieldName: String
    suspend fun getJsonElement(): JsonElement
}
