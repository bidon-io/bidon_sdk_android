package com.appodealstack.bidon.analytics.domain

import com.appodealstack.bidon.adapters.DemandId
import com.appodealstack.bidon.analytics.StatisticsCollector
import com.appodealstack.bidon.analytics.data.models.ImpressionRequestBody
import com.appodealstack.bidon.analytics.data.models.RoundStatus
import com.appodealstack.bidon.auctions.data.models.BannerRequestBody
import com.appodealstack.bidon.auctions.data.models.BidStat
import com.appodealstack.bidon.auctions.data.models.InterstitialRequestBody
import com.appodealstack.bidon.auctions.data.models.RewardedRequestBody
import com.appodealstack.bidon.core.UrlPathAdType
import com.appodealstack.bidon.core.ext.SystemTimeNow
import com.appodealstack.bidon.di.get
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

    private var stat: BidStat = BidStat(
        auctionId = auctionId,
        roundId = roundId,
        demandId = demandId,
        startTs = null,
        adUnitId = null,
        finishTs = null,
        roundStatus = null,
        ecpm = null
    )

    override suspend fun sendShowImpression(adType: StatisticsCollector.AdType) {
        if (!isShowSent.getAndSet(true)) {
            val key = SendImpressionRequestUseCase.Type.Show.key
            val lastSegment = adType.asUrlPathAdType().lastSegment
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
            val lastSegment = adType.asUrlPathAdType().lastSegment
            sendImpression(
                urlPath = "$key/$lastSegment",
                bodyKey = key,
                body = createImpressionRequestBody(adType)
            )
        }
    }

    override fun addAuctionConfigurationId(auctionConfigurationId: Int) {
        this.auctionConfigurationId = auctionConfigurationId
    }

    override fun markBidStarted(adUnitId: String?) {
        stat = stat.copy(
            startTs = SystemTimeNow,
            adUnitId = adUnitId
        )
    }

    override fun markBidFinished(roundStatus: RoundStatus, ecpm: Double?) {
        stat = stat.copy(
            finishTs = SystemTimeNow,
            roundStatus = roundStatus,
            ecpm = ecpm,
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

    private fun StatisticsCollector.AdType.asUrlPathAdType() = when (this) {
        is StatisticsCollector.AdType.Banner -> UrlPathAdType.Banner
        StatisticsCollector.AdType.Interstitial -> UrlPathAdType.Interstitial
        StatisticsCollector.AdType.Rewarded -> UrlPathAdType.Rewarded
    }
}