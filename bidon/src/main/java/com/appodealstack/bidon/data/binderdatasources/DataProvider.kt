package com.appodealstack.bidon.data.binderdatasources

import com.appodealstack.bidon.domain.databinders.DataBinderType
import kotlinx.serialization.json.JsonElement

internal interface DataProvider {
    suspend fun provide(dataBinders: List<DataBinderType>): Map<String, JsonElement>
}
