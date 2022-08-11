package com.appodealstack.bidon.config.domain

import kotlinx.serialization.json.JsonElement

internal interface DataProvider {
    suspend fun provide(dataBinders: List<DataBinderType>): Map<String, JsonElement>
}