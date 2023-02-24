# Rewarded Ad

## Loading an Rewarded Ad

To load an rewarded ad, create a `RewardedAd` instance. Add (optional) placement if needed, otherwise placement = "default" will be used.
Important: for a single instance of an RewardedAd, load() and show() can only be called once. Create new instance for every new interstitial ad.

```kotlin
val rewarded = RewardedAd(placement = "your_placement")
```

Set `RewardedListener` for receiving all-related events, including loading/displaying and revenue callbacks.

```kotlin
rewarded.setRewardedListener(object : RewardedListener {
    override fun onAdLoaded(ad: Ad) {
        // ready to show
    }

    override fun onAdLoadFailed(cause: BidonError) {
    }

    override fun onAdShowFailed(cause: BidonError) {
    }

    override fun onAdShown(ad: Ad) {
    }

    override fun onAdClicked(ad: Ad) {
    }

    override fun onAdClosed(ad: Ad) {
    }

    override fun onAdExpired(ad: Ad) {
    }

    override fun onUserRewarded(ad: Ad, reward: Reward?) {
        // reward - contains reward data if exist
    }
    
    override fun onRevenuePaid(ad: Ad, adValue: AdValue) {
        // adValue.revenue - ad revenue from mediation
    }
})
rewarded.loadAd(activity = this, pricefloor = otherMediationEcpm) // or use DefaultMinPrice
```

## Displaying interstitial ad

```kotlin
if (rewarded.isReady()) {
    rewarded.showAd(activity = this)
}
```