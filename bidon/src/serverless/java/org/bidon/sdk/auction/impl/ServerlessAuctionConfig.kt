package org.bidon.sdk.auction.impl

import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.LineItem
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
        lineItems: List<LineItem>,
        pricefloor: Double,

        token: String? = null,
        auctionId: String = UUID.randomUUID().toString(),
        auctionConfigurationId: Int = 10,
    ) {
        auctionResponse = AuctionResponse(
            auctionId = auctionId,
            pricefloor = pricefloor,
            auctionConfigurationId = auctionConfigurationId,
            lineItems = lineItems,
            token = token,
            rounds = rounds,
            externalWinNotificationsEnabled = true
        )
    }
}