package com.appodealstack.bidon.domain.stats.impl

import com.appodealstack.bidon.data.models.auction.BannerRequestBody
import com.appodealstack.bidon.data.models.auction.InterstitialRequestBody
import com.appodealstack.bidon.data.models.auction.RewardedRequestBody
import com.appodealstack.bidon.data.models.stats.ImpressionRequestBody
import com.appodealstack.bidon.data.models.stats.RoundStatus
import com.appodealstack.bidon.data.time.SystemTimeNow
import com.appodealstack.bidon.di.get
import com.appodealstack.bidon.domain.common.AdType
import com.appodealstack.bidon.domain.common.DemandId
import com.appodealstack.bidon.domain.stats.BidStat
import com.appodealstack.bidon.domain.stats.StatisticsCollector
import com.appodealstack.bidon.domain.stats.usecases.SendImpressionRequestUseCase
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class StatisticsCollectorImpl(
    auctionId: String,
    roundId: String,
    demandId: DemandId
) : StatisticsCollector {

    private var auctionConfigurationId: Int = 0

    private val impressionId: String by lazy {
        UUID.randomUUID().toString()
    }

    private val sendImpression by lazy {
        get<SendImpressionRequestUseCase>()
    }

    private val isShowSent = AtomicBoolean(false)
    private val isClickSent = AtomicBoolean(false)
    private val isRewardSent = AtomicBoolean(false)

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

    override suspend fun sendShowImpression(adType: StatisticsCollector.AdType) {
        if (!isShowSent.getAndSet(true)) {
            val key = SendImpressionRequestUseCase.Type.Show.key
            val lastSegment = adType.asAdType().code
            sendImpression(
                urlPath = "$key/$lastSegment",
                bodyKey = key,
                body = createImpressionRequestBody(adType)
            )
        }
    }

    override suspend fun sendClickImpression(adType: StatisticsCollector.AdType) {
        if (!isClickSent.getAndSet(true)) {
            val key = SendImpressionRequestUseCase.Type.Click.key
            val lastSegment = adType.asAdType().code
            sendImpression(
                urlPath = "$key/$lastSegment",
                bodyKey = key,
                body = createImpressionRequestBody(adType)
            )
        }
    }

    override suspend fun sendRewardImpression() {
        if (!isRewardSent.getAndSet(true)) {
            val key = SendImpressionRequestUseCase.Type.Reward.key
            sendImpression(
                urlPath = key,
                bodyKey = key,
                body = createImpressionRequestBody(StatisticsCollector.AdType.Rewarded)
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