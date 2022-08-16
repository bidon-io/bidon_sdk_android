package com.appodealstack.fyber.interstitial

import android.app.Activity
import androidx.core.os.bundleOf
import com.appodealstack.bidon.adapters.AdType
import com.appodealstack.bidon.adapters.DemandAd
import com.appodealstack.fyber.PlacementKey
import com.fyber.fairbid.ads.ImpressionData
import com.fyber.fairbid.ads.Interstitial
import com.fyber.fairbid.ads.LossNotificationReason
import com.fyber.fairbid.ads.ShowOptions

object BNFyberInterstitial : FyberInterstitial by FyberInterstitialImpl()

interface FyberInterstitial {
    fun request(placementId: String)
    fun request(placementId: String, activity: Activity?)
    fun setInterstitialListener(fyberInterstitialListener: FyberInterstitialListener)
    fun show(placementId: String, activity: Activity)
    fun show(placementId: String, showOptions: ShowOptions, activity: Activity)
    fun isAvailable(placementId: String): Boolean
    fun getImpressionData(placementId: String): ImpressionData?
    fun enableAutoRequesting(placementId: String)
    fun disableAutoRequesting(placementId: String)
    fun getImpressionDepth(): Int
    fun notifyLoss(placementId: String, reason: LossNotificationReason)
}

class FyberInterstitialImpl : FyberInterstitial {
    private val ads = mutableMapOf<String, DemandAd>()
    private var fyberInterstitialListener: FyberInterstitialListener? = null

    override fun request(placementId: String) {
        request(placementId, null)
    }

    override fun request(placementId: String, activity: Activity?) {
        val demandAd = getDemandAd(placementId)
        fyberInterstitialListener?.let {
            setInterstitialListener(
                demandAd = demandAd,
                fyberInterstitialListener = it,
                placementId = placementId
            )
        }
    }

    override fun setInterstitialListener(fyberInterstitialListener: FyberInterstitialListener) {
        this.fyberInterstitialListener = fyberInterstitialListener
        ads.forEach { (placementId, demandAd) ->
            setInterstitialListener(demandAd, fyberInterstitialListener, placementId)
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
        DemandAd(AdType.Interstitial)
    }

    private fun setInterstitialListener(
        demandAd: DemandAd,
        fyberInterstitialListener: FyberInterstitialListener,
        placementId: String
    ) {

    }
}
