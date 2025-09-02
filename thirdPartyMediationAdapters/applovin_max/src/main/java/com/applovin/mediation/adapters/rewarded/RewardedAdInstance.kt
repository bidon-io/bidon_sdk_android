package com.applovin.mediation.adapters.rewarded

import android.app.Activity
import com.applovin.mediation.adapters.keeper.AdInstance
import com.applovin.mediation.adapters.keeper.DEFAULT_BID_TYPE
import com.applovin.mediation.adapters.keeper.DEFAULT_DEMAND_ID
import com.applovin.mediation.adapters.keeper.DEFAULT_ECPM
import com.applovin.mediation.adapters.keeper.DEFAULT_UID
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.rewarded.RewardedAd
import org.bidon.sdk.ads.rewarded.RewardedListener

internal class RewardedAdInstance(
    auctionKey: String? = null,
) : AdInstance {

    private val rewardedAd = RewardedAd(auctionKey = auctionKey)

    private var rewardedListener: RewardedListener? = null
    private var rewardedAdInfo: Ad? = null

    init {
        rewardedAd.addExtra("mediator", "max")
    }

    override val ecpm: Double get() = rewardedAdInfo?.price ?: DEFAULT_ECPM
    override val demandId: String get() = rewardedAdInfo?.networkName ?: DEFAULT_DEMAND_ID
    override val isReady: Boolean get() = rewardedAd.isReady()
    override val uid: String get() = rewardedAdInfo?.adUnit?.uid ?: DEFAULT_UID
    override val bidType: String get() = rewardedAdInfo?.bidType?.code ?: DEFAULT_BID_TYPE

    fun setListener(listener: RewardedListener) {
        this.rewardedListener = listener
        rewardedAd.setRewardedListener(listener)
    }

    fun addExtra(key: String, value: Any?) {
        rewardedAd.addExtra(key, value)
    }

    fun load(activity: Activity) {
        rewardedAd.loadAd(activity = activity, pricefloor = BidonSdk.DefaultPricefloor)
    }

    fun show(activity: Activity) {
        rewardedAd.showAd(activity)
    }

    override fun applyAdInfo(ad: Ad): RewardedAdInstance = this.apply { rewardedAdInfo = ad }

    override fun notifyWin() {
        rewardedAd.notifyWin()
    }

    override fun notifyLoss(winnerDemandId: String, winnerPrice: Double) {
        rewardedAd.notifyLoss(
            winnerDemandId = "maxca_$winnerDemandId",
            winnerPrice = winnerPrice,
        )
    }

    override fun destroy() {
        rewardedAd.destroyAd()
        rewardedListener = null
        rewardedAdInfo = null
    }
}
