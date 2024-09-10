package org.bidon.sdk.auction.models

import kotlin.coroutines.cancellation.CancellationException

internal class AuctionCancellation : CancellationException() {
    override val message: String = "Auction was cancelled"
}