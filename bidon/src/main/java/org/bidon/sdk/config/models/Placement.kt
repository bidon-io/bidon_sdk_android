package org.bidon.sdk.config.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal data class Placement(
    @field:JsonName("name")
    val name: String,
    @field:JsonName("reward")
    val reward: Reward?,
    @field:JsonName("capping")
    val capping: Capping?,
) : Serializable
