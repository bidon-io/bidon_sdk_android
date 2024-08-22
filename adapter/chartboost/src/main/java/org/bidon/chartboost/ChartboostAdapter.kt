package org.bidon.chartboost

import android.content.Context
import com.chartboost.sdk.Chartboost
import com.chartboost.sdk.LoggingLevel
import com.chartboost.sdk.events.StartError
import com.chartboost.sdk.privacy.model.CCPA
import com.chartboost.sdk.privacy.model.CCPA.CCPA_CONSENT
import com.chartboost.sdk.privacy.model.COPPA
import com.chartboost.sdk.privacy.model.GDPR
import com.chartboost.sdk.privacy.model.GDPR.GDPR_CONSENT
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bidon.chartboost.ext.adapterVersion
import org.bidon.chartboost.ext.sdkVersion
import org.bidon.chartboost.impl.ChartboostBannerAuctionParams
import org.bidon.chartboost.impl.ChartboostBannerImpl
import org.bidon.chartboost.impl.ChartboostFullscreenAuctionParams
import org.bidon.chartboost.impl.ChartboostInterstitialImpl
import org.bidon.chartboost.impl.ChartboostRewardedAdImpl
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
import kotlin.coroutines.resume

internal val ChartboostDemandId = DemandId("chartboost")

@Suppress("unused")
internal class ChartboostAdapter :
    Adapter.Network,
    Initializable<ChartboostParams>,
    SupportsRegulation,
    SupportsTestMode by SupportsTestModeImpl(),
    AdProvider.Banner<ChartboostBannerAuctionParams>,
    AdProvider.Interstitial<ChartboostFullscreenAuctionParams>,
    AdProvider.Rewarded<ChartboostFullscreenAuctionParams> {

    private var context: Context? = null

    override val demandId: DemandId = ChartboostDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override fun parseConfigParam(json: String): ChartboostParams {
        val jsonObject = JSONObject(json)
        return ChartboostParams(
            appId = jsonObject.getString("app_id"),
            appSignature = jsonObject.getString("app_signature")
        )
    }

    override suspend fun init(context: Context, configParams: ChartboostParams) =
        suspendCancellableCoroutine { continuation ->
            this.context = context.applicationContext

            // Test mode should be enabled from UI (https://answers.chartboost.com/en-us/articles/200780549)
            // Here we set only logging level
            if (isTestMode) {
                Chartboost.setLoggingLevel(LoggingLevel.ALL)
            }
            Chartboost.startWithAppId(
                context = context,
                appId = configParams.appId,
                appSignature = configParams.appSignature,
                onStarted = { error: StartError? ->
                    if (error == null) {
                        logInfo(TAG, "Chartboost SDK initialized successfully")
                        continuation.resume(Unit)
                    } else {
                        logInfo(TAG, "Chartboost SDK initialization failed $error")
                        continuation.resumeWith(Result.failure(Exception("Chartboost SDK initialization failed: $error")))
                    }
                }
            )
        }

    override fun updateRegulation(regulation: Regulation) {
        val context = context ?: return
        if (regulation.gdprApplies) {
            Chartboost.addDataUseConsent(
                context = context,
                dataUseConsent = GDPR(if (regulation.hasGdprConsent) GDPR_CONSENT.BEHAVIORAL else GDPR_CONSENT.NON_BEHAVIORAL)
            )
        }

        if (regulation.ccpaApplies) {
            Chartboost.addDataUseConsent(
                context = context,
                dataUseConsent = CCPA(if (regulation.hasCcpaConsent) CCPA_CONSENT.OPT_IN_SALE else CCPA_CONSENT.OPT_OUT_SALE)
            )
        }

        Chartboost.addDataUseConsent(
            context = context,
            dataUseConsent = COPPA(regulation.coppaApplies)
        )
    }

    override fun banner(): AdSource.Banner<ChartboostBannerAuctionParams> {
        return ChartboostBannerImpl()
    }

    override fun interstitial(): AdSource.Interstitial<ChartboostFullscreenAuctionParams> {
        return ChartboostInterstitialImpl()
    }

    override fun rewarded(): AdSource.Rewarded<ChartboostFullscreenAuctionParams> {
        return ChartboostRewardedAdImpl()
    }
}

private const val TAG = "ChartboostAdapter"