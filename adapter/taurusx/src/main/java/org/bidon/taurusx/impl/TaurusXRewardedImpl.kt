package org.bidon.taurusx.impl

import android.app.Activity
import com.taurusx.tax.api.OnTaurusXRewardListener
import com.taurusx.tax.api.TaurusXAdError
import com.taurusx.tax.api.TaurusXRewardedAds
import org.bidon.sdk.adapter.AdAuctionParamSource
import org.bidon.sdk.adapter.AdAuctionParams
import org.bidon.sdk.adapter.AdEvent
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.impl.AdEventFlow
import org.bidon.sdk.adapter.impl.AdEventFlowImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.impl.StatisticsCollectorImpl
import org.bidon.sdk.stats.models.BidType
import org.bidon.taurusx.ext.asBidonError

internal class TaurusXRewardedImpl :
    AdSource.Rewarded<TaurusXFullscreenAuctionParams>,
    AdEventFlow by AdEventFlowImpl(),
    StatisticsCollector by StatisticsCollectorImpl() {

    private var rewarded: TaurusXRewardedAds? = null

    private val listener: OnTaurusXRewardListener = object : OnTaurusXRewardListener {
        override fun onAdLoaded() {
            logInfo(TAG, "Rewarded ad loaded successfully")
            getAd()?.let { emitEvent(AdEvent.Fill(it)) }
        }

        override fun onAdShown() {
            logInfo(TAG, "Rewarded ad shown successfully")
            getAd()?.let { ad ->
                emitEvent(AdEvent.Shown(ad))
                emitEvent(
                    AdEvent.PaidRevenue(
                        ad = ad,
                        adValue = AdValue(
                            adRevenue = rewarded?.price?.toDouble() ?: 0.0,
                            currency = AdValue.USD,
                            precision = Precision.Precise
                        )
                    )
                )
            }
        }

        override fun onAdClicked() {
            logInfo(TAG, "Rewarded ad clicked")
            getAd()?.let { emitEvent(AdEvent.Clicked(it)) }
        }

        override fun onAdClosed() {
            logInfo(TAG, "Rewarded ad closed")
            getAd()?.let { emitEvent(AdEvent.Closed(it)) }
        }

        override fun onAdFailedToLoad(error: TaurusXAdError) {
            logInfo(TAG, "Rewarded ad load failed: ${error.message}")
            emitEvent(AdEvent.LoadFailed(error.asBidonError()))
        }

        override fun onAdShowFailed(error: TaurusXAdError) {
            logInfo(TAG, "Rewarded ad show failed: ${error.message}")
            emitEvent(AdEvent.ShowFailed(error.asBidonError()))
        }

        override fun onVideoStart() {
            logInfo(TAG, "Rewarded ad video start: $this")
        }

        override fun onVideoCompleted() {
            logInfo(TAG, "Rewarded ad video completed: $this")
        }

        override fun onRewarded(reward: TaurusXRewardedAds.RewardItem?) {
            logInfo(TAG, "Rewarded ad onRewarded: $this")
            getAd()?.let {
                emitEvent(AdEvent.OnReward(ad = it, reward = null))
            }
        }

        override fun onRewardFailed() {
            logInfo(TAG, "Rewarded ad onRewardFailed: $this")
        }
    }

    override val isAdReadyToShow: Boolean
        get() = rewarded?.isReady == true

    override fun show(activity: Activity) {
        if (isAdReadyToShow) {
            rewarded?.showReward()
        } else {
            emitEvent(AdEvent.ShowFailed(BidonError.AdNotReady))
        }
    }

    override fun load(adParams: TaurusXFullscreenAuctionParams) {
        logInfo(TAG, "Starting Rewarded load")
        val adUnitId = adParams.adUnitId
        if (adUnitId == null) {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(
                        demandId = demandId, message = "adUnitId is required"
                    )
                )
            )
            return
        }
        val bidType = adParams.adUnit.bidType
        val payload = adParams.payload
        if (bidType == BidType.RTB && payload == null) {
            emitEvent(
                AdEvent.LoadFailed(
                    BidonError.IncorrectAdUnit(demandId = demandId, message = "payload is required")
                )
            )
            return
        }
        val rewarded = TaurusXRewardedAds(adParams.context).also {
            rewarded = it
        }
        rewarded.setAdUnitId(adUnitId)
        rewarded.setListener(listener)
        if (bidType == BidType.RTB) {
            rewarded.loadRewardFromBid(payload)
        } else {
            rewarded.loadReward()
        }
    }

    override fun destroy() {
        rewarded?.setListener(null)
        rewarded = null
    }

    override fun getAuctionParam(auctionParamsScope: AdAuctionParamSource): Result<AdAuctionParams> {
        return ObtainAuctionParamUseCase().getFullscreenParam(auctionParamsScope)
    }
}

private const val TAG = "TaurusXRewardedImpl"