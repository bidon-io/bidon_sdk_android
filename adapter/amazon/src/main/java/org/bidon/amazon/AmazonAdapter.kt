package org.bidon.amazon

import android.content.Context
import com.amazon.device.ads.AdRegistration
import com.amazon.device.ads.MRAIDPolicy
import org.bidon.amazon.ext.adapterVersion
import org.bidon.amazon.ext.sdkVersion
import org.bidon.amazon.impl.AmazonBannerImpl
import org.bidon.amazon.impl.AmazonInterstitialImpl
import org.bidon.amazon.impl.AmazonRewardedImpl
import org.bidon.amazon.impl.BannerAuctionParams
import org.bidon.amazon.impl.FullscreenAuctionParams
import org.bidon.amazon.impl.ParseSlotsUseCase
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.adapter.SupportsTestMode
import org.bidon.sdk.adapter.impl.SupportsTestModeImpl
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.logs.logging.Logger
import org.bidon.sdk.logs.logging.impl.logInfo
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Aleksei Cherniaev on 27/09/2023.
 *
 * [Amazon documentation](https://ams.amazon.com/webpublisher/uam/docs/aps-mobile/android)
 */
internal val AmazonDemandId = DemandId("amazon")

@Suppress("unused")
internal class AmazonAdapter :
    Adapter.Bidding,
    Initializable<AmazonParameters>,
    SupportsTestMode by SupportsTestModeImpl(),
    AdProvider.Banner<BannerAuctionParams>,
    AdProvider.Interstitial<FullscreenAuctionParams>,
    AdProvider.Rewarded<FullscreenAuctionParams> {

    private var slots: Map<SlotType, List<String>> = emptyMap()
    private val bidManager: AmazonBidManager get() = amazonBidManager

    override val demandId: DemandId = AmazonDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun getToken(adTypeParam: AdTypeParam): String? =
        bidManager.obtainToken(slots, adTypeParam)

    override fun parseConfigParam(json: String): AmazonParameters {
        val jsonObject = JSONObject(json)
        return AmazonParameters(
            appKey = jsonObject.getString("app_key"),
            slots = ParseSlotsUseCase()(jsonObject).also {
                logInfo(TAG, "Parsed slots: $it")
            }
        )
    }

    override suspend fun init(context: Context, configParams: AmazonParameters) =
        suspendCoroutine { continuation ->
            this.slots = configParams.slots

            AdRegistration.enableTesting(isTestMode)
            AdRegistration.enableLogging(BidonSdk.loggerLevel in arrayOf(Logger.Level.Verbose, Logger.Level.Error))

            if (AdRegistration.isInitialized()) {
                logInfo(TAG, "Amazon SDK is already initialized")
                continuation.resume(Unit)
            } else {
                logInfo(TAG, "Initializing Amazon SDK")
                // TODO: 16/09/2024 [glavatskikh] Pass activity to AdRegistration.getInstance
                //  for correct initialization of com.amazon.device.ads.ActivityMonitor.
                //  Currently, we pass activity through DTBActivityMonitor.setActivity(activity) as a workaround.
                // Initialize Amazon SDK
                AdRegistration.getInstance(configParams.appKey, context)

                AdRegistration.setMRAIDSupportedVersions(arrayOf("1.0", "2.0", "3.0"))
                AdRegistration.setMRAIDPolicy(MRAIDPolicy.CUSTOM)
                continuation.resume(Unit)
            }
        }

    override fun banner(): AdSource.Banner<BannerAuctionParams> {
        return AmazonBannerImpl(bidManager)
    }

    override fun interstitial(): AdSource.Interstitial<FullscreenAuctionParams> {
        return AmazonInterstitialImpl(bidManager)
    }

    override fun rewarded(): AdSource.Rewarded<FullscreenAuctionParams> {
        return AmazonRewardedImpl(bidManager)
    }
}

private const val TAG = "AmazonAdapter"