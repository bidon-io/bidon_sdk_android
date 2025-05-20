package com.applovin.mediation.adapters.rewarded

import android.app.Activity
import com.applovin.impl.mediation.MaxRewardImpl
import com.applovin.mediation.adapter.MaxAdapterError
import com.applovin.mediation.adapter.MaxRewardedAdapter
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener
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
import org.bidon.sdk.ads.rewarded.Reward
import org.bidon.sdk.ads.rewarded.RewardedListener
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.analytic.AdValue

internal class BidonRewarded(
    private val adKeeper: AdKeeper<RewardedAdInstance> = AdKeepers.rewarded
) : MaxRewardedAdapter, Logger by AppLovinSdkLogger {

    private var adInstance: RewardedAdInstance? = null

    private var maxPlacementId: String = "UNDEFINED"
    private var maxEcpm: Double = 0.0

    init {
        log(TAG, "Create instance $this")
    }

    override fun loadRewardedAd(
        parameters: MaxAdapterResponseParameters,
        activity: Activity?,
        listener: MaxRewardedAdapterListener
    ) {
        BidonSdk.updatePrivacySettings(parameters)

        val customParameters = parameters.customParameters

        maxPlacementId = parameters.thirdPartyAdPlacementId
        maxEcpm = customParameters.getAsDouble("ecpm")

        // Get last registered ecpm
        val lastRegisteredEcpm = adKeeper.lastRegisteredEcpm()
        // Register ecpm for range calculation
        adKeeper.registerEcpm(maxEcpm)

        val unicorn = customParameters.getBoolean("unicorn")
        if (unicorn) {
            log(TAG, "Placement ID: $maxPlacementId, Unicorn Detected, Placement ECPM: $maxEcpm")
            if (activity == null) {
                log(TAG, "Rewarded ad failed to load: Activity is null")
                listener.onRewardedAdLoadFailed(MaxAdapterError.MISSING_ACTIVITY)
                onDestroy()
            } else {
                val auctionKey = customParameters.getString("auction_key", null)
                log(
                    TAG,
                    "Loading rewarded ad for auction key: $auctionKey, pricefloor: ${0.0}, Placement ID: $maxPlacementId"
                )

                val newAdInstance = RewardedAdInstance(auctionKey = auctionKey)
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
                    "Rewarded ad failed to load from cache: ECPM ISN'T SUITABLE, Placement ID: $maxPlacementId"
                )
                listener.onRewardedAdLoadFailed(MaxAdapterError.NO_FILL)
                onDestroy()
            } else {
                log(
                    TAG,
                    "Rewarded ad loaded $consumeAdInstance from cache, Placement ID: $maxPlacementId"
                )
                consumeAdInstance.setListener(listener.asBidonListener())
                listener.onRewardedAdLoaded()
            }
        }
    }

    override fun showRewardedAd(
        parameters: MaxAdapterResponseParameters?,
        activity: Activity?,
        listener: MaxRewardedAdapterListener
    ) {
        log(TAG, "Showing rewarded ad: $adInstance, Placement ID: $maxPlacementId")
        val adInstance = adInstance
        val activity = activity
        if (adInstance == null) {
            log(TAG, "Rewarded ad display failed: Ad is null, Placement ID: $maxPlacementId")
            listener.onRewardedAdDisplayFailed(MaxAdapterError.AD_DISPLAY_FAILED)
            onDestroy()
        } else if (adInstance.isReady == false) {
            log(
                TAG,
                "Rewarded ad display failed: Ad is not ready, Placement ID: $maxPlacementId"
            )
            listener.onRewardedAdDisplayFailed(MaxAdapterError.AD_DISPLAY_FAILED)
            onDestroy()
        } else if (activity == null) {
            log(
                TAG,
                "Rewarded ad display failed: Activity is null, Placement ID: $maxPlacementId"
            )
            listener.onRewardedAdDisplayFailed(MaxAdapterError.MISSING_ACTIVITY)
            onDestroy()
        } else {
            adInstance.show(activity)
        }
    }

    private fun onDestroy() {
        log(TAG, "Destroying rewarded ad: $adInstance, Placement ID: $maxPlacementId")
        adInstance?.destroy()
        adInstance = null
    }

    private fun MaxRewardedAdapterListener.asBidonListener(): RewardedListener {
        val maxRewardedCallback = this
        var hasGrantedReward = false
        return object : RewardedListener {
            override fun onAdLoaded(ad: Ad, auctionInfo: AuctionInfo) {
                val loadedAdInstance = adInstance
                if (loadedAdInstance == null) {
                    log(
                        TAG,
                        "Rewarded ad failed to load: Ad is null, Placement ID: $maxPlacementId"
                    )
                    maxRewardedCallback.onRewardedAdLoadFailed(MaxAdapterError.NO_FILL)
                    onDestroy()
                } else {
                    log(
                        TAG,
                        "Rewarded ad try to keep, Placement ID: $maxPlacementId, ECPM: ${ad.price}"
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
                            "Rewarded ad failed to load from cache: ECPM ISN'T SUITABLE, Placement ID: $maxPlacementId"
                        )
                        maxRewardedCallback.onRewardedAdLoadFailed(MaxAdapterError.NO_FILL)
                        onDestroy()
                    } else {
                        log(
                            TAG,
                            "Rewarded ad loaded $consumeAdInstance from cache, Placement ID: $maxPlacementId"
                        )
                        consumeAdInstance.setListener(this)
                        maxRewardedCallback.onRewardedAdLoaded()
                    }
                }
            }

            override fun onAdLoadFailed(auctionInfo: AuctionInfo?, cause: BidonError) {
                log(TAG, "Rewarded ad failed to load: $cause, Placement ID: $maxPlacementId")
                maxRewardedCallback.onRewardedAdLoadFailed(cause.asMaxAdapterError())
                onDestroy()
            }

            override fun onAdShown(ad: Ad) {
                log(TAG, "Rewarded ad shown, Placement ID: $maxPlacementId, ECPM: ${ad.price}")
                maxRewardedCallback.onRewardedAdDisplayed()
            }

            override fun onAdShowFailed(cause: BidonError) {
                log(TAG, "Rewarded ad failed to show: $cause, Placement ID: $maxPlacementId")
                maxRewardedCallback.onRewardedAdDisplayFailed(cause.asMaxAdapterError())
                onDestroy()
            }

            override fun onAdClicked(ad: Ad) {
                log(
                    TAG,
                    "Rewarded ad clicked, Placement ID: $maxPlacementId, ECPM: ${ad.price}"
                )
                maxRewardedCallback.onRewardedAdClicked()
            }

            override fun onAdClosed(ad: Ad) {
                log(TAG, "Rewarded ad closed, Placement ID: $maxPlacementId, ECPM: ${ad.price}")

                // https://developers.applovin.com/en/max/demand-partners/building-a-custom-adapter/
                // If a reward should be presented to the user, call MaxRewardedAdapterListener.onUserRewarded()
                // with an appropriate MaxReward amount and currency. If no amount is available, default to MaxReward.DEFAULT_AMOUNT.
                // AppLovin recommends that you call this immediately before MaxRewardedAdapterListener.onRewardedAdHidden().
                //
                // Also we need to call `configureReward()` in `showRewardedAd()` method
                //
                // Example: https://github.com/AppLovin/AppLovin-MAX-SDK-Android/blob/master/BidMachine/src/main/java/com/applovin/mediation/adapters/BidMachineMediationAdapter.java#L543
                if (hasGrantedReward) {
                    val reward = MaxRewardImpl.createDefault()
                    log(
                        TAG,
                        "Rewarded ad user rewarded, Placement ID: $maxPlacementId, ECPM: ${ad.price}, Reward: $reward"
                    )
                    maxRewardedCallback.onUserRewarded(reward)
                }
                maxRewardedCallback.onRewardedAdHidden()
                onDestroy()
            }

            override fun onAdExpired(ad: Ad) {
                log(
                    TAG,
                    "Rewarded ad expired, Placement ID: $maxPlacementId, ECPM: ${ad.price}"
                )
                onDestroy()
            }

            override fun onUserRewarded(ad: Ad, reward: Reward?) {
                // https://developers.applovin.com/en/max/demand-partners/building-a-custom-adapter/
                // If a reward should be presented to the user, call MaxRewardedAdapterListener.onUserRewarded()
                // with an appropriate MaxReward amount and currency. If no amount is available, default to MaxReward.DEFAULT_AMOUNT.
                // AppLovin recommends that you call this immediately before MaxRewardedAdapterListener.onRewardedAdHidden().
                //
                // Also we need to call `configureReward()` in `showRewardedAd()` method
                //
                // Example: https://github.com/AppLovin/AppLovin-MAX-SDK-Android/blob/master/BidMachine/src/main/java/com/applovin/mediation/adapters/BidMachineMediationAdapter.java#L543
                hasGrantedReward = true
            }

            override fun onRevenuePaid(ad: Ad, adValue: AdValue) {
                log(
                    TAG,
                    "Rewarded ad revenue paid: $adValue, Placement ID: $maxPlacementId, ECPM: ${ad.price}"
                )
            }
        }
    }
}

private const val TAG = "BidonRewarded"
