package org.bidon.sdk.auction.impl

import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.AuctionResponse
import java.util.UUID

/**
 * Created by Bidon Team on 06/03/2023.
 */
object ServerlessAuctionConfig {

    private var auctionResponse: AuctionResponse? = null

    internal fun getAuctionResponse() = auctionResponse

    fun setLocalAuctionResponse(
        adUnits: List<AdUnit>,
        pricefloor: Double,
        auctionId: String = UUID.randomUUID().toString(),
        auctionTimeout: Long = 30_000L,
        auctionConfigurationId: Long = 1238798479,
        auctionConfigurationUid: String = "1238798479",
    ) {
        auctionResponse = AuctionResponse(
            adUnits = adUnits,
            pricefloor = pricefloor,
            auctionId = auctionId,
            auctionTimeout = auctionTimeout,
            auctionConfigurationId = auctionConfigurationId,
            auctionConfigurationUid = auctionConfigurationUid,
            externalWinNotificationsEnabled = false,
        )
    }
}