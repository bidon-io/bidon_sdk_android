package org.bidon.applovin

import android.content.Context
import com.applovin.sdk.AppLovinPrivacySettings
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkSettings
import org.bidon.applovin.ext.adapterVersion
import org.bidon.applovin.ext.sdkVersion
import org.bidon.applovin.impl.ApplovinBannerImpl
import org.bidon.applovin.impl.ApplovinInterstitialImpl
import org.bidon.applovin.impl.ApplovinRewardedImpl
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.*
import org.bidon.sdk.logs.logging.Logger
import org.bidon.sdk.regulation.Regulation
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

val ApplovinDemandId = DemandId("applovin")

@Suppress("unused")
class ApplovinAdapter :
    Adapter.Network,
    SupportsRegulation,
    Initializable<ApplovinParameters>,
    AdProvider.Banner<ApplovinBannerAuctionParams>,
    AdProvider.Interstitial<ApplovinFullscreenAdAuctionParams>,
    AdProvider.Rewarded<ApplovinFullscreenAdAuctionParams> {

    private var applovinSdk: AppLovinSdk? = null
    private var context: Context? = null

    override val demandId: DemandId = ApplovinDemandId
    override val adapterInfo = AdapterInfo(
        adapterVersion = adapterVersion,
        sdkVersion = sdkVersion
    )

    override suspend fun init(context: Context, configParams: ApplovinParameters): Unit =
        suspendCoroutine { continuation ->
            this.context = context
            val instance =
                AppLovinSdk.getInstance(configParams.key, AppLovinSdkSettings(context), context)
                    .also {
                        applovinSdk = it
                    }
            instance.settings.setVerboseLogging(BidonSdk.loggerLevel != Logger.Level.Off)
            if (!instance.isInitialized) {
                instance.initializeSdk {
                    continuation.resume(Unit)
                }
            } else {
                continuation.resume(Unit)
            }
        }

    override fun parseConfigParam(json: String): ApplovinParameters {
        val jsonObject = JSONObject(json)
        return ApplovinParameters(
            key = jsonObject.getString("app_key"),
        )
    }

    override fun updateRegulation(regulation: Regulation) {
        if (regulation.gdprApplies) {
            AppLovinPrivacySettings.setHasUserConsent(regulation.hasGdprConsent, context)
        }
        if (regulation.ccpaApplies) {
            AppLovinPrivacySettings.setDoNotSell(!regulation.hasCcpaConsent, context)
        }
        if (regulation.coppaApplies) {
            AppLovinPrivacySettings.setIsAgeRestrictedUser(true, context)
        }
    }

    override fun interstitial(): AdSource.Interstitial<ApplovinFullscreenAdAuctionParams> {
        return ApplovinInterstitialImpl(
            applovinSdk = requireNotNull(applovinSdk),
        )
    }

    override fun rewarded(): AdSource.Rewarded<ApplovinFullscreenAdAuctionParams> {
        return ApplovinRewardedImpl(
            applovinSdk = requireNotNull(applovinSdk),
        )
    }

    override fun banner(): AdSource.Banner<ApplovinBannerAuctionParams> {
        return ApplovinBannerImpl(
            applovinSdk = requireNotNull(applovinSdk),
        )
    }
}