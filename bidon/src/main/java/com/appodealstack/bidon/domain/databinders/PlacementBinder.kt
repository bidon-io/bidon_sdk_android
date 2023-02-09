package com.appodealstack.bidon.domain.databinders

import com.appodealstack.bidon.data.binderdatasources.placement.PlacementDataSource
import com.appodealstack.bidon.data.json.JsonParsers
import com.appodealstack.bidon.data.models.config.Capping
import com.appodealstack.bidon.data.models.config.Placement
import com.appodealstack.bidon.data.models.config.Reward
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class PlacementBinder(
    val dataSource: PlacementDataSource,
) : DataBinder<JSONObject>  {
    override val fieldName: String = "placement"

    override suspend fun getJsonObject(): JSONObject = JsonParsers.serialize(createPlacement())

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