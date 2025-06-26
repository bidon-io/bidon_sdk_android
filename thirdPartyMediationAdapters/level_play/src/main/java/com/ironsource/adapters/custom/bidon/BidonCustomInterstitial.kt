package com.ironsource.adapters.custom.bidon

import android.app.Activity
import com.ironsource.adapters.custom.bidon.ext.ACTIVITY_IS_NULL_ERROR
import com.ironsource.adapters.custom.bidon.ext.AD_IS_NULL_ERROR
import com.ironsource.adapters.custom.bidon.ext.AD_NOT_READY_ERROR
import com.ironsource.adapters.custom.bidon.ext.BIDON_SHOW_ERROR
import com.ironsource.adapters.custom.bidon.ext.NO_FILL_ERROR
import com.ironsource.adapters.custom.bidon.ext.asLevelPlayAdapterError
import com.ironsource.adapters.custom.bidon.ext.getErrorCode
import com.ironsource.adapters.custom.bidon.interstitial.InterstitialAdInstance
import com.ironsource.adapters.custom.bidon.keeper.AdKeeper
import com.ironsource.adapters.custom.bidon.keeper.AdKeepers
import com.ironsource.adapters.custom.bidon.logger.LevelPLaySdkLogger
import com.ironsource.adapters.custom.bidon.logger.Logger
import com.ironsource.mediationsdk.adunit.adapter.BaseInterstitial
import com.ironsource.mediationsdk.adunit.adapter.listener.InterstitialAdListener
import com.ironsource.mediationsdk.adunit.adapter.utility.AdData
import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import com.ironsource.mediationsdk.model.NetworkSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.AuctionInfo
import org.bidon.sdk.ads.interstitial.InterstitialListener
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue
import java.lang.ref.WeakReference

internal class BidonCustomInterstitial(
    networkSetting: NetworkSettings
) : BaseInterstitial<BidonCustomAdapter>(networkSetting), Logger by LevelPLaySdkLogger {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var weakActivity: WeakReference<Activity>? = null
    private var adInstance: InterstitialAdInstance? = null

    private var instanceName: String = "UNDEFINED"
    private var lpEcpm: Double = 0.0

    init {
        log(TAG, "Create instance $this")
    }

    override fun loadAd(adData: AdData, activity: Activity, listener: InterstitialAdListener) {
        this.weakActivity = WeakReference(activity)
        val configuration = adData.configuration
        val adUnitData = adData.adUnitData

        val adUnitId = adUnitData["adUnitId"] as? String ?: run {
            scope.launch {
                listener.onAdLoadFailed(
                    AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL,
                    NO_FILL_ERROR,
                    "adUnitId is empty"
                )
            }
            onDestroy()
            return
        }
        log(TAG, "Loading interstitial ad for adUnitId: $adUnitId")
        // Get the appropriate keeper for the ad format
        val adKeeper = AdKeepers.getInterstitialKeeper(adUnitId)

        instanceName = configuration["instanceName"] as? String ?: "UNDEFINED"
        lpEcpm = (configuration["price"] as? String)?.toDoubleOrNull() ?: 0.0

        // Get last registered ecpm
        val lastRegisteredEcpm = adKeeper.lastRegisteredEcpm()
        // Register ecpm for range calculation
        adKeeper.registerEcpm(lpEcpm)

        val shouldLoad = (configuration["should_load"] as? String)?.toBooleanStrictOrNull() == true
        if (shouldLoad) {
            log(
                TAG,
                "AdUnitId: $adUnitId, instanceName: $instanceName, shouldLoad = true detected, Placement ECPM: $lpEcpm"
            )

            val auctionKey = configuration["auctionKey"] as? String?
            log(
                TAG,
                "Loading interstitial ad for auction key: $auctionKey, pricefloor: ${0.0}, InstanceName: $instanceName"
            )

            val newAdInstance = InterstitialAdInstance(auctionKey = auctionKey)
                .also { this.adInstance = it }
            newAdInstance.setListener(listener.asBidonListener(adKeeper))
            newAdInstance.addExtra("previous_auction_price", lastRegisteredEcpm)
            newAdInstance.load(activity)
        } else {
            log(
                TAG,
                "AdUnitId: $adUnitId, instanceName: $instanceName, shouldLoad = false detected, Placement ECPM: $lpEcpm"
            )
            val consumeAdInstance = adKeeper.consumeAd(ecpm = lpEcpm)
                .also { this.adInstance = it }
            if (consumeAdInstance == null) {
                val message =
                    "Interstitial ad failed to load from cache: ECPM ISN'T SUITABLE, InstanceName: $instanceName"
                log(TAG, message)
                scope.launch {
                    listener.onAdLoadFailed(
                        AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL,
                        NO_FILL_ERROR,
                        message
                    )
                }
                onDestroy()
            } else {
                log(
                    TAG,
                    "Interstitial ad loaded $consumeAdInstance from cache, InstanceName: $instanceName"
                )
                consumeAdInstance.setListener(listener.asBidonListener(adKeeper))
                scope.launch {
                    listener.onAdLoadSuccess()
                }
            }
        }
    }

    override fun showAd(adData: AdData, listener: InterstitialAdListener) {
        val adUnitId = adData.adUnitData["adUnitId"] as? String ?: "UNDEFINED"
        log(
            TAG,
            "Showing interstitial ad: AdUnitId: $adUnitId, adInstance: $adInstance, InstanceName: $instanceName"
        )
        val adInstance = adInstance
        val activity = this.weakActivity?.get()
        if (adInstance == null) {
            val message = "Interstitial ad display failed: Ad is null, InstanceName: $instanceName"
            log(TAG, message)
            listener.onAdShowFailed(AD_IS_NULL_ERROR, message)
            onDestroy()
        } else if (!adInstance.isReady) {
            val message =
                "Interstitial ad display failed: Ad is not ready, InstanceName: $instanceName"
            log(TAG, message)
            listener.onAdShowFailed(AD_NOT_READY_ERROR, message)
            onDestroy()
        } else if (activity == null) {
            val message =
                "Interstitial ad display failed: Activity is null, InstanceName: $instanceName"
            log(TAG, message)
            listener.onAdShowFailed(ACTIVITY_IS_NULL_ERROR, message)
            onDestroy()
        } else {
            adInstance.show(activity)
        }
    }

    private fun onDestroy() {
        val message = "Destroying interstitial ad: $adInstance, InstanceName: $instanceName"
        log(TAG, message)
        adInstance?.destroy()
        adInstance = null
        weakActivity?.clear()
        weakActivity = null
    }

    override fun isAdAvailable(adData: AdData) = adInstance?.isReady == true

    private fun InterstitialAdListener.asBidonListener(adKeeper: AdKeeper<InterstitialAdInstance>): InterstitialListener {
        val levelPlayInterstitialCallback = this
        return object : InterstitialListener {
            override fun onAdLoaded(ad: Ad, auctionInfo: AuctionInfo) {
                val loadedAdInstance = adInstance
                if (loadedAdInstance == null) {
                    val message =
                        "Interstitial ad failed to load: Ad is null, InstanceName: $instanceName"
                    log(TAG, message)
                    levelPlayInterstitialCallback.onAdLoadFailed(
                        AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL,
                        NO_FILL_ERROR,
                        message
                    )
                    onDestroy()
                } else {
                    log(
                        TAG,
                        "Interstitial ad try to keep, InstanceName: $instanceName, ECPM: ${ad.price}"
                    )
                    // Apply ad info to the loaded ad instance
                    // Keep the loaded ad instance
                    val rejectedAdInstance =
                        adKeeper.keepAd(adInstance = loadedAdInstance.applyAdInfo(ad))
                    rejectedAdInstance?.destroy()

                    // Consume the ad instance
                    val consumeAdInstance = adKeeper.consumeAd(ecpm = lpEcpm)
                        .also { adInstance = it }
                    if (consumeAdInstance == null) {
                        val message =
                            "Interstitial ad failed to load from cache: ECPM ISN'T SUITABLE, InstanceName: $instanceName"
                        log(TAG, message)
                        levelPlayInterstitialCallback.onAdLoadFailed(
                            AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL,
                            NO_FILL_ERROR,
                            message
                        )
                        onDestroy()
                    } else {
                        log(
                            TAG,
                            "Interstitial ad loaded $consumeAdInstance from cache, InstanceName: $instanceName"
                        )
                        consumeAdInstance.setListener(this)
                        levelPlayInterstitialCallback.onAdLoadSuccess()
                    }
                }
            }

            override fun onAdLoadFailed(auctionInfo: AuctionInfo?, cause: BidonError) {
                log(TAG, "Interstitial ad failed to load: $cause, InstanceName: $instanceName")
                val adapterErrorType = cause.asLevelPlayAdapterError()
                levelPlayInterstitialCallback.onAdLoadFailed(
                    adapterErrorType,
                    adapterErrorType.getErrorCode(),
                    cause.message ?: "Unknown error"
                )
                onDestroy()
            }

            override fun onAdShown(ad: Ad) {
                log(TAG, "Interstitial ad shown, InstanceName: $instanceName, ECPM: ${ad.price}")
                levelPlayInterstitialCallback.onAdOpened()
                levelPlayInterstitialCallback.onAdShowSuccess()
            }

            override fun onAdShowFailed(cause: BidonError) {
                log(TAG, "Interstitial ad failed to show: $cause, InstanceName: $instanceName")
                levelPlayInterstitialCallback.onAdShowFailed(BIDON_SHOW_ERROR, cause.message)
                onDestroy()
            }

            override fun onAdClicked(ad: Ad) {
                log(TAG, "Interstitial ad clicked, InstanceName: $instanceName, ECPM: ${ad.price}")
                levelPlayInterstitialCallback.onAdClicked()
            }

            override fun onAdClosed(ad: Ad) {
                log(TAG, "Interstitial ad closed, InstanceName: $instanceName, ECPM: ${ad.price}")
                levelPlayInterstitialCallback.onAdClosed()
                onDestroy()
            }

            override fun onAdExpired(ad: Ad) {
                log(TAG, "Interstitial ad expired, InstanceName: $instanceName, ECPM: ${ad.price}")
                onDestroy()
            }

            override fun onRevenuePaid(ad: Ad, adValue: AdValue) {
                log(TAG, "Interstitial ad revenue paid: $adValue, InstanceName: $instanceName, ECPM: ${ad.price}")
            }
        }
    }
}

private const val TAG = "BidonCustomInterstitial"
