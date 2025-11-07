package org.bidon.applovin

import android.content.Context
import com.applovin.sdk.AppLovinPrivacySettings
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkInitializationConfiguration
import org.bidon.applovin.ext.adapterVersion
import org.bidon.applovin.ext.sdkVersion
import org.bidon.applovin.impl.ApplovinBannerImpl
import org.bidon.applovin.impl.ApplovinInterstitialImpl
import org.bidon.applovin.impl.ApplovinRewardedImpl
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.adapter.AdProvider
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.adapter.AdapterInfo
import org.bidon.sdk.adapter.DemandId
import org.bidon.sdk.adapter.Initializable
import org.bidon.sdk.adapter.SupportsRegulation
import org.bidon.sdk.logs.logging.Logger
import org.bidon.sdk.regulation.Regulation
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal val ApplovinDemandId = DemandId("applovin")

@Suppress("unused")
internal class ApplovinAdapter :
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
            val instance = AppLovinSdk.getInstance(context)
                .also { applovinSdk = it }
            instance.settings.setVerboseLogging(BidonSdk.loggerLevel != Logger.Level.Off)
            if (!instance.isInitialized) {
                val initConfigBuilder =
                    AppLovinSdkInitializationConfiguration.builder(configParams.key)
                configParams.mediator?.let { mediator ->
                    initConfigBuilder.setMediationProvider(mediator)
                }
                configParams.adUnitIds?.let { adUnitIds ->
                    initConfigBuilder.setAdUnitIds(adUnitIds)
                }
                val initConfig = initConfigBuilder.build()
                instance.initialize(initConfig) {
                    continuation.resume(Unit)
                }
            } else {
                continuation.resume(Unit)
            }
        }

    override fun parseConfigParam(json: String): ApplovinParameters {
        val jsonObject = JSONObject(json)
        val adUnitIds = buildList {
            jsonObject.optJSONArray("ad_unit_ids")?.let { jsonArray ->
                repeat(jsonArray.length()) { index ->
                    runCatching {
                        add(jsonArray.optString(index))
                    }
                }
            }
        }
        return ApplovinParameters(
            key = jsonObject.getString("app_key"),
            mediator = jsonObject.optString("mediator").takeIf { it.isNotEmpty() },
            adUnitIds = adUnitIds.takeIf { it.isNotEmpty() },
        )
    }

    override fun updateRegulation(regulation: Regulation) {
        if (regulation.gdprApplies) {
            AppLovinPrivacySettings.setHasUserConsent(regulation.hasGdprConsent)
        }
        if (regulation.ccpaApplies) {
            AppLovinPrivacySettings.setDoNotSell(!regulation.hasCcpaConsent)
        }
    }

    override fun interstitial(): AdSource.Interstitial<ApplovinFullscreenAdAuctionParams> {
        return ApplovinInterstitialImpl(applovinSdk = requireNotNull(applovinSdk))
    }

    override fun rewarded(): AdSource.Rewarded<ApplovinFullscreenAdAuctionParams> {
        return ApplovinRewardedImpl()
    }

    override fun banner(): AdSource.Banner<ApplovinBannerAuctionParams> {
        return ApplovinBannerImpl()
    }
}