package com.appodealstack.fyber.rewarded

import android.app.Activity
import androidx.core.os.bundleOf
import com.appodealstack.bidon.ads.AdType
import com.appodealstack.bidon.ads.DemandAd
import com.appodealstack.fyber.PlacementKey
import com.fyber.fairbid.ads.ImpressionData
import com.fyber.fairbid.ads.Interstitial
import com.fyber.fairbid.ads.LossNotificationReason
import com.fyber.fairbid.ads.ShowOptions

object BNFyberRewarded : FyberRewarded by FyberRewardedImpl()

interface FyberRewarded {
    fun request(placementId: String)
    fun request(placementId: String, activity: Activity?)
    fun setRewardedListener(fyberRewardedListener: FyberRewardedListener)
    fun show(placementId: String, activity: Activity)
    fun show(placementId: String, showOptions: ShowOptions, activity: Activity)
    fun isAvailable(placementId: String): Boolean
    fun getImpressionData(placementId: String): ImpressionData?
    fun enableAutoRequesting(placementId: String)
    fun disableAutoRequesting(placementId: String)
    fun getImpressionDepth(): Int
    fun notifyLoss(placementId: String, reason: LossNotificationReason)
}

class FyberRewardedImpl : FyberRewarded {
    private val ads = mutableMapOf<String, DemandAd>()
    private var fyberRewardedListener: FyberRewardedListener? = null

    override fun request(placementId: String) {
        request(placementId, null)
    }

    override fun request(placementId: String, activity: Activity?) {
        val demandAd = getDemandAd(placementId)
        fyberRewardedListener?.let {
            setInterstitialListener(
                demandAd = demandAd,
                fyberRewardedListener = it,
                placementId = placementId
            )
        }
    }

    override fun setRewardedListener(fyberRewardedListener: FyberRewardedListener) {
        this.fyberRewardedListener = fyberRewardedListener
        ads.forEach { (placementId, demandAd) ->
            setInterstitialListener(demandAd, fyberRewardedListener, placementId)
        }
    }

    override fun show(placementId: String, activity: Activity) {
    }

    override fun show(placementId: String, showOptions: ShowOptions, activity: Activity) {
        val bundle = bundleOf(PlacementKey to placementId).apply {
            showOptions.customParameters.forEach { (key, value) ->
                this.putString(key, value)
            }
        }
    }

    override fun isAvailable(placementId: String): Boolean {
        return Interstitial.isAvailable(placementId)
    }

    override fun getImpressionData(placementId: String): ImpressionData? {
        return Interstitial.getImpressionData(placementId)
    }

    override fun enableAutoRequesting(placementId: String) {
    }

    override fun disableAutoRequesting(placementId: String) {
    }

    override fun getImpressionDepth(): Int {
        return Interstitial.getImpressionDepth()
    }

    override fun notifyLoss(placementId: String, reason: LossNotificationReason) {
        Interstitial.notifyLoss(placementId, reason)
    }

    private fun getDemandAd(placementId: String) = ads.getOrPut(placementId) {
        DemandAd(AdType.Rewarded)
    }

    private fun setInterstitialListener(
        demandAd: DemandAd,
        fyberRewardedListener: FyberRewardedListener,
        placementId: String
    ) {
    }
}
