package org.bidon.sdk.auction.usecases

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.Mode
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.ResultsCollector
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.RoundRequest
import org.bidon.sdk.auction.usecases.models.NetworksResult

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */
internal interface ConductNetworkRoundUseCase {
    /**
     * @param participantIds Bidding Demand Ids
     */
    fun invoke(
        context: Context,
        networkSources: List<Mode.Network>,
        participantIds: List<String>,
        adTypeParam: AdTypeParam,
        demandAd: DemandAd,
        lineItems: List<LineItem>,
        round: RoundRequest,
        pricefloor: Double,
        scope: CoroutineScope,
        resultsCollector: ResultsCollector,
    ): NetworksResult
}
