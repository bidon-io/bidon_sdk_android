package org.bidon.startio

import android.annotation.SuppressLint
import android.content.Context
import com.startapp.sdk.adsbase.StartAppSDK
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource.Banner
import org.bidon.sdk.adapter.AdSource.Interstitial
import org.bidon.sdk.adapter.AdSource.Rewarded
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.adapter.SupportsRegulation
import org.bidon.sdk.adapter.SupportsTestMode
import org.bidon.sdk.adapter.impl.SupportsTestModeImpl
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.regulation.Regulation
import org.bidon.startio.impl.StartIoBannerAuctionParams
import org.bidon.startio.impl.StartIoBannerImpl
import org.bidon.startio.impl.StartIoFullscreenAuctionParams
import org.bidon.startio.impl.StartIoInterstitialImpl
import org.bidon.startio.impl.StartIoRewardedImpl
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal val StartIoDemandId = DemandId("startio")

internal class StartIoAdapter :
    Adapter.Bidding,
    Initializable<StartIoParams>,
    SupportsRegulation,
    SupportsTestMode by SupportsTestModeImpl(),
    AdProvider.Banner<StartIoBannerAuctionParams>,
    AdProvider.Interstitial<StartIoFullscreenAuctionParams>,
    AdProvider.Rewarded<StartIoFullscreenAuctionParams> {

    private val isInitialized = AtomicBoolean(false)
    private var context: Context? = null

    override suspend fun getToken(adTypeParam: AdTypeParam) = StartAppSDK.getBidToken()

    override val demandId: DemandId = StartIoDemandId

    override val adapterInfo: AdapterInfo = AdapterInfo(
        adapterVersion = BuildConfig.ADAPTER_VERSION,
        sdkVersion = StartAppSDK.getVersion()
    )

    override suspend fun init(
        context: Context,
        configParams: StartIoParams
    ) = suspendCoroutine { continuation ->
        this.context = context

        if (isInitialized.get()) {
            logInfo(TAG, "StartIo SDK already initialized")
            continuation.resume(Unit)
            return@suspendCoroutine
        }

        if (configParams.appId.isBlank()) {
            val errorMessage = "Adapter(${StartIoDemandId.demandId}) app id is empty or blank"
            val error = IllegalArgumentException(errorMessage)
            logError(TAG, errorMessage, error)
            continuation.resumeWithException(error)
            return@suspendCoroutine
        }

        if (isTestMode) {
            StartAppSDK.setTestAdsEnabled(true)
        }

        StartAppSDK
            .initParams(context, configParams.appId)
            .setCallback {
                isInitialized.set(true)
                continuation.resume(Unit)
            }
            .init()
    }

    override fun parseConfigParam(json: String): StartIoParams {
        val jsonObject = JSONObject(json)
        return StartIoParams(
            appId = jsonObject.optString("app_id"),
        )
    }

    override fun updateRegulation(regulation: Regulation) {
        context?.let { context ->
            if (regulation.gdprApplies) {
                StartAppSDK.setUserConsent(
                    /* context = */ context,
                    /* consentType = */ "pas",
                    /* timestamp = */ System.currentTimeMillis(),
                    /* enabled = */ regulation.hasGdprConsent
                )
            }
            if (regulation.ccpaApplies) {
                @SuppressLint("UseKtx")
                StartAppSDK.getExtras(context)
                    .edit()
                    .putString("IABUSPrivacy_String", regulation.usPrivacyString)
                    .apply()
            }
        }
    }

    override fun banner(): Banner<StartIoBannerAuctionParams> = StartIoBannerImpl()
    override fun interstitial(): Interstitial<StartIoFullscreenAuctionParams> = StartIoInterstitialImpl()
    override fun rewarded(): Rewarded<StartIoFullscreenAuctionParams> = StartIoRewardedImpl()
}

private const val TAG = "StartIoAdapter"
