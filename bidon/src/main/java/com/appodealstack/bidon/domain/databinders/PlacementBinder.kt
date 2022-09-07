package com.appodealstack.bidon.domain.databinders

import com.appodealstack.bidon.data.binderdatasources.placement.PlacementDataSource
import com.appodealstack.bidon.data.json.BidonJson
import com.appodealstack.bidon.data.models.config.Capping
import com.appodealstack.bidon.data.models.config.Placement
import com.appodealstack.bidon.data.models.config.Reward
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