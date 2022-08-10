package com.appodealstack.bidon.config.domain

import kotlinx.serialization.json.JsonElement

internal interface DataBinder {
    val fieldName: String
    fun getJsonElement(): JsonElement
}