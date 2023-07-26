package org.bidon.sdk.stats

import org.bidon.sdk.auction.models.BannerRequestBody
import org.bidon.sdk.stats.models.BidStat
import org.bidon.sdk.stats.models.RoundStatus

/**
 * Created by Bidon Team on 06/02/2023.
 */
interface StatisticsCollector {

    fun sendShowImpression()
    fun sendClickImpression()
    fun sendRewardImpression()
    fun sendLoss(winnerDemandId: String, winnerEcpm: Double)
    fun sendWin()

    fun markBidStarted(adUnitId: String? = null)
    fun markBidFinished(roundStatus: RoundStatus, ecpm: Double?)
    fun markFillStarted(adUnitId: String?)
    fun markFillFinished(roundStatus: RoundStatus, ecpm: Double?)
    fun markWin()
    fun markLoss()
    fun markBelowPricefloor()

    fun setStatisticAdType(adType: AdType)
    fun addAuctionConfigurationId(auctionConfigurationId: Int)
    fun addExternalWinNotificationsEnabled(enabled: Boolean)

    fun buildBidStatistic(): BidStat

    sealed interface AdType {
        object Rewarded : AdType
        object Interstitial : AdType
        data class Banner(val format: BannerRequestBody.StatFormat) : AdType
    }
}
