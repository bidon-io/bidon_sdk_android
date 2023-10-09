package org.bidon.amazon

import android.content.Context
import com.amazon.device.ads.AdRegistration
import com.amazon.device.ads.MRAIDPolicy
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bidon.amazon.ext.adapterVersion
import org.bidon.amazon.ext.sdkVersion
import org.bidon.amazon.impl.AmazonBannerImpl
import org.bidon.amazon.impl.AmazonInterstitialImpl
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
import org.bidon.sdk.logs.logging.Logger
import org.bidon.sdk.logs.logging.impl.logInfo
import org.json.JSONObject
import kotlin.coroutines.resume

/**
 * Created by Aleksei Cherniaev on 27/09/2023.
 *
 * [Amazon documentation](https://ams.amazon.com/webpublisher/uam/docs/aps-mobile/android)
 */
internal val AmazonDemandId = DemandId("amazon")

class AmazonAdapter :
    Adapter,
    Initializable<AmazonParameters>,
    SupportsTestMode by SupportsTestModeImpl(),
    AdProvider.Banner<BannerAuctionParams>,
    AdProvider.Interstitial<FullscreenAuctionParams> {
    private var slots: Map<SlotType, List<String>> = emptyMap()

    override val demandId: DemandId = AmazonDemandId

    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override fun parseConfigParam(json: String): AmazonParameters {
        val jsonObject = JSONObject(json)
        return AmazonParameters(
            appKey = jsonObject.getString("app_key"),
            slots = ParseSlotsUseCase()(jsonObject).also {
                logInfo("AmazonAdapter", "Parsed slots: $it")
            }
        )
    }

    override suspend fun init(context: Context, configParams: AmazonParameters) = suspendCancellableCoroutine { continuation ->
        if (isTestMode) {
            AdRegistration.enableTesting(true)
        }
        AdRegistration.enableLogging(BidonSdk.loggerLevel in arrayOf(Logger.Level.Verbose, Logger.Level.Error))

        AdRegistration.getInstance(configParams.appKey, context)
        slots = configParams.slots
        AdRegistration.setMRAIDSupportedVersions(arrayOf("1.0", "2.0", "3.0"))
        AdRegistration.setMRAIDPolicy(MRAIDPolicy.CUSTOM)
        continuation.resume(Unit)
    }

    override fun banner(): AdSource.Banner<BannerAuctionParams> {
        return AmazonBannerImpl(slots)
    }

    override fun interstitial(): AdSource.Interstitial<FullscreenAuctionParams> {
        return AmazonInterstitialImpl(slots)
    }
}