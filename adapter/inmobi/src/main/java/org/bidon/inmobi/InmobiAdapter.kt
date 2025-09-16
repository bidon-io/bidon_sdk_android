package org.bidon.inmobi

import android.content.Context
import com.inmobi.compliance.InMobiPrivacyCompliance
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
import org.bidon.sdk.auction.AdTypeParam
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

@Suppress("unused")
internal class InmobiAdapter :
    Adapter.Bidding,
    Adapter.Network,
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

    override suspend fun getToken(adTypeParam: AdTypeParam): String? =
        InMobiSdk.getToken(getExtras(), null)

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

    // TODO: 02/09/2025 [glavatskikh] https://appodeal.slack.com/archives/C02PE4GAFU0/p1756807478969089?thread_ts=1754615120.657369&cid=C02PE4GAFU0
    override fun updateRegulation(regulation: Regulation) {
        // GDPR compliance - use both methods for maximum compatibility
        if (regulation.gdprApplies) {
            // Method 1: Direct SDK integration approach with full consent details
            val consentObject = getConsentObject(regulation)
            InMobiSdk.updateGDPRConsent(consentObject)

            // Method 2: Partner/Mediation approach
            val partnerConsentObject = JSONObject().apply {
                put("partner_gdpr_consent_available", regulation.hasGdprConsent)
            }
            InMobiSdk.setPartnerGDPRConsent(partnerConsentObject)
        }

        // CCPA compliance
        if (regulation.ccpaApplies) {
            // Set whether user has opted out of data sale (inverted: true = do not sell)
            InMobiPrivacyCompliance.setDoNotSell(!regulation.hasCcpaConsent)

            // Set US Privacy String if available
            regulation.usPrivacyString?.let {
                InMobiPrivacyCompliance.setUSPrivacyString(it)
            }
        }

        // COPPA compliance
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

    companion object {
        fun getExtras(): Map<String, String> {
            return mapOf(
                "tp" to "c_bidon",
                "tp-ver" to BidonSdk.SdkVersion
            )
        }
    }
}

private const val TAG = "InmobiAdapter"