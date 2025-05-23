package org.bidon.bigoads

import android.content.Context
import org.bidon.bigoads.ext.adapterVersion
import org.bidon.bigoads.ext.sdkVersion
import org.bidon.bigoads.impl.BigoAdsBannerAuctionParams
import org.bidon.bigoads.impl.BigoAdsBannerImpl
import org.bidon.bigoads.impl.BigoAdsFullscreenAuctionParams
import org.bidon.bigoads.impl.BigoAdsInterstitialImpl
import org.bidon.bigoads.impl.BigoAdsRewardedAdImpl
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.adapter.SupportsRegulation
import org.bidon.sdk.adapter.SupportsTestMode
import org.bidon.sdk.adapter.impl.SupportsTestModeImpl
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.regulation.Regulation
import org.json.JSONObject
import sg.bigo.ads.BigoAdSdk
import sg.bigo.ads.ConsentOptions
import sg.bigo.ads.api.AdConfig
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Aleksei Cherniaev on 25/07/2023.
 */
internal val BigoAdsDemandId = DemandId("bigoads")

/**
 * [BigoAds](https://www.bigossp.com/guide/sdk/android/document)
 */
@Suppress("unused")
internal class BigoAdsAdapter :
    Adapter.Bidding,
    Adapter.Network,
    Initializable<BigoAdsParameters>,
    SupportsTestMode by SupportsTestModeImpl(),
    AdProvider.Banner<BigoAdsBannerAuctionParams>,
    SupportsRegulation,
    AdProvider.Interstitial<BigoAdsFullscreenAuctionParams>,
    AdProvider.Rewarded<BigoAdsFullscreenAuctionParams> {

    private var context: Context? = null

    override val demandId: DemandId = BigoAdsDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun getToken(adTypeParam: AdTypeParam): String? = BigoAdSdk.getBidderToken()

    override suspend fun init(context: Context, configParams: BigoAdsParameters) = suspendCoroutine { continuation ->
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

    override fun parseConfigParam(json: String): BigoAdsParameters {
        return BigoAdsParameters(
            appId = JSONObject(json).getString("app_id"),
            channel = JSONObject(json).optString("channel"),
        )
    }

    override fun banner(): AdSource.Banner<BigoAdsBannerAuctionParams> {
        return BigoAdsBannerImpl()
    }

    override fun interstitial(): AdSource.Interstitial<BigoAdsFullscreenAuctionParams> {
        return BigoAdsInterstitialImpl()
    }

    override fun rewarded(): AdSource.Rewarded<BigoAdsFullscreenAuctionParams> {
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
            BigoAdSdk.setUserConsent(context, ConsentOptions.COPPA, !regulation.coppaApplies)
        }
    }
}