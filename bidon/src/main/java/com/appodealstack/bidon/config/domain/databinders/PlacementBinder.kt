package com.appodealstack.bidon.config.domain.databinders

import com.appodealstack.bidon.config.data.models.Capping
import com.appodealstack.bidon.config.data.models.Placement
import com.appodealstack.bidon.config.data.models.Reward
import com.appodealstack.bidon.config.domain.DataBinder
import com.appodealstack.bidon.core.BidonJson
import com.appodealstack.bidon.utilities.datasource.placement.PlacementDataSource
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

internal class PlacementBinder(
    val dataSource: PlacementDataSource,
) : DataBinder {
    override val fieldName: String = "placement"

    override suspend fun getJsonElement(): JsonElement =
        BidonJson.encodeToJsonElement(createPlacement())

    private fun createPlacement(): Placement {
        val type = dataSource.getRewardType()
        val amount = dataSource.getRewardAmount()
        val reward = amount?.let {
            Reward(currency = type ?: "", amount = it)
        }
        return Placement(
            name = dataSource.getName(),
            reward = reward,
            capping = Capping(
                setting = dataSource.getCappingSetting(),
                value = dataSource.getCappingValue()
            )
        )
    }
}