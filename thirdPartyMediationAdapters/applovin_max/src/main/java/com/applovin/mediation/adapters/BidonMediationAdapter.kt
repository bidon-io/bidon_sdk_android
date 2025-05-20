package com.applovin.mediation.adapters

import android.app.Activity
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.adapter.MaxAdViewAdapter
import com.applovin.mediation.adapter.MaxAdapter
import com.applovin.mediation.adapter.MaxAdapter.InitializationStatus.INITIALIZED_FAILURE
import com.applovin.mediation.adapter.MaxAdapter.InitializationStatus.INITIALIZED_SUCCESS
import com.applovin.mediation.adapter.MaxInterstitialAdapter
import com.applovin.mediation.adapter.MaxRewardedAdapter
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters
import com.applovin.mediation.adapters.banner.BidonBanner
import com.applovin.mediation.adapters.ext.updatePrivacySettings
import com.applovin.mediation.adapters.interstitial.BidonInterstitial
import com.applovin.mediation.adapters.rewarded.BidonRewarded
import com.applovin.sdk.AppLovinSdk
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.logs.logging.Logger

@Suppress("unused")
internal class BidonMediationAdapter(
    sdk: AppLovinSdk
) : MediationAdapterBase(sdk),
    MaxInterstitialAdapter by BidonInterstitial(),
    MaxAdViewAdapter by BidonBanner(),
    MaxRewardedAdapter by BidonRewarded() {

    override fun getSdkVersion(): String = BidonSdk.SdkVersion
    override fun getAdapterVersion(): String = BuildConfig.ADAPTER_VERSION

    override fun shouldInitializeOnUiThread(): Boolean = false
    override fun shouldLoadAdsOnUiThread(adFormat: MaxAdFormat?): Boolean = false

    override fun initialize(
        parameters: MaxAdapterInitializationParameters,
        activity: Activity?,
        onCompletionListener: MaxAdapter.OnCompletionListener
    ) {
        BidonSdk.updatePrivacySettings(parameters)

        val appKey = parameters.serverParameters.getString("app_id")
        if (BidonSdk.isInitialized()) {
            log("Bidon SDK already initialized")
            onCompletionListener.onCompletion(INITIALIZED_SUCCESS, null)
        } else if (appKey.isNullOrEmpty()) {
            log("Bidon SDK initialization failed: Missing app key")
            onCompletionListener.onCompletion(INITIALIZED_FAILURE, "Missing app key")
        } else {
            val context = activity?.applicationContext ?: applicationContext
            val isTesting = parameters.isTesting

            log("Bidon SDK initializing with app key:$appKey, test mode: $isTesting, context: $context")
            BidonSdk
                .setBaseUrl("https://b.appbaqend.com")
                .registerDefaultAdapters()
                .setTestMode(isTesting)
                .setLoggerLevel(Logger.Level.Verbose)
                .setInitializationCallback {
                    log("Bidon SDK initialized successfully")
                    onCompletionListener.onCompletion(INITIALIZED_SUCCESS, null)
                }
                .initialize(context = context, appKey = appKey)
        }
    }

    override fun onDestroy() {
        // No-op
    }
}
