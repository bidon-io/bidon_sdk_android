package com.applovin.mediation.adapters.interstitial

import android.app.Activity
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.adapter.MaxAdapterError
import com.applovin.mediation.adapter.MaxInterstitialAdapter
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters
import com.applovin.mediation.adapters.ext.asMaxAdapterError
import com.applovin.mediation.adapters.ext.getAsDouble
import com.applovin.mediation.adapters.ext.updatePrivacySettings
import com.applovin.mediation.adapters.keeper.AdKeeper
import com.applovin.mediation.adapters.keeper.AdKeepers
import com.applovin.mediation.adapters.logger.AppLovinSdkLogger
import com.applovin.mediation.adapters.logger.Logger
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.ads.Ad
import org.bidon.sdk.ads.AuctionInfo
import org.bidon.sdk.ads.interstitial.InterstitialListener
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue

internal class BidonInterstitial : MaxInterstitialAdapter, Logger by AppLovinSdkLogger {

    private var adInstance: InterstitialAdInstance? = null
    private var adKeeper: AdKeeper<InterstitialAdInstance>? = null

    private var maxPlacementId: String = "UNDEFINED"
    private var maxEcpm: Double = 0.0

    init {
        log(TAG, "Create instance $this")
    }

    override fun loadInterstitialAd(
        parameters: MaxAdapterResponseParameters,
        activity: Activity?,
        listener: MaxInterstitialAdapterListener
    ) {
        BidonSdk.updatePrivacySettings(parameters)

        val customParameters = parameters.customParameters

        maxPlacementId = parameters.thirdPartyAdPlacementId
        maxEcpm = customParameters.getAsDouble("ecpm")

        val adKeeper = AdKeepers.getKeeper<InterstitialAdInstance>(parameters.adUnitId, MaxAdFormat.INTERSTITIAL)
            .also { this.adKeeper = it }

        // Get last registered ecpm
        val lastRegisteredEcpm = adKeeper.lastRegisteredEcpm()
        // Register ecpm for range calculation
        adKeeper.registerEcpm(maxEcpm)

        val unicorn = customParameters.getBoolean("unicorn")
        if (unicorn) {
            log(TAG, "Placement ID: $maxPlacementId, Unicorn Detected, Placement ECPM: $maxEcpm")
            if (activity == null) {
                log(TAG, "Interstitial ad failed to load: Activity is null")
                listener.onInterstitialAdLoadFailed(MaxAdapterError.MISSING_ACTIVITY)
                onDestroy()
            } else {
                val auctionKey = customParameters.getString("auction_key", null)
                log(
                    TAG,
                    "Loading interstitial ad for auction key: $auctionKey, pricefloor: ${0.0}, Placement ID: $maxPlacementId"
                )

                val newAdInstance = InterstitialAdInstance(auctionKey = auctionKey)
                    .also { this.adInstance = it }
                newAdInstance.setListener(listener.asBidonListener())
                newAdInstance.addExtra("previous_auction_price", lastRegisteredEcpm)
                newAdInstance.load(activity)
            }
        } else {
            log(TAG, "Placement ID: $maxPlacementId, No Unicorn Detected, Placement ECPM: $maxEcpm")
            val consumeAdInstance = adKeeper.consumeAd(ecpm = maxEcpm)
                .also { this.adInstance = it }
            if (consumeAdInstance == null) {
                log(
                    TAG,
                    "Interstitial ad failed to load from cache: ECPM ISN'T SUITABLE, Placement ID: $maxPlacementId"
                )
                listener.onInterstitialAdLoadFailed(MaxAdapterError.NO_FILL)
                onDestroy()
            } else {
                log(
                    TAG,
                    "Interstitial ad loaded $consumeAdInstance from cache, Placement ID: $maxPlacementId"
                )
                consumeAdInstance.setListener(listener.asBidonListener())
                listener.onInterstitialAdLoaded()
            }
        }
    }

    override fun showInterstitialAd(
        parameters: MaxAdapterResponseParameters?,
        activity: Activity?,
        listener: MaxInterstitialAdapterListener
    ) {
        log(TAG, "Showing interstitial ad: $adInstance, Placement ID: $maxPlacementId")
        val adInstance = adInstance
        val activity = activity
        if (adInstance == null) {
            log(TAG, "Interstitial ad display failed: Ad is null, Placement ID: $maxPlacementId")
            listener.onInterstitialAdDisplayFailed(MaxAdapterError.AD_DISPLAY_FAILED)
            onDestroy()
        } else if (adInstance.isReady == false) {
            log(
                TAG,
                "Interstitial ad display failed: Ad is not ready, Placement ID: $maxPlacementId"
            )
            listener.onInterstitialAdDisplayFailed(MaxAdapterError.AD_DISPLAY_FAILED)
            onDestroy()
        } else if (activity == null) {
            log(
                TAG,
                "Interstitial ad display failed: Activity is null, Placement ID: $maxPlacementId"
            )
            listener.onInterstitialAdDisplayFailed(MaxAdapterError.MISSING_ACTIVITY)
            onDestroy()
        } else {
            adInstance.show(activity)
        }
    }

    private fun onDestroy() {
        log(TAG, "Destroying interstitial ad: $adInstance, Placement ID: $maxPlacementId")
        adInstance?.destroy()
        adInstance = null
        adKeeper = null
    }

    private fun MaxInterstitialAdapterListener.asBidonListener(): InterstitialListener {
        val maxInterstitialCallback = this
        return object : InterstitialListener {
            override fun onAdLoaded(ad: Ad, auctionInfo: AuctionInfo) {
                val loadedAdInstance = this@BidonInterstitial.adInstance
                val adKeeper = this@BidonInterstitial.adKeeper
                if (loadedAdInstance == null || adKeeper == null) {
                    log(
                        TAG,
                        "Interstitial ad failed to load: Ad is null or keeper is null, Placement ID: $maxPlacementId"
                    )
                    maxInterstitialCallback.onInterstitialAdLoadFailed(MaxAdapterError.NO_FILL)
                    onDestroy()
                } else {
                    log(
                        TAG,
                        "Interstitial ad try to keep, Placement ID: $maxPlacementId, ECPM: ${ad.price}"
                    )
                    // Apply ad info to the loaded ad instance
                    // Keep the loaded ad instance
                    val rejectedAdInstance =
                        adKeeper.keepAd(adInstance = loadedAdInstance.applyAdInfo(ad))
                    rejectedAdInstance?.destroy()

                    // Consume the ad instance
                    val consumeAdInstance = adKeeper.consumeAd(ecpm = maxEcpm)
                        .also { adInstance = it }
                    if (consumeAdInstance == null) {
                        log(
                            TAG,
                            "Interstitial ad failed to load from cache: ECPM ISN'T SUITABLE, Placement ID: $maxPlacementId"
                        )
                        maxInterstitialCallback.onInterstitialAdLoadFailed(MaxAdapterError.NO_FILL)
                        onDestroy()
                    } else {
                        log(
                            TAG,
                            "Interstitial ad loaded $consumeAdInstance from cache, Placement ID: $maxPlacementId"
                        )
                        consumeAdInstance.setListener(this)
                        maxInterstitialCallback.onInterstitialAdLoaded()
                    }
                }
            }

            override fun onAdLoadFailed(auctionInfo: AuctionInfo?, cause: BidonError) {
                log(TAG, "Interstitial ad failed to load: $cause, Placement ID: $maxPlacementId")
                maxInterstitialCallback.onInterstitialAdLoadFailed(cause.asMaxAdapterError())
                onDestroy()
            }

            override fun onAdShown(ad: Ad) {
                log(TAG, "Interstitial ad shown, Placement ID: $maxPlacementId, ECPM: ${ad.price}")
                maxInterstitialCallback.onInterstitialAdDisplayed()
            }

            override fun onAdShowFailed(cause: BidonError) {
                log(TAG, "Interstitial ad failed to show: $cause, Placement ID: $maxPlacementId")
                maxInterstitialCallback.onInterstitialAdDisplayFailed(cause.asMaxAdapterError())
                onDestroy()
            }

            override fun onAdClicked(ad: Ad) {
                log(
                    TAG,
                    "Interstitial ad clicked, Placement ID: $maxPlacementId, ECPM: ${ad.price}"
                )
                maxInterstitialCallback.onInterstitialAdClicked()
            }

            override fun onAdClosed(ad: Ad) {
                log(TAG, "Interstitial ad closed, Placement ID: $maxPlacementId, ECPM: ${ad.price}")
                maxInterstitialCallback.onInterstitialAdHidden()
                onDestroy()
            }

            override fun onAdExpired(ad: Ad) {
                log(
                    TAG,
                    "Interstitial ad expired, Placement ID: $maxPlacementId, ECPM: ${ad.price}"
                )
                onDestroy()
            }

            override fun onRevenuePaid(ad: Ad, adValue: AdValue) {
                log(
                    TAG,
                    "Interstitial ad revenue paid: $adValue, Placement ID: $maxPlacementId, ECPM: ${ad.price}"
                )
            }
        }
    }
}

private const val TAG = "BidonInterstitial"
