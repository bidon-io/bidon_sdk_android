package org.bidon.sdk.auction.usecases

import org.bidon.sdk.ads.AdType
import org.bidon.sdk.auction.models.LineItem

/**
 * Created by Aleksei Cherniaev on 10/09/2023.
 */
object LineItemsPortal {
    operator fun invoke(scope: LineItemsPortal.() -> Unit) {
        scope(this)
    }

    private var dspInterstitialAdItems = listOf<LineItem>()
    private var dspRewardedAdItems = listOf<LineItem>()
    private var dspBannerAdItems = listOf<LineItem>()
    private var biddingInterstitialParticipants = listOf<String>()
    private var biddingRewardedAdParticipants = listOf<String>()
    private var biddingBannerParticipants = listOf<String>()

    fun AdType.use(lineItems: List<LineItem>, bidding: List<String>) {
        when (this) {
            AdType.Interstitial -> {
                dspInterstitialAdItems = lineItems
                biddingInterstitialParticipants = bidding
            }

            AdType.Rewarded -> {
                dspRewardedAdItems = lineItems
                biddingRewardedAdParticipants = bidding
            }

            AdType.Banner -> {
                dspBannerAdItems = lineItems
                biddingBannerParticipants = bidding
            }
        }
    }

    internal fun getBiddingParticipants(adType: AdType): List<String> {
        return when (adType) {
            AdType.Interstitial -> biddingInterstitialParticipants
            AdType.Rewarded -> biddingRewardedAdParticipants
            AdType.Banner -> biddingBannerParticipants
        }
    }

    internal fun getAll(adType: AdType): List<LineItem> {
        return when (adType) {
            AdType.Interstitial -> dspInterstitialAdItems
            AdType.Rewarded -> dspRewardedAdItems
            AdType.Banner -> dspBannerAdItems
        }
    }
}