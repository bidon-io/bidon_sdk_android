package org.bidon.sdk.stats.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal data class StatsRequestBody(
    @field:JsonName("auction_id")
    val auctionId: String,
    @field:JsonName("auction_configuration_id")
    val auctionConfigurationId: Long,
    @field:JsonName("auction_configuration_uid")
    val auctionConfigurationUid: String,
    @field:JsonName("auction_pricefloor")
    val auctionPricefloor: Double?,
    @field:JsonName("ad_units")
    val adUnits: List<StatsAdUnit?>,
    @field:JsonName("result")
    val result: ResultBody,
) : Serializable
