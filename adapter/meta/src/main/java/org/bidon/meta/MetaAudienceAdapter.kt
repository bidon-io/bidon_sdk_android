package org.bidon.meta

import android.content.Context
import com.facebook.ads.AdSettings
import com.facebook.ads.AudienceNetworkAds
import kotlinx.coroutines.suspendCancellableCoroutine
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
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.regulation.Regulation
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Created by Aleksei Cherniaev on 08/08/2023.
 */
val MetaDemandId = DemandId("meta")

class MetaAudienceAdapter :
    Adapter,
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

    override suspend fun init(context: Context, configParams: MetaParams) = suspendCancellableCoroutine {
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
        AdSettings.setMixedAudience(regulation.coppaApplies)
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