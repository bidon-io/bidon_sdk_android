package com.appodealstack.bidon.analytics

import com.appodealstack.bidon.analytics.data.models.RoundStatus
import com.appodealstack.bidon.auctions.data.models.BannerRequestBody
import com.appodealstack.bidon.auctions.data.models.BidStat

interface StatisticsCollector {

    suspend fun sendShowImpression(adType: AdType)
    suspend fun sendClickImpression(adType: AdType)

    fun markBidStarted(adUnitId: String? = null)
    fun markBidFinished(roundStatus: RoundStatus, ecpm: Double?)
    fun markWin()
    fun markLoss()
    fun addAuctionConfigurationId(auctionConfigurationId: Int)

    fun buildBidStatistic(): BidStat

    sealed interface AdType {
        object Rewarded : AdType
        object Interstitial : AdType
        data class Banner(val format: BannerRequestBody.Format) : AdType
    }
}
