package org.bidon.sdk.stats

import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.auction.models.AdUnit
import org.bidon.sdk.auction.models.BannerRequest
import org.bidon.sdk.auction.models.TokenInfo
import org.bidon.sdk.stats.models.BidStat
import org.bidon.sdk.stats.models.RoundStatus

/**
 * Created by Bidon Team on 06/02/2023.
 */
interface StatisticsCollector {

    val demandAd: DemandAd
    val demandId: DemandId
    val auctionId: String
    fun getAd(): Ad?

    fun sendShowImpression()
    fun sendClickImpression()
    fun sendRewardImpression()
    fun sendLoss(winnerDemandId: String, winnerPrice: Double)
    fun sendWin()

    /**
     * Some adapters don't use [AdUnit]s (BidMachine), so we need to set price manually after ad is loaded.
     * Need to be used before [AdEvent.Fill] is exposed
     */
    fun setPrice(price: Double)

    /**
     * Set DSP source name (actually for BidMachine, DTExchange) if it's possible.
     * Need to be used before [AdEvent.Fill] is exposed
     */
    fun setDsp(dspSource: String?)
    fun setTokenInfo(tokenInfo: TokenInfo)
    fun markFillStarted(adUnit: AdUnit, pricefloor: Double?)
    fun markFillFinished(roundStatus: RoundStatus, price: Double?)
    fun markWin()
    fun markLoss()
    fun markBelowPricefloor()

    fun setStatisticAdType(adType: AdType)
    fun addAuctionConfigurationId(auctionConfigurationId: Long)
    fun addAuctionConfigurationUid(auctionConfigurationUid: String)
    fun addExternalWinNotificationsEnabled(enabled: Boolean)
    fun addDemandId(demandId: DemandId)
    fun addRoundInfo(
        auctionId: String,
        demandAd: DemandAd,
        auctionPricefloor: Double,
    )

    fun getStats(): BidStat

    sealed interface AdType {
        object Rewarded : AdType
        object Interstitial : AdType
        data class Banner(val format: BannerRequest.StatFormat) : AdType
    }
}
