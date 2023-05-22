package org.bidon.sdk.config.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Aleksei Cherniaev on 22/05/2023.
 */
data class TokenRequestBody(
    @field:JsonName("demand_id")
    val demandId: String,
    @field:JsonName("bidding_token")
    val biddingToken: String
) : Serializable