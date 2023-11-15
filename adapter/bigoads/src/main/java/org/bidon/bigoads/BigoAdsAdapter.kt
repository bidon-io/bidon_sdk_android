package org.bidon.bigoads

import android.content.Context
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bidon.bigoads.ext.adapterVersion
import org.bidon.bigoads.ext.sdkVersion
import org.bidon.bigoads.impl.BigoAdsBannerImpl
import org.bidon.bigoads.impl.BigoAdsInterstitialImpl
import org.bidon.bigoads.impl.BigoAdsRewardedAdImpl
import org.bidon.bigoads.impl.BigoBannerAuctionParams
import org.bidon.bigoads.impl.BigoFullscreenAuctionParams
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.adapter.SupportsRegulation
import org.bidon.sdk.adapter.SupportsTestMode
import org.bidon.sdk.adapter.impl.SupportsTestModeImpl
import org.bidon.sdk.regulation.Regulation
import org.json.JSONObject
import sg.bigo.ads.BigoAdSdk
import sg.bigo.ads.ConsentOptions
import sg.bigo.ads.api.AdConfig
import kotlin.coroutines.resume

/**
 * Created by Aleksei Cherniaev on 25/07/2023.
 */
internal val BigoAdsDemandId = DemandId("bigoads")

/**
 * [BigoAds](https://www.bigossp.com/guide/sdk/android/document)
 */
class BigoAdsAdapter :
    Adapter,
    Initializable<BigoParameters>,
    SupportsTestMode by SupportsTestModeImpl(),
    AdProvider.Banner<BigoBannerAuctionParams>,
    SupportsRegulation,
    AdProvider.Interstitial<BigoFullscreenAuctionParams>,
    AdProvider.Rewarded<BigoFullscreenAuctionParams> {

    private var context: Context? = null

    override val demandId: DemandId = BigoAdsDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(context: Context, configParams: BigoParameters) = suspendCancellableCoroutine { continuation ->
        this.context = context
        val config = AdConfig.Builder()
            .setAppId(configParams.appId)
            .setDebug(isTestMode)
            .apply {
                configParams.channel?.let { setChannel(it) }
            }

        BigoAdSdk.initialize(context, config.build()) {
            continuation.resume(Unit)
        }
    }

    override fun parseConfigParam(json: String): BigoParameters {
        return BigoParameters(
            appId = JSONObject(json).getString("app_id"),
            channel = JSONObject(json).optString("channel"),
        )
    }

    override fun banner(): AdSource.Banner<BigoBannerAuctionParams> {
        return BigoAdsBannerImpl()
    }

    override fun interstitial(): AdSource.Interstitial<BigoFullscreenAuctionParams> {
        return BigoAdsInterstitialImpl()
    }

    override fun rewarded(): AdSource.Rewarded<BigoFullscreenAuctionParams> {
        return BigoAdsRewardedAdImpl()
    }

    override fun updateRegulation(regulation: Regulation) {
        context?.let { context ->
            if (regulation.gdprApplies) {
                BigoAdSdk.setUserConsent(context, ConsentOptions.GDPR, regulation.hasGdprConsent)
            }
            if (regulation.ccpaApplies) {
                BigoAdSdk.setUserConsent(context, ConsentOptions.CCPA, regulation.hasCcpaConsent)
            }
        }
    }
}