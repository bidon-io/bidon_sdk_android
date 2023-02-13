package com.appodealstack.bidon.stats

import com.appodealstack.bidon.auction.models.BannerRequestBody
import com.appodealstack.bidon.stats.models.RoundStatus

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
interface StatisticsCollector {

    suspend fun sendShowImpression(adType: AdType)
    suspend fun sendClickImpression(adType: AdType)
    suspend fun sendRewardImpression()

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
        data class Banner(val format: BannerRequestBody.Format) : AdType
    }
}
