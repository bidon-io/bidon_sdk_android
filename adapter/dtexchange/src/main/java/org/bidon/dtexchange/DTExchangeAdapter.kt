package org.bidon.dtexchange

import android.content.Context
import android.util.Log
import com.fyber.inneractive.sdk.external.InneractiveAdManager
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener.FyberInitStatus
import kotlinx.coroutines.suspendCancellableCoroutine
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
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.logs.logging.Logger
import org.bidon.sdk.logs.logging.impl.logError
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Created by Aleksei Cherniaev on 28/02/2023.
 */
val DTExchangeDemandId = DemandId("dtexchange")

/**
 * [Documentation](https://developer.digitalturbine.com/hc/en-us/articles/360019744297-Android-Ad-Formats)
 */
class DTExchangeAdapter :
    Adapter,
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
        suspendCancellableCoroutine { continuation ->
            if (BuildConfig.DEBUG) {
                when (BidonSdk.loggerLevel) {
                    Logger.Level.Verbose -> InneractiveAdManager.setLogLevel(Log.VERBOSE)
                    Logger.Level.Error -> InneractiveAdManager.setLogLevel(Log.ERROR)
                    Logger.Level.Off -> {
                        // do nothing
                    }
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
                        logError(Tag, "Error while initialization", cause)
                        continuation.resumeWithException(cause)
                    }
                }
            }
        }

    override fun parseConfigParam(json: String): DTExchangeParameters {
        return JSONObject(json).let {
            DTExchangeParameters(
                appId = requireNotNull(it.optString("app_id")),
            )
        }
    }

    override fun rewarded(demandAd: DemandAd, roundId: String, auctionId: String): AdSource.Rewarded<DTExchangeAdAuctionParams> {
        return DTExchangeRewarded(
            demandId = demandId,
            demandAd = demandAd,
            roundId = roundId,
            auctionId = auctionId
        )
    }

    override fun interstitial(
        demandAd: DemandAd,
        roundId: String,
        auctionId: String
    ): AdSource.Interstitial<DTExchangeAdAuctionParams> {
        return DTExchangeInterstitial(
            demandId = demandId,
            demandAd = demandAd,
            roundId = roundId,
            auctionId = auctionId
        )
    }

    override fun banner(
        demandAd: DemandAd,
        roundId: String,
        auctionId: String
    ): AdSource.Banner<DTExchangeBannerAuctionParams> {
        return DTExchangeBanner(
            demandId = demandId,
            demandAd = demandAd,
            roundId = roundId,
            auctionId = auctionId
        )
    }
}

private const val Tag = "DTExchangeAdapter"