package org.bidon.moloco

import android.content.Context
import com.moloco.sdk.publisher.Initialization
import com.moloco.sdk.publisher.MediationInfo
import com.moloco.sdk.publisher.Moloco
import com.moloco.sdk.publisher.MolocoAdError
import com.moloco.sdk.publisher.init.MolocoInitParams
import com.moloco.sdk.publisher.privacy.MolocoPrivacy
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bidon.moloco.impl.MolocoBannerAuctionParams
import org.bidon.moloco.impl.MolocoBannerImpl
import org.bidon.moloco.impl.MolocoFullscreenAuctionParams
import org.bidon.moloco.impl.MolocoInterstitialImpl
import org.bidon.moloco.impl.MolocoRewardedImpl
import org.bidon.sdk.adapter.AdProvider
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
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import com.moloco.sdk.BuildConfig as MolocoSdkBuildConfig
import org.bidon.moloco.BuildConfig as MolocoAdapterBuildConfig

private const val TAG = "MolocoAdapter"
internal val MolocoDemandId = DemandId("moloco")
internal const val EMPTY_MEDIATOR = ""
internal const val EMPTY_WATERMARK = ""

internal class MolocoAdapter :
    Adapter.Bidding,
    Initializable<MolocoParams>,
    SupportsRegulation,
    SupportsTestMode by SupportsTestModeImpl(),
    AdProvider.Banner<MolocoBannerAuctionParams>,
    AdProvider.Interstitial<MolocoFullscreenAuctionParams>,
    AdProvider.Rewarded<MolocoFullscreenAuctionParams> {

    override val demandId: DemandId = MolocoDemandId

    override val adapterInfo: AdapterInfo = AdapterInfo(
        adapterVersion = MolocoAdapterBuildConfig.ADAPTER_VERSION,
        sdkVersion = MolocoSdkBuildConfig.SDK_VERSION_NAME
    )

    override suspend fun getToken(adTypeParam: AdTypeParam) =
        suspendCancellableCoroutine { continuation ->
            logInfo(TAG, "Requesting bid token")
            Moloco.getBidToken(
                adTypeParam.activity.applicationContext
            ) { bidToken: String, error: MolocoAdError.ErrorType? ->
                if (error != null) {
                    logError(
                        tag = TAG,
                        message = "Failed to get bid token: ${error.name} - ${error.description} (code: ${error.errorCode})",
                        error = null
                    )
                }
                continuation.resume(bidToken)
            }
        }

    override suspend fun init(
        context: Context,
        configParams: MolocoParams
    ) = suspendCoroutine { continuation ->
        if (Moloco.isInitialized) {
            logInfo(TAG, "Moloco SDK already initialized")
            continuation.resume(Unit)
            return@suspendCoroutine
        }

        if (configParams.appKey.isBlank()) {
            val errorMessage = "Adapter(${MolocoDemandId.demandId}) app key is empty or blank"
            val error = IllegalArgumentException(errorMessage)
            logError(TAG, errorMessage, error)
            continuation.resumeWithException(error)
            return@suspendCoroutine
        }

        val initParams = MolocoInitParams(
            appContext = context,
            appKey = configParams.appKey,
            mediationInfo = MediationInfo(EMPTY_MEDIATOR)
        )

        Moloco.initialize(initParams) { status ->
            when (status.initialization) {
                Initialization.SUCCESS -> {
                    continuation.resume(Unit)
                }

                Initialization.FAILURE -> {
                    val errorMessage = "Moloco SDK initialization failed: ${status.description}"
                    val error = Exception(errorMessage)
                    logError(TAG, errorMessage, error)
                    continuation.resumeWithException(error)
                }
            }
        }
    }

    override fun parseConfigParam(json: String): MolocoParams {
        val jsonObject = JSONObject(json)
        return MolocoParams(
            appKey = jsonObject.optString("app_key"),
        )
    }

    override fun updateRegulation(regulation: Regulation) {
        if (regulation.gdprApplies) {
            MolocoPrivacy.setPrivacy(
                MolocoPrivacy.PrivacySettings(
                    isUserConsent = regulation.hasGdprConsent,
                )
            )
        }
        if (regulation.ccpaApplies) {
            MolocoPrivacy.setPrivacy(
                MolocoPrivacy.PrivacySettings(
                    isDoNotSell = !regulation.hasCcpaConsent
                )
            )
        }
        MolocoPrivacy.setPrivacy(
            MolocoPrivacy.PrivacySettings(
                isAgeRestrictedUser = regulation.coppaApplies,
            )
        )
    }

    override fun banner() = MolocoBannerImpl()
    override fun interstitial() = MolocoInterstitialImpl()
    override fun rewarded() = MolocoRewardedImpl()
}
