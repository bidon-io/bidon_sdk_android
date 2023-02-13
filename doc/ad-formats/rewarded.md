# Rewarded Ad

## Loading an Rewarded Ad

To load an rewarded ad, create a `Rewarded` instance. Add (optional) placement if needed, otherwise placement = "default" will be used.
Important: for a single instance of an Rewarded, load() and show() can only be called once. Create new instance for every new interstitial ad.

```kotlin
val rewarded = Rewarded(placement = "your_placement")
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
    
    override fun onRevenuePaid(ad: Ad) {
        // ad.price - ad revenue from mediation
    }
})
rewarded.load(activity = this, minPrice = otherMediationEcpm) // or use DefaultMinPrice
```

## Displaying interstitial ad

```kotlin
rewarded.show(activity = this)
```