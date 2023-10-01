package org.bidon.sdk.auction.usecases

import androidx.annotation.Keep
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.auction.models.LineItem

/**
 * Created by Aleksei Cherniaev on 10/09/2023.
 */
@Keep
object LineItemsPortal {
    operator fun invoke(scope: LineItemsPortal.() -> Unit) {
        scope(this)
    }

    private var dspInterstitial = listOf<LineItem>()
    private var dspRewardedAd = listOf<LineItem>()
    private var dspBannerAd = listOf<LineItem>()
    private var biddingInterstitialParticipants = listOf<String>()
    private var biddingRewardedAdParticipants = listOf<String>()
    private var biddingBannerParticipants = listOf<String>()

    fun AdType.use(lineItems: List<LineItem>, bidding: List<String>) {
        when (this) {
            AdType.Interstitial -> {
                dspInterstitial = lineItems
                biddingInterstitialParticipants = bidding
            }

            AdType.Rewarded -> {
                dspRewardedAd = lineItems
                biddingRewardedAdParticipants = bidding
            }

            AdType.Banner -> {
                dspBannerAd = lineItems
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
            AdType.Interstitial -> dspInterstitial
            AdType.Rewarded -> dspRewardedAd
            AdType.Banner -> dspBannerAd
        }
    }
}