package com.ironsource.adapters.custom.bidon

import android.content.Context
import com.ironsource.adapters.custom.bidon.ext.MISSING_APP_KEY_ERROR
import com.ironsource.adapters.custom.bidon.logger.LevelPLaySdkLogger
import com.ironsource.adapters.custom.bidon.logger.Logger
import com.ironsource.mediationsdk.AdapterNetworkData
import com.ironsource.mediationsdk.adunit.adapter.BaseAdapter
import com.ironsource.mediationsdk.adunit.adapter.listener.NetworkInitializationListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.logs.logging.Logger.Level.Off
import org.bidon.sdk.logs.logging.Logger.Level.Verbose
import org.bidon.sdk.regulation.Coppa
import org.bidon.sdk.regulation.Gdpr

public class BidonCustomAdapter : BaseAdapter(), Logger by LevelPLaySdkLogger {

    override fun getNetworkSDKVersion(): String = BidonSdk.SdkVersion
    override fun getAdapterVersion(): String = "${BidonSdk.SdkVersion}.${BuildConfig.ADAPTER_VERSION}"

    override fun init(
        adData: AdData,
        context: Context,
        initListener: NetworkInitializationListener?
    ) {
        val configuration = adData.configuration
        val appKey = configuration["appKey"] as? String

        if (BidonSdk.isInitialized()) {
            log(TAG, "Bidon SDK already initialized")
            initListener?.onInitSuccess()
        } else if (appKey.isNullOrEmpty()) {
            log(TAG, "Bidon SDK initialization failed: Missing app key")
            initListener?.onInitFailed(
                MISSING_APP_KEY_ERROR,
                "Bidon SDK initialization failed: Missing app key"
            )
        } else {
            log(TAG, "Bidon SDK initializing with app key:$appKey, context: $context")
            BidonSdk
                .setBaseUrl("https://b.appbaqend.com")
                .registerDefaultAdapters()
                .setInitializationCallback {
                    log(TAG, "Bidon SDK initialized successfully")
                    initListener?.onInitSuccess()
                }
                .initialize(context = context, appKey = appKey)
        }
    }

    override fun setAdapterDebug(isDebug: Boolean) {
        super.setAdapterDebug(isDebug)
        log(tag = TAG, message = "setAdapterDebug: $isDebug")
        BidonSdk.setLoggerLevel(if (isDebug) Verbose else Off)
    }

    override fun setNetworkData(networkData: AdapterNetworkData) {
        super.setNetworkData(networkData)
        val data = networkData.allData()
        log(tag = TAG, message = "setNetworkData: $data")
        data.keys().forEach { key ->
            when (key) {
                GDPR_KEY, CCPA_KEY, COPPA_KEY -> {
                    val consentValue = data.get(key)
                    if (consentValue is Boolean) {
                        updateRegulation(key, consentValue)
                    }
                }
            }
        }
    }

    private fun updateRegulation(key: String, value: Boolean) {
        when (key) {
            GDPR_KEY -> BidonSdk.regulation.gdpr = if (value) Gdpr.Applies else Gdpr.DoesNotApply
            CCPA_KEY -> BidonSdk.regulation.usPrivacyString = if (value) US_PRIVACY_CONSENT else US_PRIVACY_NO_CONSENT
            COPPA_KEY -> BidonSdk.regulation.coppa = if (value) Coppa.Yes else Coppa.No
        }
    }
}

private const val TAG = "BidonCustomAdapter"
private const val US_PRIVACY_CONSENT = "1YY-"
private const val US_PRIVACY_NO_CONSENT = "1YN-"

private const val GDPR_KEY = "BidonCA_GDPR"
private const val CCPA_KEY = "BidonCA_CCPA"
private const val COPPA_KEY = "BidonCA_COPPA"
