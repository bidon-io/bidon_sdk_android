package org.bidon.sdk.stats

import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.auction.models.BannerRequest
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.stats.models.BidStat
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.stats.models.RoundStatus

/**
 * Created by Bidon Team on 06/02/2023.
 */
interface StatisticsCollector {

    val demandAd: DemandAd
    val demandId: DemandId
    val bidType: BidType
    val auctionId: String
    val roundId: String
    val roundIndex: Int
    fun getAd(demandAdObject: Any): Ad?

    fun sendShowImpression()
    fun sendClickImpression()
    fun sendRewardImpression()
    fun sendLoss(winnerDemandId: String, winnerEcpm: Double)
    fun sendWin()

    fun markFillStarted(lineItem: LineItem?, pricefloor: Double?)
    fun markFillFinished(roundStatus: RoundStatus, ecpm: Double?)
    fun markWin()
    fun markLoss()
    fun markBelowPricefloor()

    fun setStatisticAdType(adType: AdType)
    fun addAuctionConfigurationId(auctionConfigurationId: Int, auctionConfigurationUid: String)
    fun addExternalWinNotificationsEnabled(enabled: Boolean)
    fun addDemandId(demandId: DemandId)
    fun addRoundInfo(
        auctionId: String,
        roundId: String,
        roundIndex: Int,
        demandAd: DemandAd,
        bidType: BidType
    )

    fun getStats(): BidStat

    sealed interface AdType {
        object Rewarded : AdType
        object Interstitial : AdType
        data class Banner(val format: BannerRequest.StatFormat) : AdType
    }
}
