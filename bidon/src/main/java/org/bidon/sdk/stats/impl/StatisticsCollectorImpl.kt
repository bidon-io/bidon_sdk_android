package org.bidon.sdk.stats.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.auction.models.BannerRequest
import org.bidon.sdk.auction.models.InterstitialRequest
import org.bidon.sdk.auction.models.LineItem
import org.bidon.sdk.auction.models.RewardedRequest
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.models.BidStat
import org.bidon.sdk.stats.models.BidType
import org.bidon.sdk.stats.models.ImpressionRequestBody
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.stats.usecases.SendImpressionRequestUseCase
import org.bidon.sdk.stats.usecases.SendWinLossRequestUseCase
import org.bidon.sdk.stats.usecases.WinLossRequestData
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.ext.SystemTimeNow
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Bidon Team on 06/02/2023.
 */
class StatisticsCollectorImpl : StatisticsCollector {

    private var auctionConfigurationId: Int = 0
    private var auctionConfigurationUid: ULong = 0UL
    private var externalWinNotificationsEnabled: Boolean = true
    private lateinit var adType: StatisticsCollector.AdType

    private val impressionId: String by lazy {
        UUID.randomUUID().toString()
    }

    private val sendImpression by lazy {
        get<SendImpressionRequestUseCase>()
    }
    private val sendLossRequest by lazy {
        get<SendWinLossRequestUseCase>()
    }

    private val isShowSent = AtomicBoolean(false)
    private val isWinLossSent = AtomicBoolean(false)
    private val isClickSent = AtomicBoolean(false)
    private val isRewardSent = AtomicBoolean(false)
    private val scope by lazy {
        CoroutineScope(SdkDispatchers.IO)
    }

    private var _demandAd: DemandAd? = null
    private var stat: BidStat = BidStat(
        auctionId = null,
        roundId = null,
        roundIndex = null,
        demandId = DemandId(""),
        adUnitId = null,
        lineItemUid = null,
        fillStartTs = null,
        fillFinishTs = null,
        roundStatus = null,
        ecpm = 0.0,
        bidType = null,
    )

    override val demandAd: DemandAd
        get() = requireNotNull(_demandAd) { "DemandAd is not set" }
    override val demandId: DemandId
        get() = requireNotNull(stat.demandId) { "DemandId is not set" }
    override val auctionId: String
        get() = requireNotNull(stat.auctionId) { "AuctionId is not set" }
    override val roundId: String
        get() = requireNotNull(stat.roundId) { "RoundId is not set" }
    override val roundIndex: Int
        get() = requireNotNull(stat.roundIndex) { "RoundId is not set" }

    override fun getAd(demandAdObject: Any): Ad? {
        val demandId = stat.demandId
        val roundId = stat.roundId ?: return null
        val auctionId = stat.auctionId ?: return null
        return Ad(
            demandAd = demandAd,
            ecpm = stat.ecpm,
            networkName = demandId.demandId,
            adUnitId = stat.adUnitId,
            currencyCode = AdValue.USD,
            roundId = roundId,
            auctionId = auctionId,
            dsp = null,
            demandAdObject = demandAdObject,
        )
    }

    override fun addDemandId(demandId: DemandId) {
        stat = stat.copy(
            demandId = demandId
        )
    }

    override fun addRoundInfo(
        auctionId: String,
        roundId: String,
        roundIndex: Int,
        demandAd: DemandAd,
        bidType: BidType
    ) {
        this._demandAd = demandAd
        stat = stat.copy(
            auctionId = auctionId,
            roundId = roundId,
            roundIndex = roundIndex,
            bidType = bidType
        )
    }

    override fun sendShowImpression() {
        if (!isShowSent.getAndSet(true)) {
            scope.launch {
                val key = SendImpressionRequestUseCase.Type.Show.key
                val lastSegment = adType.asAdType().code
                sendImpression(
                    urlPath = "$key/$lastSegment",
                    bodyKey = "bid",
                    body = createImpressionRequestBody(adType),
                    extras = demandAd.getExtras()
                )
            }
        }
    }

    override fun sendClickImpression() {
        if (!isClickSent.getAndSet(true)) {
            scope.launch {
                val key = SendImpressionRequestUseCase.Type.Click.key
                val lastSegment = adType.asAdType().code
                sendImpression(
                    urlPath = "$key/$lastSegment",
                    bodyKey = "bid",
                    body = createImpressionRequestBody(adType),
                    extras = _demandAd?.getExtras().orEmpty()
                )
            }
        }
    }

    override fun sendRewardImpression() {
        if (!isRewardSent.getAndSet(true)) {
            scope.launch {
                val key = SendImpressionRequestUseCase.Type.Reward.key
                val lastSegment = StatisticsCollector.AdType.Rewarded.asAdType().code
                sendImpression(
                    urlPath = "$key/$lastSegment",
                    bodyKey = "bid",
                    body = createImpressionRequestBody(StatisticsCollector.AdType.Rewarded),
                    extras = demandAd.getExtras()
                )
            }
        }
    }

    override fun sendLoss(winnerDemandId: String, winnerEcpm: Double) {
        if (!externalWinNotificationsEnabled) {
            logInfo(TAG, "External WinLoss Notifications disabled: external_win_notifications=false")
            return
        }
        if (!isShowSent.getAndSet(true) && !isWinLossSent.getAndSet(true)) {
            scope.launch {
                sendLossRequest.invoke(
                    WinLossRequestData.Loss(
                        winnerDemandId = winnerDemandId,
                        winnerEcpm = winnerEcpm,
                        demandAd = demandAd,
                        body = createImpressionRequestBody(adType)
                    )
                )
            }
        }
    }

    override fun sendWin() {
        if (!externalWinNotificationsEnabled) {
            logInfo(TAG, "External WinLoss Notifications disabled: external_win_notifications=false")
            return
        }
        if (!isShowSent.get() && !isWinLossSent.getAndSet(true)) {
            scope.launch {
                sendLossRequest.invoke(
                    WinLossRequestData.Win(
                        demandAd = demandAd,
                        body = createImpressionRequestBody(adType)
                    )
                )
            }
        }
    }

    override fun setStatisticAdType(adType: StatisticsCollector.AdType) {
        this.adType = adType
    }

    override fun addAuctionConfigurationId(auctionConfigurationId: Int, auctionConfigurationUid: ULong) {
        this.auctionConfigurationId = auctionConfigurationId
        this.auctionConfigurationUid = auctionConfigurationUid
    }

    override fun addExternalWinNotificationsEnabled(enabled: Boolean) {
        externalWinNotificationsEnabled = enabled
    }

    override fun markFillStarted(lineItem: LineItem?, pricefloor: Double?) {
        stat = stat.copy(
            fillStartTs = SystemTimeNow,
            adUnitId = lineItem?.adUnitId,
            ecpm = pricefloor ?: stat.ecpm,
            lineItemUid = lineItem?.uid
        )
    }

    override fun markFillFinished(roundStatus: RoundStatus, ecpm: Double?) {
        stat = stat.copy(
            fillFinishTs = SystemTimeNow,
            roundStatus = roundStatus,
            ecpm = ecpm ?: 0.0
        )
    }

    override fun markWin() {
        stat = stat.copy(
            roundStatus = RoundStatus.Win
        )
    }

    override fun markLoss() {
        stat = stat.copy(
            roundStatus = RoundStatus.Lose
        )
    }

    override fun markBelowPricefloor() {
        stat = stat.copy(
            roundStatus = RoundStatus.BelowPricefloor
        )
    }

    override fun getStats(): BidStat = stat

    private fun createImpressionRequestBody(adType: StatisticsCollector.AdType): ImpressionRequestBody {
        val (banner, interstitial, rewarded) = getData(adType)
        return ImpressionRequestBody(
            auctionId = auctionId,
            roundId = roundId,
            auctionConfigurationId = auctionConfigurationId,
            auctionConfigurationUid = auctionConfigurationUid,
            impressionId = impressionId,
            demandId = demandId.demandId,
            adUnitId = stat.adUnitId,
            lineItemUid = stat.lineItemUid,
            ecpm = stat.ecpm,
            banner = banner,
            interstitial = interstitial,
            rewarded = rewarded,
            roundIndex = roundIndex,
            bidType = stat.bidType?.code,
        )
    }

    private fun getData(adType: StatisticsCollector.AdType): Triple<BannerRequest?, InterstitialRequest?, RewardedRequest?> {
        return when (adType) {
            is StatisticsCollector.AdType.Banner -> {
                Triple(BannerRequest(formatCode = adType.format.code), null, null)
            }

            StatisticsCollector.AdType.Interstitial -> {
                Triple(null, InterstitialRequest(), null)
            }

            StatisticsCollector.AdType.Rewarded -> {
                Triple(null, null, RewardedRequest())
            }
        }
    }

    private fun StatisticsCollector.AdType.asAdType() = when (this) {
        is StatisticsCollector.AdType.Banner -> AdType.Banner
        StatisticsCollector.AdType.Interstitial -> AdType.Interstitial
        StatisticsCollector.AdType.Rewarded -> AdType.Rewarded
    }
}

private const val TAG = "StatisticsCollector"