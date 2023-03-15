package org.bidon.sdk.auction.impl

import org.bidon.sdk.auction.models.AuctionResponse
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.Round
import java.util.UUID

/**
 * Created by Aleksei Cherniaev on 06/03/2023.
 */
object ServerlessAuctionConfig {

    private var auctionResponse: AuctionResponse? = null

    internal fun getAuctionResponse() = auctionResponse

    fun setLocalAuctionResponse(
        rounds: List<Round>,
        lineItems: List<LineItem>,
        pricefloor: Double,

        fillTimeout: Long = 5000L,
        token: String? = null,
        auctionId: String = UUID.randomUUID().toString(),
        auctionConfigurationId: Int = 10,
    ) {
        auctionResponse = AuctionResponse(
            auctionId = auctionId,
            pricefloor = pricefloor,
            fillTimeout = fillTimeout,
            auctionConfigurationId = auctionConfigurationId,
            lineItems = lineItems,
            token = token,
            rounds = rounds,
        )
    }
}