package org.bidon.sdk.stats.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Bidon Team on 06/04/2023.
 */
internal data class Loss(
    @field:JsonName("demand_id")
    val demandId: String,
    @field:JsonName("ecpm")
    val ecpm: Double,
) : Serializable
