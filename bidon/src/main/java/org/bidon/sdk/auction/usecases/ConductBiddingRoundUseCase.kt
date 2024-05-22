package org.bidon.sdk.auction.usecases

import android.content.Context
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.ResultsCollector
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.RoundRequest

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */
internal interface ConductBiddingRoundUseCase {
    /**
     * @param participantIds Bidding Demand Ids
     */
    suspend fun invoke(
        context: Context,
        biddingSources: List<Mode.Bidding>,
        participantIds: List<String>,
        adTypeParam: AdTypeParam,
        demandAd: DemandAd,
        bidfloor: Double,
        auctionId: String,
        round: RoundRequest,
        auctionConfigurationId: Long?,
        auctionConfigurationUid: String?,
        adUnits: List<AdUnit>,
        resultsCollector: ResultsCollector,
    )
}
