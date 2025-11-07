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
public interface StatisticsCollector {

    public val demandAd: DemandAd
    public val demandId: DemandId
    public val auctionId: String
    public fun getAd(): Ad?

    public fun sendShowImpression()
    public fun sendClickImpression()
    public fun sendRewardImpression()
    public fun sendLoss(winnerDemandId: String, winnerPrice: Double)
    public fun sendWin()

    /**
     * Some adapters don't use [AdUnit]s (BidMachine), so we need to set price manually after ad is loaded.
     * Need to be used before [AdEvent.Fill] is exposed
     */
    public fun setPrice(price: Double)

    /**
     * Set DSP source name (actually for BidMachine, DTExchange) if it's possible.
     * Need to be used before [AdEvent.Fill] is exposed
     */
    public fun setDsp(dspSource: String?)
    public fun setTokenInfo(tokenInfo: TokenInfo)
    public fun markFillStarted(adUnit: AdUnit, pricefloor: Double?)
    public fun markFillFinished(roundStatus: RoundStatus, price: Double?)
    public fun markWin()
    public fun markLoss()
    public fun markBelowPricefloor()

    public fun setStatisticAdType(adType: AdType)
    public fun addAuctionConfigurationId(auctionConfigurationId: Long)
    public fun addAuctionConfigurationUid(auctionConfigurationUid: String)
    public fun addExternalWinNotificationsEnabled(enabled: Boolean)
    public fun addDemandId(demandId: DemandId)
    public fun addRoundInfo(
        auctionId: String,
        demandAd: DemandAd,
        auctionPricefloor: Double,
    )

    public fun getStats(): BidStat

    /**
     * Checks if win/lose external notifications can be sent
     * @return true if external notifications are enabled and not already sent
     */
    public fun canSendWinLoseNotifications(): Boolean

    /**
     * Marks win/lose notifications as sent to prevent duplicate notifications
     */
    public fun markWinLoseNotificationsSent()

    public sealed interface AdType {
        public object Rewarded : AdType
        public object Interstitial : AdType
        public data class Banner(val format: BannerRequest.StatFormat) : AdType
    }
}
