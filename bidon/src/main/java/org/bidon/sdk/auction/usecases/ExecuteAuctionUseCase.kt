package org.bidon.sdk.auction.usecases

import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.ResultsCollector
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.TokenInfo

/**
 * Created by Aleksei Cherniaev on 02/06/2023.
 */
internal interface ExecuteAuctionUseCase {
    suspend fun execute(
        auctionId: String,
        auctionConfigurationId: Long,
        auctionConfigurationUid: String,
        externalWinNotificationsEnabled: Boolean,
        demandAd: DemandAd,
        adTypeParam: AdTypeParam,
        pricefloor: Double,
        auctionTimeout: Long,
        adUnits: List<AdUnit>,
        resultsCollector: ResultsCollector,
        tokens: Map<String, TokenInfo>
    )

    suspend fun cancel(resultsCollector: ResultsCollector)
}
