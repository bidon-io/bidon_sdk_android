package com.applovin.mediation.adapters.banner

import android.app.Activity
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.adapter.MaxAdViewAdapter
import com.applovin.mediation.adapter.MaxAdapterError
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener
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
import org.bidon.sdk.ads.banner.BannerListener
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue

internal class BidonBanner : MaxAdViewAdapter, Logger by AppLovinSdkLogger {

    private var adInstance: BannerAdInstance? = null

    private var maxPlacementId: String = "UNDEFINED"
    private var maxEcpm: Double = 0.0

    init {
        log(TAG, "Create instance $this")
    }

    override fun loadAdViewAd(
        parameters: MaxAdapterResponseParameters,
        adFormat: MaxAdFormat,
        activity: Activity?,
        listener: MaxAdViewAdapterListener,
    ) {
        BidonSdk.updatePrivacySettings(parameters)

        val customParameters = parameters.customParameters

        maxPlacementId = parameters.thirdPartyAdPlacementId
        maxEcpm = customParameters.getAsDouble("ecpm")

        val adKeeper = AdKeepers.getKeeper<BannerAdInstance>(parameters.adUnitId, adFormat)

        // Get last registered ecpm
        val lastRegisteredEcpm = adKeeper.lastRegisteredEcpm()
        // Register ecpm for range calculation
        adKeeper.registerEcpm(maxEcpm)

        val unicorn = customParameters.getBoolean("unicorn")
        if (unicorn) {
            log(TAG, "Placement ID: $maxPlacementId, Unicorn Detected, Placement ECPM: $maxEcpm")
            if (activity == null) {
                log(TAG, "Banner ad failed to load: Activity is null")
                listener.onAdViewAdLoadFailed(MaxAdapterError.MISSING_ACTIVITY)
                onDestroy()
            } else {
                val auctionKey = customParameters.getString("auction_key", null)
                log(
                    TAG,
                    "Loading banner ad for auction key: $auctionKey, pricefloor: ${0.0}, Placement ID: $maxPlacementId"
                )

                val newAdInstance = BannerAdInstance(
                    context = activity,
                    format = adFormat,
                    auctionKey = auctionKey,
                ).also { this.adInstance = it }
                newAdInstance.setListener(listener.asBidonListener(adKeeper))
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
                    "Banner ad failed to load from cache: ECPM ISN'T SUITABLE, Placement ID: $maxPlacementId"
                )
                listener.onAdViewAdLoadFailed(MaxAdapterError.NO_FILL)
                onDestroy()
            } else {
                log(
                    TAG,
                    "Banner ad loaded $consumeAdInstance from cache, Placement ID: $maxPlacementId"
                )
                consumeAdInstance.setListener(listener.asBidonListener(adKeeper))
                consumeAdInstance.show()
                listener.onAdViewAdLoaded(consumeAdInstance.bannerAd)
            }
        }
    }

    private fun onDestroy() {
        log(TAG, "Destroying banner ad: $adInstance, Placement ID: $maxPlacementId")
        adInstance?.destroy()
        adInstance = null
    }

    private fun MaxAdViewAdapterListener.asBidonListener(adKeeper: AdKeeper<BannerAdInstance>): BannerListener {
        val maxBannerCallback = this
        return object : BannerListener {
            override fun onAdLoaded(ad: Ad, auctionInfo: AuctionInfo) {
                val loadedAdInstance = this@BidonBanner.adInstance
                if (loadedAdInstance == null) {
                    log(
                        TAG,
                        "Banner ad failed to load: Ad is null, Placement ID: $maxPlacementId"
                    )
                    maxBannerCallback.onAdViewAdLoadFailed(MaxAdapterError.NO_FILL)
                    onDestroy()
                } else {
                    log(
                        TAG,
                        "Banner ad try to keep, Placement ID: $maxPlacementId, ECPM: ${ad.price}"
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
                            "Banner ad failed to load from cache: ECPM ISN'T SUITABLE, Placement ID: $maxPlacementId"
                        )
                        maxBannerCallback.onAdViewAdLoadFailed(MaxAdapterError.NO_FILL)
                        onDestroy()
                    } else {
                        log(
                            TAG,
                            "Banner ad loaded $consumeAdInstance from cache, Placement ID: $maxPlacementId"
                        )
                        consumeAdInstance.setListener(this)
                        consumeAdInstance.show()
                        maxBannerCallback.onAdViewAdLoaded(consumeAdInstance.bannerAd)
                    }
                }
            }

            override fun onAdLoadFailed(auctionInfo: AuctionInfo?, cause: BidonError) {
                log(TAG, "Banner ad failed to load: $cause, Placement ID: $maxPlacementId")
                maxBannerCallback.onAdViewAdLoadFailed(cause.asMaxAdapterError())
                onDestroy()
            }

            override fun onAdShown(ad: Ad) {
                log(TAG, "Banner ad shown, Placement ID: $maxPlacementId, ECPM: ${ad.price}")
                maxBannerCallback.onAdViewAdDisplayed()
            }

            override fun onAdShowFailed(cause: BidonError) {
                log(TAG, "Banner ad failed to show: $cause, Placement ID: $maxPlacementId")
                maxBannerCallback.onAdViewAdDisplayFailed(cause.asMaxAdapterError())
                onDestroy()
            }

            override fun onAdClicked(ad: Ad) {
                log(
                    TAG,
                    "Banner ad clicked, Placement ID: $maxPlacementId, ECPM: ${ad.price}"
                )
                maxBannerCallback.onAdViewAdClicked()
            }

            override fun onAdExpired(ad: Ad) {
                log(
                    TAG,
                    "Banner ad expired, Placement ID: $maxPlacementId, ECPM: ${ad.price}"
                )
                onDestroy()
            }

            override fun onRevenuePaid(ad: Ad, adValue: AdValue) {
                log(
                    TAG,
                    "Banner ad revenue paid: $adValue, Placement ID: $maxPlacementId, ECPM: ${ad.price}"
                )
            }
        }
    }
}

private const val TAG = "BidonBanner"
