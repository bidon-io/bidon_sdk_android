package org.bidon.sdk.databinders.placement

import org.bidon.sdk.config.models.Capping
import org.bidon.sdk.config.models.Placement
import org.bidon.sdk.config.models.Reward
import org.bidon.sdk.databinders.DataBinder
import org.bidon.sdk.utils.json.JsonParsers
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class PlacementBinder(
    val dataSource: PlacementDataSource,
) : DataBinder<JSONObject> {
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