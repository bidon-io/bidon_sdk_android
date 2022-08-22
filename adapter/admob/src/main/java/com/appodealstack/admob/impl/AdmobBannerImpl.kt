package com.appodealstack.admob.impl

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.appodealstack.admob.AdmobBannerAuctionParams
import com.appodealstack.admob.AdmobLineItem
import com.appodealstack.admob.asBidonError
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.adapters.banners.BannerSize
import com.appodealstack.bidon.analytics.BNMediationNetwork
import com.appodealstack.bidon.auctions.data.models.AuctionResult
import com.appodealstack.bidon.auctions.data.models.LineItem
import com.appodealstack.bidon.core.ext.*
import com.google.android.gms.ads.*
import com.google.android.gms.ads.AdListener
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

internal class AdmobBannerImpl(
    override val demandId: DemandId,
    private val demandAd: DemandAd,
    private val roundId: String
) : AdSource.Banner<AdmobBannerAuctionParams> {

    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
    private val admobLineItems = mutableListOf<AdmobLineItem>()
    private var adView: AdView? = null
    private val requiredAdView: AdView get() = requireNotNull(adView)

    private val requestListener by lazy {
        object : AdListener() {
            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                logError(Tag, "Error while loading ad: $loadAdError")
                state.value = AdState.LoadFailed(loadAdError.asBidonError())
            }

            override fun onAdLoaded() {
                adView?.let {
                    it.onPaidEventListener = paidListener
                    state.value = AdState.Bid(
                        AuctionResult(
                            priceFloor = admobLineItems.getPriceFloor(it.adUnitId),
                            adSource = this@AdmobBannerImpl
                        )
                    )
                }
            }

            override fun onAdClicked() {
                state.value = AdState.Clicked(requiredAdView.asAd())
            }

            override fun onAdClosed() {
                state.value = AdState.Closed(requiredAdView.asAd())
            }

            override fun onAdImpression() {
                state.value = AdState.Impression(requiredAdView.asAd())
            }

            override fun onAdOpened() {}
        }
    }

    /**
     * @see [https://developers.google.com/android/reference/com/google/android/gms/ads/OnPaidEventListener]
     */
    private val paidListener by lazy {
        OnPaidEventListener { adValue ->
            val type = when (adValue.precisionType) {
                0 -> "UNKNOWN"
                1 -> "PRECISE"
                2 -> "ESTIMATED"
                3 -> "PUBLISHER_PROVIDED"
                else -> "unknown type ${adValue.precisionType}"
            }
            val valueMicros = adValue.valueMicros
            val ecpm = adValue.valueMicros / 1_000_000L
            logInfo(
                Tag,
                "OnPaidEventListener( ValueMicros=$valueMicros, $ecpm ${adValue.currencyCode}, $type )"
            )
        }
    }

    override val ad: Ad?
        get() = adView?.asAd()

    override val state = MutableStateFlow<AdState>(AdState.Initialized)

    override fun destroy() {
        adView?.onPaidEventListener = null
        adView = null
        admobLineItems.clear()
    }

    override fun getAuctionParams(
        priceFloor: Double,
        timeout: Long,
        lineItems: List<LineItem>,
        bannerSize: BannerSize,
        adContainer: ViewGroup,
    ): AdAuctionParams {
        return AdmobBannerAuctionParams(
            admobLineItems = lineItems
                .filter { it.demandId == demandId.demandId }
                .mapNotNull {
                    val price = it.priceFloor ?: return@mapNotNull null
                    val adUnitId = it.adUnitId ?: return@mapNotNull null
                    AdmobLineItem(price = price, adUnitId = adUnitId)
                }.sortedBy { it.price },
            priceFloor = priceFloor,
            bannerSize = bannerSize,
            adContainer = adContainer,
        )
    }

    override suspend fun bid(adParams: AdmobBannerAuctionParams): Result<AuctionResult> {
        return withContext(dispatcher) {
            logInternal(Tag, "Starting with $adParams")
            admobLineItems.addAll(adParams.admobLineItems)
            val adUnitId = admobLineItems.firstOrNull { it.price > adParams.priceFloor }?.adUnitId
            val admobBannerSize = adParams.bannerSize.asAdmobAdSize()
            if (!adUnitId.isNullOrBlank() && admobBannerSize != null) {
                val adView = AdView(adParams.adContainer.context).also {
                    adView = it
                }
                adView.setAdSize(admobBannerSize)
                adView.adUnitId = adUnitId
                adView.adListener = requestListener
                val adRequest = AdRequest.Builder().build()
                adView.loadAd(adRequest)
            } else {
                val error = BidonError.NoAppropriateAdUnitId
                logError(
                    tag = Tag,
                    message = "No appropriate AdUnitId found for price_floor=${adParams.priceFloor}. LineItems: $admobLineItems",
                    error = error
                )
                state.value = AdState.LoadFailed(error)
            }
            val state = state.first {
                it is AdState.Bid || it is AdState.LoadFailed
            }
            when (state) {
                is AdState.LoadFailed -> state.cause.asFailure()
                is AdState.Bid -> state.result.asSuccess()
                else -> error("unexpected: $state")
            }
        }
    }

    override suspend fun fill(): Result<Ad> = runCatching {
        /**
         * Admob fills the bid automatically. It's not needed to fill it manually.
         */
        AdState.Fill(
            requireNotNull(adView?.asAd())
        ).also { state.value = it }.ad
    }

    override fun show(activity: Activity) {}

    override fun getAdView(): View = requiredAdView

    private fun AdView.asAd(): Ad {
        return Ad(
            demandId = demandId,
            demandAd = demandAd,
            price = admobLineItems.getPriceFloor(adUnitId),
            sourceAd = this,
            monetizationNetwork = BNMediationNetwork.GoogleAdmob.networkName,
            dsp = null,
            roundId = roundId,
            currencyCode = "USD"
        )
    }

    private fun List<AdmobLineItem>.getPriceFloor(adUnitId: String): Double {
        return this.first { it.adUnitId == adUnitId }.price
    }

    private fun BannerSize.asAdmobAdSize() = when (this) {
        BannerSize.Banner -> AdSize.BANNER
        BannerSize.LeaderBoard -> AdSize.LEADERBOARD
        BannerSize.MRec -> AdSize.MEDIUM_RECTANGLE
        else -> null
    }
}

private const val Tag = "Admob Banner"
