package org.bidon.sdk.stats.impl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.ads.AdType
import org.bidon.sdk.auction.models.BannerRequestBody
import org.bidon.sdk.auction.models.InterstitialRequestBody
import org.bidon.sdk.auction.models.RewardedRequestBody
import org.bidon.sdk.stats.BidStat
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.models.ImpressionRequestBody
import org.bidon.sdk.stats.models.RoundStatus
import org.bidon.sdk.stats.usecases.SendImpressionRequestUseCase
import org.bidon.sdk.stats.usecases.SendLossRequestUseCase
import org.bidon.sdk.utils.SdkDispatchers
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.ext.SystemTimeNow
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Bidon Team on 06/02/2023.
 */
class StatisticsCollectorImpl(
    auctionId: String,
    roundId: String,
    demandId: DemandId,
    private val demandAd: DemandAd
) : StatisticsCollector {

    private var auctionConfigurationId: Int = 0

    private val impressionId: String by lazy {
        UUID.randomUUID().toString()
    }

    private val sendImpression by lazy {
        get<SendImpressionRequestUseCase>()
    }
    private val sendLossRequest by lazy {
        get<SendLossRequestUseCase>()
    }

    private val isShowSent = AtomicBoolean(false)
    private val isClickSent = AtomicBoolean(false)
    private val isRewardSent = AtomicBoolean(false)
    private val scope by lazy {
        CoroutineScope(SdkDispatchers.IO)
    }

    private var stat: BidStat = BidStat(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId,
        adUnitId = null,
        bidStartTs = null,
        bidFinishTs = null,
        fillStartTs = null,
        fillFinishTs = null,
        roundStatus = null,
        ecpm = null
    )

    override fun sendShowImpression(adType: StatisticsCollector.AdType) {
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

    override fun sendClickImpression(adType: StatisticsCollector.AdType) {
        if (!isClickSent.getAndSet(true)) {
            scope.launch {
                val key = SendImpressionRequestUseCase.Type.Click.key
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

    override fun sendLoss(winnerDemandId: String, winnerEcpm: Double, adType: StatisticsCollector.AdType) {
        scope.launch {
            sendLossRequest.invoke(
                winnerDemandId = winnerDemandId,
                winnerEcpm = winnerEcpm,
                demandAd = demandAd,
                bodyKey = "bid",
                body = createImpressionRequestBody(adType)
            )
        }
    }

    override fun addAuctionConfigurationId(auctionConfigurationId: Int) {
        this.auctionConfigurationId = auctionConfigurationId
    }

    override fun markBidStarted(adUnitId: String?) {
        stat = stat.copy(
            bidStartTs = SystemTimeNow,
            adUnitId = adUnitId
        )
    }

    override fun markBidFinished(roundStatus: RoundStatus, ecpm: Double?) {
        stat = stat.copy(
            bidFinishTs = SystemTimeNow,
            roundStatus = roundStatus,
            ecpm = ecpm,
        )
    }

    override fun markFillStarted() {
        stat = stat.copy(
            fillStartTs = SystemTimeNow,
        )
    }

    override fun markFillFinished(roundStatus: RoundStatus) {
        stat = stat.copy(
            fillFinishTs = SystemTimeNow,
            roundStatus = roundStatus,
        )
    }

    override fun markWin() {
        stat = stat.copy(
            roundStatus = RoundStatus.Win
        )
    }

    override fun markLoss() {
        stat = stat.copy(
            roundStatus = RoundStatus.Loss
        )
    }

    override fun markBelowPricefloor() {
        stat = stat.copy(
            roundStatus = RoundStatus.BelowPricefloor
        )
    }

    override fun buildBidStatistic(): BidStat = stat

    private fun createImpressionRequestBody(adType: StatisticsCollector.AdType): ImpressionRequestBody {
        val (banner, interstitial, rewarded) = getData(adType)
        return ImpressionRequestBody(
            auctionId = stat.auctionId,
            auctionConfigurationId = auctionConfigurationId,
            impressionId = impressionId,
            demandId = stat.demandId.demandId,
            adUnitId = stat.adUnitId,
            ecpm = stat.ecpm ?: 0.0,
            banner = banner,
            interstitial = interstitial,
            rewarded = rewarded,
        )
    }

    private fun getData(adType: StatisticsCollector.AdType): Triple<BannerRequestBody?, InterstitialRequestBody?, RewardedRequestBody?> {
        return when (adType) {
            is StatisticsCollector.AdType.Banner -> {
                Triple(BannerRequestBody(formatCode = adType.format.code), null, null)
            }
            StatisticsCollector.AdType.Interstitial -> {
                Triple(null, InterstitialRequestBody(), null)
            }
            StatisticsCollector.AdType.Rewarded -> {
                Triple(null, null, RewardedRequestBody())
            }
        }
    }

    private fun StatisticsCollector.AdType.asAdType() = when (this) {
        is StatisticsCollector.AdType.Banner -> AdType.Banner
        StatisticsCollector.AdType.Interstitial -> AdType.Interstitial
        StatisticsCollector.AdType.Rewarded -> AdType.Rewarded
    }
}