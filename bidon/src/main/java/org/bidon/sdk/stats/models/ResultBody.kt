package org.bidon.sdk.stats.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Bidon Team on 03/03/2023.
 */
internal data class ResultBody(
    @field:JsonName("status")
    val status: String,
    @field:JsonName("winner_id")
    val demandId: String?,
    @field:JsonName("ecpm")
    val ecpm: Double?,
    @field:JsonName("ad_unit_id")
    val adUnitId: String?,
) : Serializable
