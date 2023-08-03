package org.bidon.sdk.auction.usecases.models

import kotlinx.coroutines.Deferred
import org.bidon.sdk.auction.models.AuctionResult
import org.bidon.sdk.auction.models.LineItem

/**
 * Created by Aleksei Cherniaev on 01/06/2023.
 */
internal class NetworksResult(
    val results: List<Deferred<AuctionResult>>,
    /**
     * Remaining LineItems, excluded consumed ones
     */
    val remainingLineItems: List<LineItem>
)
