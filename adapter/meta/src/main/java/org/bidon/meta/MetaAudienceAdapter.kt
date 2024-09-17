package org.bidon.meta

import android.content.Context
import com.facebook.ads.AdSettings
import com.facebook.ads.AudienceNetworkAds
import com.facebook.ads.BidderTokenProvider
import org.bidon.meta.ext.adapterVersion
import org.bidon.meta.ext.sdkVersion
import org.bidon.meta.impl.MetaBannerAuctionParams
import org.bidon.meta.impl.MetaBannerImpl
import org.bidon.meta.impl.MetaFullscreenAuctionParams
import org.bidon.meta.impl.MetaInterstitialImpl
import org.bidon.meta.impl.MetaParams
import org.bidon.meta.impl.MetaRewardedAdImpl
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
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.regulation.Regulation
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Aleksei Cherniaev on 08/08/2023.
 */
internal val MetaDemandId = DemandId("meta")

@Suppress("unused")
internal class MetaAudienceAdapter :
    Adapter.Bidding,
    SupportsRegulation,
    SupportsTestMode by SupportsTestModeImpl(),
    AdProvider.Interstitial<MetaFullscreenAuctionParams>,
    AdProvider.Rewarded<MetaFullscreenAuctionParams>,
    AdProvider.Banner<MetaBannerAuctionParams>,
    Initializable<MetaParams> {
    override val demandId: DemandId = MetaDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun getToken(adTypeParam: AdTypeParam): String? =
        BidderTokenProvider.getBidderToken(adTypeParam.activity.applicationContext)

    override suspend fun init(context: Context, configParams: MetaParams) = suspendCoroutine {
        if (isTestMode) {
            AdSettings.setIntegrationErrorMode(AdSettings.IntegrationErrorMode.INTEGRATION_ERROR_CRASH_DEBUG_MODE)
            AdSettings.setDebugBuild(true)
        }
        AdSettings.setTestMode(isTestMode)
        configParams.mediationService?.let {
            AdSettings.setMediationService(it)
        }
        AudienceNetworkAds
            .buildInitSettings(context)
            .withInitListener { initResult ->
                if (initResult.isSuccess) {
                    it.resume(Unit)
                } else {
                    logError(TAG, "Meta SDK initialization failed: ${initResult.message}", BidonError.SdkNotInitialized)
                    it.resumeWithException(BidonError.SdkNotInitialized)
                }
            }
            .initialize()
    }

    override fun parseConfigParam(json: String): MetaParams {
        return MetaParams(
            mediationService = JSONObject(json).optString("mediation_service")
        )
    }

    override fun updateRegulation(regulation: Regulation) {
        if (regulation.coppaApplies) {
            AdSettings.setMixedAudience(true)
        }
        if (regulation.ccpaApplies) {
            if (regulation.hasCcpaConsent) {
                AdSettings.setDataProcessingOptions(arrayOf())
            } else {
                AdSettings.setDataProcessingOptions(arrayOf("LDU"), 0, 0)
            }
        }
    }

    override fun interstitial(): AdSource.Interstitial<MetaFullscreenAuctionParams> {
        return MetaInterstitialImpl()
    }

    override fun banner(): AdSource.Banner<MetaBannerAuctionParams> {
        return MetaBannerImpl()
    }

    override fun rewarded(): AdSource.Rewarded<MetaFullscreenAuctionParams> {
        return MetaRewardedAdImpl()
    }
}

private const val TAG = "MetaAudienceAdapter"