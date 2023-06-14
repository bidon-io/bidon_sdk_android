package org.bidon.sdk.auction.usecases

import kotlinx.coroutines.Deferred
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.auction.models.LineItem

/**
 * Created by Aleksei Cherniaev on 01/06/2023.
 */
internal class DeferredRoundResult(
    val results: List<Deferred<AuctionResult>>,
    /**
     * Remaining LineItems, excluded consumed ones
     */
    val remainingLineItems: List<LineItem>
)
