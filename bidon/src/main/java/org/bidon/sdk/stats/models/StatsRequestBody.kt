package org.bidon.sdk.stats.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal data class StatsRequestBody(
    @field:JsonName("auction_id")
    val auctionId: String,
    @Deprecated("Use auctionConfigurationUid instead")
    @field:JsonName("auction_configuration_id")
    val auctionConfigurationId: Int,
    @field:JsonName("auction_configuration_uid")
    val auctionConfigurationUid: String,
    @field:JsonName("rounds")
    val rounds: List<Round>,
    @field:JsonName("result")
    val result: ResultBody,
) : Serializable
