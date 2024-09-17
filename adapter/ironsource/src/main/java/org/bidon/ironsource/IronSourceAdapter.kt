package org.bidon.ironsource

import android.content.Context
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.logger.IronSourceError
import com.unity3d.ironsourceads.InitListener
import com.unity3d.ironsourceads.InitRequest
import com.unity3d.ironsourceads.IronSourceAds
import com.unity3d.ironsourceads.LogLevel
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bidon.ironsource.ext.adapterVersion
import org.bidon.ironsource.ext.sdkVersion
import org.bidon.ironsource.impl.IronSourceBannerAuctionParams
import org.bidon.ironsource.impl.IronSourceBannerImpl
import org.bidon.ironsource.impl.IronSourceFullscreenAuctionParams
import org.bidon.ironsource.impl.IronSourceInterstitialImpl
import org.bidon.ironsource.impl.IronSourceRewardedAdImpl
import org.bidon.ironsource.impl.ironSourceRouter
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.adapter.SupportsRegulation
import org.bidon.sdk.adapter.SupportsTestMode
import org.bidon.sdk.adapter.impl.SupportsTestModeImpl
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.regulation.Regulation
import org.json.JSONObject

internal val IronSourceDemandId = DemandId("ironsource")

@Suppress("unused")
internal class IronSourceAdapter :
    Adapter.Network,
    SupportsRegulation,
    SupportsTestMode by SupportsTestModeImpl(),
    AdProvider.Interstitial<IronSourceFullscreenAuctionParams>,
    AdProvider.Rewarded<IronSourceFullscreenAuctionParams>,
    AdProvider.Banner<IronSourceBannerAuctionParams>,
    Initializable<IronSourceParameters> {

    override val demandId: DemandId = IronSourceDemandId
    override val adapterInfo: AdapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override fun parseConfigParam(json: String): IronSourceParameters {
        return IronSourceParameters(
            appKey = JSONObject(json).getString("app_key")
        )
    }

    override suspend fun init(context: Context, configParams: IronSourceParameters) =
        suspendCancellableCoroutine { continuation ->
            IronSource.setAdaptersDebug(isTestMode)

            // Set the IronSource callbacks router
            IronSource.setISDemandOnlyInterstitialListener(ironSourceRouter)
            IronSource.setISDemandOnlyRewardedVideoListener(ironSourceRouter)

            val initRequest: InitRequest = InitRequest.Builder(configParams.appKey)
                .withLogLevel(LogLevel.VERBOSE)
                .withLegacyAdFormats(
                    listOf(
                        IronSourceAds.AdFormat.INTERSTITIAL,
                        IronSourceAds.AdFormat.REWARDED,
                        IronSourceAds.AdFormat.BANNER
                    )
                ).build()

            IronSourceAds.init(
                context = context.applicationContext,
                initRequest = initRequest,
                initializationListener =
                object : InitListener {
                    override fun onInitSuccess() {
                        logInfo(TAG, "IronSource SDK initialized successfully")
                        continuation.resumeWith(Result.success(Unit))
                    }

                    override fun onInitFailed(error: IronSourceError) {
                        logInfo(TAG, "IronSource SDK initialization failed: $error")
                        continuation.resumeWith(Result.failure(Exception("IronSource SDK initialization failed: $error")))
                    }
                }
            )
        }

    // https://developers.is.com/ironsource-mobile/android/regulation-advanced-settings/
    override fun updateRegulation(regulation: Regulation) {
        // GDPR – Managing Consent
        if (regulation.gdprApplies) {
            IronSource.setConsent(regulation.hasGdprConsent)
        }

        // US Privacy compliance
        if (regulation.ccpaApplies) {
            // we invert the value because IronSource expects `true` to indicate that the user
            // has opted out of “sale” or “sharing” of personal information
            val isDoNotSell: Boolean = regulation.hasCcpaConsent.not()
            IronSource.setMetaData("do_not_sell", isDoNotSell.toString())
        }

        // User-Level Settings for Child-Directed Apps with Age Gates
        val isAgeRestrictedUser: Boolean = regulation.coppaApplies
        IronSource.setMetaData("is_deviceid_optout", isAgeRestrictedUser.toString())
        IronSource.setMetaData("is_child_directed", isAgeRestrictedUser.toString())
    }

    override fun interstitial(): AdSource.Interstitial<IronSourceFullscreenAuctionParams> {
        return IronSourceInterstitialImpl()
    }

    override fun rewarded(): AdSource.Rewarded<IronSourceFullscreenAuctionParams> {
        return IronSourceRewardedAdImpl()
    }

    override fun banner(): AdSource.Banner<IronSourceBannerAuctionParams> {
        return IronSourceBannerImpl()
    }
}

private const val TAG = "IronSourceAdapter"
