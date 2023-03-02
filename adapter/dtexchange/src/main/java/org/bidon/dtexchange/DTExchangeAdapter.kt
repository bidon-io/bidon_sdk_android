package org.bidon.dtexchange

import android.app.Activity
import android.util.Log
import com.fyber.inneractive.sdk.external.InneractiveAdManager
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener.FyberInitStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bidon.dtexchange.ext.adapterVersion
import org.bidon.dtexchange.ext.sdkVersion
import org.bidon.dtexchange.impl.DTExchangeAdAuctionParams
import org.bidon.dtexchange.impl.DTExchangeInterstitial
import org.bidon.dtexchange.impl.DTExchangeRewarded
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.*
import org.bidon.sdk.logs.logging.Logger
import org.bidon.sdk.logs.logging.impl.logError
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

val DTExchangeDemandId = DemandId("dtexchange")

/**
 * Created by Aleksei Cherniaev on 28/02/2023.
 */
class DTExchangeAdapter :
    Adapter,
    Initializable<DTExchangeParameters>,
    AdProvider.Rewarded<DTExchangeAdAuctionParams>,
    AdProvider.Interstitial<DTExchangeAdAuctionParams> {
    override val demandId: DemandId = DTExchangeDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(activity: Activity, configParams: DTExchangeParameters) =
        suspendCancellableCoroutine { continuation ->
            when (BidonSdk.loggerLevel) {
                Logger.Level.Verbose -> InneractiveAdManager.setLogLevel(Log.VERBOSE)
                Logger.Level.Error -> InneractiveAdManager.setLogLevel(Log.ERROR)
                Logger.Level.Off -> {
                    // do nothing
                }
            }
            InneractiveAdManager.initialize(activity.applicationContext, configParams.appId) { initStatus ->
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

    override fun rewarded(demandAd: DemandAd, roundId: String, auctionId: String): AdSource.Rewarded<DTExchangeAdAuctionParams> {
        return DTExchangeRewarded(
            demandId = demandId,
            demandAd = demandAd,
            roundId = roundId,
            auctionId = auctionId
        )
    }
}

private const val Tag = "DTExchangeAdapter"