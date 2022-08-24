package com.appodealstack.bidmachine

import android.app.Activity
import android.content.Context
import com.appodealstack.bidmachine.ext.adapterVersion
import com.appodealstack.bidmachine.ext.sdkVersion
import com.appodealstack.bidmachine.impl.BMBannerAdImpl
import com.appodealstack.bidmachine.impl.BMInterstitialAdImpl
import com.appodealstack.bidmachine.impl.BMRewardedAdImpl
import com.appodealstack.bidon.adapters.*
import com.appodealstack.bidon.config.data.models.AdapterInfo
import com.appodealstack.bidon.core.parse
import io.bidmachine.BidMachine
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonObject
import kotlin.coroutines.resume

val BidMachineDemandId = DemandId("bidmachine")

internal typealias BidMachineBannerSize = io.bidmachine.banner.BannerSize
internal typealias BMAuctionResult = io.bidmachine.models.AuctionResult

@Suppress("unused")
class BidMachineAdapter :
    Adapter,
    Initializable<BidMachineParameters>,
    AdProvider.Banner<BMBannerAuctionParams>,
    AdProvider.Rewarded<BMFullscreenAuctionParams>,
    AdProvider.Interstitial<BMFullscreenAuctionParams> {
    private lateinit var context: Context

    override val demandId = BidMachineDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(activity: Activity, configParams: BidMachineParameters): Unit =
        suspendCancellableCoroutine { continuation ->
            this.context = activity.applicationContext
            val sourceId = configParams.sellerId
            BidMachine.setLoggingEnabled(true)
            BidMachine.initialize(context, sourceId) {
                continuation.resume(Unit)
            }
        }

    override fun parseConfigParam(json: JsonObject): BidMachineParameters = json.parse(BidMachineParameters.serializer())

    override fun interstitial(demandAd: DemandAd, roundId: String): AdSource.Interstitial<BMFullscreenAuctionParams> {
        return BMInterstitialAdImpl(demandId, demandAd, roundId)
    }

    override fun rewarded(demandAd: DemandAd, roundId: String): AdSource.Rewarded<BMFullscreenAuctionParams> {
        return BMRewardedAdImpl(demandId, demandAd, roundId)
    }

    override fun banner(demandAd: DemandAd, roundId: String): AdSource.Banner<BMBannerAuctionParams> {
        return BMBannerAdImpl(demandId, demandAd, roundId)
    }
}