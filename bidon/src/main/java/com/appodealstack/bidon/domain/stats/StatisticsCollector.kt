package com.appodealstack.bidon.domain.stats

import com.appodealstack.bidon.data.models.auction.BannerRequestBody
import com.appodealstack.bidon.data.models.stats.RoundStatus

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
