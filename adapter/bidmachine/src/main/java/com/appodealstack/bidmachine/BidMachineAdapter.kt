package com.appodealstack.bidmachine

import android.app.Activity
import android.content.Context
import com.appodealstack.bidmachine.ext.adapterVersion
import com.appodealstack.bidmachine.ext.sdkVersion
import com.appodealstack.bidmachine.impl.BMBannerAdImpl
import com.appodealstack.bidmachine.impl.BMInterstitialAdImpl
import com.appodealstack.bidmachine.impl.BMRewardedAdImpl
import com.appodealstack.bidon.BidOn
import com.appodealstack.bidon.adapter.AdProvider
import com.appodealstack.bidon.adapter.AdSource
import com.appodealstack.bidon.adapter.Adapter
import com.appodealstack.bidon.adapter.Initializable
import com.appodealstack.bidon.ads.DemandAd
import com.appodealstack.bidon.ads.DemandId
import com.appodealstack.bidon.adapter.AdapterInfo
import com.appodealstack.bidon.logs.logging.Logger
import io.bidmachine.BidMachine
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
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
            BidMachine.setLoggingEnabled(BidOn.loggerLevel != Logger.Level.Off)
            BidMachine.initialize(context, sourceId) {
                continuation.resume(Unit)
            }
        }

    override fun parseConfigParam(json: String): BidMachineParameters {
        val jsonObject = JSONObject(json)
        return BidMachineParameters(
            sellerId = jsonObject.getString("seller_id"),
            endpoint = jsonObject.optString("endpoint", "").takeIf { !it.isNullOrBlank() },
            mediationConfig = jsonObject.optJSONArray("mediation_config")?.let {
                buildList {
                    repeat(it.length()) { index ->
                        add(it.getString(index))
                    }
                }
            },
        )
    }

    override fun interstitial(
        demandAd: DemandAd,
        roundId: String,
        auctionId: String
    ): AdSource.Interstitial<BMFullscreenAuctionParams> {
        return BMInterstitialAdImpl(demandId = demandId, demandAd = demandAd, roundId = roundId, auctionId = auctionId)
    }

    override fun rewarded(demandAd: DemandAd, roundId: String, auctionId: String): AdSource.Rewarded<BMFullscreenAuctionParams> {
        return BMRewardedAdImpl(demandId = demandId, demandAd = demandAd, roundId = roundId, auctionId = auctionId)
    }

    override fun banner(demandAd: DemandAd, roundId: String, auctionId: String): AdSource.Banner<BMBannerAuctionParams> {
        return BMBannerAdImpl(demandId = demandId, demandAd = demandAd, roundId = roundId, auctionId = auctionId)
    }
}