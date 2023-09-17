package org.bidon.sdk.auction.usecases

import org.bidon.sdk.auction.models.LineItem

/**
 * Created by Aleksei Cherniaev on 10/09/2023.
 */
object LineItemsPortal {
    operator fun invoke(scope: LineItemsPortal.() -> Unit) {
        scope(this)
    }

    var dspInterstitialLineItems = listOf<LineItem>()
    var dspRewardedAdLineItems = listOf<LineItem>()
    var dspBannerLineItems = listOf<LineItem>()
    var biddingInterstitialParticipants = listOf<String>()
    var biddingRewardedAdParticipants = listOf<String>()
    var biddingBannerParticipants = listOf<String>()
}