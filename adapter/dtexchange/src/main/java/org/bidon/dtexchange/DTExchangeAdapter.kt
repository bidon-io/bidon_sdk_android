package org.bidon.dtexchange

import android.content.Context
import android.util.Log
import com.fyber.inneractive.sdk.external.InneractiveAdManager
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener.FyberInitStatus
import org.bidon.dtexchange.ext.adapterVersion
import org.bidon.dtexchange.ext.sdkVersion
import org.bidon.dtexchange.impl.DTExchangeAdAuctionParams
import org.bidon.dtexchange.impl.DTExchangeBanner
import org.bidon.dtexchange.impl.DTExchangeBannerAuctionParams
import org.bidon.dtexchange.impl.DTExchangeInterstitial
import org.bidon.dtexchange.impl.DTExchangeRewarded
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.adapter.SupportsRegulation
import org.bidon.sdk.logs.logging.Logger
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.regulation.Regulation
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Aleksei Cherniaev on 28/02/2023.
 */
val DTExchangeDemandId = DemandId("dtexchange")

/**
 * [Documentation](https://developer.digitalturbine.com/hc/en-us/articles/360019744297-Android-Ad-Formats)
 */
class DTExchangeAdapter :
    Adapter,
    SupportsRegulation,
    Initializable<DTExchangeParameters>,
    AdProvider.Rewarded<DTExchangeAdAuctionParams>,
    AdProvider.Interstitial<DTExchangeAdAuctionParams>,
    AdProvider.Banner<DTExchangeBannerAuctionParams> {
    override val demandId: DemandId = DTExchangeDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(context: Context, configParams: DTExchangeParameters) =
        suspendCoroutine { continuation ->
            when (BidonSdk.loggerLevel) {
                Logger.Level.Verbose -> InneractiveAdManager.setLogLevel(Log.VERBOSE)
                Logger.Level.Error -> InneractiveAdManager.setLogLevel(Log.ERROR)
                Logger.Level.Off -> {
                    // do nothing
                }
            }
            InneractiveAdManager.initialize(context, configParams.appId) { initStatus ->
                when (initStatus) {
                    FyberInitStatus.SUCCESSFULLY -> {
                        continuation.resume(Unit)
                    }

                    FyberInitStatus.FAILED_NO_KITS_DETECTED,
                    FyberInitStatus.FAILED,
                    FyberInitStatus.INVALID_APP_ID, null -> {
                        val cause = Throwable("Adapter(${DTExchangeDemandId.demandId}) not initialized ($initStatus)")
                        logError(TAG, "Error while initialization", cause)
                        continuation.resumeWithException(cause)
                    }
                }
            }
        }

    override fun parseConfigParam(json: String): DTExchangeParameters {
        return DTExchangeParameters(
            appId = requireNotNull(JSONObject(json).optString("app_id")),
        )
    }

    override fun updateRegulation(regulation: Regulation) {
        if (regulation.ccpaApplies) {
            InneractiveAdManager.setUSPrivacyString(regulation.usPrivacyString)
        } else {
            InneractiveAdManager.clearUSPrivacyString()
        }
        if (regulation.gdprApplies) {
            InneractiveAdManager.setGdprConsent(regulation.hasGdprConsent)
            if (!regulation.gdprConsentString.isNullOrBlank()) {
                InneractiveAdManager.setGdprConsentString(regulation.gdprConsentString)
            }
        } else {
            InneractiveAdManager.clearGdprConsentData()
        }
        if (regulation.coppaApplies) {
            InneractiveAdManager.currentAudienceAppliesToCoppa()
        }
    }

    override fun rewarded(): AdSource.Rewarded<DTExchangeAdAuctionParams> {
        return DTExchangeRewarded()
    }

    override fun interstitial(): AdSource.Interstitial<DTExchangeAdAuctionParams> {
        return DTExchangeInterstitial()
    }

    override fun banner(): AdSource.Banner<DTExchangeBannerAuctionParams> {
        return DTExchangeBanner()
    }
}

private const val TAG = "DTExchangeAdapter"