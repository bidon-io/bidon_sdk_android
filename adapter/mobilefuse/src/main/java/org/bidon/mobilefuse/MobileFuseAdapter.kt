package org.bidon.mobilefuse

import android.content.Context
import com.mobilefuse.sdk.MobileFuse
import com.mobilefuse.sdk.MobileFuseSettings
import com.mobilefuse.sdk.SdkInitListener
import com.mobilefuse.sdk.privacy.MobileFusePrivacyPreferences
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bidon.mobilefuse.ext.adapterVersion
import org.bidon.mobilefuse.ext.sdkVersion
import org.bidon.mobilefuse.impl.MobileFuseInterstitialImpl
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.adapter.SupportsRegulation
import org.bidon.sdk.adapter.SupportsTestMode
import org.bidon.sdk.adapter.impl.SupportsTestModeImpl
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.regulation.Regulation
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Created by Aleksei Cherniaev on 06/07/2023.
 */
val MobileFuseDemandId = DemandId("mobilefuse")

/**
 * [MobileFuse Documentation](https://docs.mobilefuse.com/docs/android-interstitial-ads)
 */
class MobileFuseAdapter :
    Adapter,
    Initializable<MobileFuseParams>,
    SupportsRegulation,
    SupportsTestMode by SupportsTestModeImpl(),
    AdProvider.Interstitial<MobileFuseAuctionParams> {
    override val demandId: DemandId = MobileFuseDemandId
    override val adapterInfo: AdapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(context: Context, configParams: MobileFuseParams) = suspendCancellableCoroutine { continuation ->
        MobileFuseSettings.setTestMode(isTestMode)
        MobileFuse.initSdkServices(context)
        MobileFuse.init(
            context, configParams.publisherId, configParams.appId,
            object : SdkInitListener {
                override fun onInitSuccess() {
                    return continuation.resume(Unit)
                }

                override fun onInitError() {
                    return continuation.resumeWithException(BidonError.Unspecified(demandId, Throwable("Error while initialization")))
                }
            }
        )
    }

    override fun parseConfigParam(json: String): MobileFuseParams {
        val jsonObject = JSONObject(json)
        return MobileFuseParams(
            publisherId = jsonObject.getInt("publisher_id"),
            appId = jsonObject.getInt("app_id")
        )
    }

    override fun updateRegulation(regulation: Regulation) {
        val prefs = MobileFusePrivacyPreferences.Builder()
            .setSubjectToCoppa(regulation.coppaApplies)
            .setSubjectToGdpr(regulation.gdprConsent)
            .setGppConsentString(regulation.gdprConsentString)
            .setUsPrivacyConsentString(regulation.usPrivacyString)
            .build()
        MobileFuse.setPrivacyPreferences(prefs)
    }

    override fun interstitial(
        demandAd: DemandAd,
        roundId: String,
        auctionId: String
    ): AdSource.Interstitial<MobileFuseAuctionParams> {
        return MobileFuseInterstitialImpl(
            demandId = demandId,
            demandAd = demandAd,
            roundId = roundId,
            auctionId = auctionId,
            isTestMode = isTestMode
        )
    }
}