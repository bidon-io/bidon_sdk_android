package org.bidon.inmobi

import android.content.Context
import com.inmobi.sdk.InMobiSdk
import com.inmobi.sdk.SdkInitializationListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bidon.inmobi.ext.adapterVersion
import org.bidon.inmobi.ext.sdkVersion
import org.bidon.inmobi.impl.InmobiBannerAuctionParams
import org.bidon.inmobi.impl.InmobiBannerImpl
import org.bidon.inmobi.impl.InmobiFullscreenAuctionParams
import org.bidon.inmobi.impl.InmobiInterstitialImpl
import org.bidon.inmobi.impl.InmobiRewardedImpl
import org.bidon.sdk.BidonSdk
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
import org.bidon.sdk.regulation.Gdpr
import org.bidon.sdk.regulation.Regulation
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Created by Aleksei Cherniaev on 11/09/2023.
 *
 * [Documentation](https://support.inmobi.com/monetize/sdk-documentation/android-guidelines/overview-android-guidelines#add-the-inmobi-sdk)
 */
internal val InmobiDemandId = DemandId("inmobi")

class InmobiAdapter :
    Adapter,
    Initializable<InmobiParams>,
    SupportsRegulation,
    SupportsTestMode by SupportsTestModeImpl(),
    AdProvider.Banner<InmobiBannerAuctionParams>,
    AdProvider.Interstitial<InmobiFullscreenAuctionParams>,
    AdProvider.Rewarded<InmobiFullscreenAuctionParams> {

    override val demandId: DemandId = InmobiDemandId
    override val adapterInfo: AdapterInfo
        get() = AdapterInfo(
            adapterVersion = adapterVersion,
            sdkVersion = sdkVersion
        )

    override suspend fun init(context: Context, configParams: InmobiParams) = withContext(Dispatchers.Main.immediate) {
        suspendCoroutine {
            if (isTestMode) {
                InMobiSdk.setLogLevel(InMobiSdk.LogLevel.DEBUG)
            }
            val consentObject = getConsentObject(BidonSdk.regulation)
            InMobiSdk.init(
                context, configParams.accountId, consentObject,
                object : SdkInitializationListener {
                    override fun onInitializationComplete(error: Error?) {
                        if (null != error) {
                            logError(TAG, "InMobi Init Failed", error)
                            it.resumeWithException(BidonError.SdkNotInitialized)
                        } else {
                            it.resume(Unit)
                        }
                    }
                }
            )
        }
    }

    override fun parseConfigParam(json: String): InmobiParams {
        return InmobiParams(JSONObject(json).optString("account_id"))
    }

    override fun updateRegulation(regulation: Regulation) {
        val consentObject = getConsentObject(regulation)
        InMobiSdk.updateGDPRConsent(consentObject)
        if (regulation.coppaApplies) {
            InMobiSdk.setIsAgeRestricted(true)
        }
    }

    override fun interstitial(): AdSource.Interstitial<InmobiFullscreenAuctionParams> {
        return InmobiInterstitialImpl()
    }

    override fun banner(): AdSource.Banner<InmobiBannerAuctionParams> {
        return InmobiBannerImpl()
    }

    override fun rewarded(): AdSource.Rewarded<InmobiFullscreenAuctionParams> {
        return InmobiRewardedImpl()
    }

    private fun getConsentObject(regulation: Regulation): JSONObject {
        return JSONObject().apply {
            if (regulation.gdpr != Gdpr.Unknown) {
                this.put("gdpr", regulation.gdpr.code)
            }
            if (regulation.gdprApplies) {
                this.put(InMobiSdk.IM_GDPR_CONSENT_AVAILABLE, regulation.hasGdprConsent)
                this.put(InMobiSdk.IM_GDPR_CONSENT_IAB, regulation.gdprConsentString)
            }
        }
    }
}

private const val TAG = "InmobiAdapter"