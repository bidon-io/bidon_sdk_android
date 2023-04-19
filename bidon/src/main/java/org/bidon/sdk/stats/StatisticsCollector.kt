package org.bidon.sdk.stats

import org.bidon.sdk.auction.models.BannerRequestBody
import org.bidon.sdk.stats.models.RoundStatus

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface StatisticsCollector {

    fun sendShowImpression(adType: AdType)
    fun sendClickImpression(adType: AdType)
    fun sendRewardImpression()
    fun sendLoss(winnerDemandId: String, winnerEcpm: Double, adType: AdType)

    fun markBidStarted(adUnitId: String? = null)
    fun markBidFinished(roundStatus: RoundStatus, ecpm: Double?)
    fun markFillStarted()
    fun markFillFinished(roundStatus: RoundStatus)
    fun markWin()
    fun markLoss()
    fun markBelowPricefloor()
    fun addAuctionConfigurationId(auctionConfigurationId: Int)

    fun buildBidStatistic(): BidStat

    sealed interface AdType {
        object Rewarded : AdType
        object Interstitial : AdType
        data class Banner(val format: BannerRequestBody.StatFormat) : AdType
    }
}
