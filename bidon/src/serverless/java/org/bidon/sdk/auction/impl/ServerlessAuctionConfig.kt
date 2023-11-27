package org.bidon.sdk.auction.impl

import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.RoundRequest
import java.util.UUID

/**
 * Created by Bidon Team on 06/03/2023.
 */
object ServerlessAuctionConfig {

    private var auctionResponse: AuctionResponse? = null

    internal fun getAuctionResponse() = auctionResponse

    fun setLocalAuctionResponse(
        rounds: List<RoundRequest>,
        adUnits: List<AdUnit>,
        pricefloor: Double,

        token: String? = null,
        auctionId: String = UUID.randomUUID().toString(),
        auctionConfigurationUid: String = "1238798479",
    ) {
        auctionResponse = AuctionResponse(
            auctionId = auctionId,
            pricefloor = pricefloor,
            auctionConfigurationUid = auctionConfigurationUid,
            adUnits = adUnits,
            token = token,
            rounds = rounds,
            externalWinNotificationsEnabled = true,
        )
    }
}