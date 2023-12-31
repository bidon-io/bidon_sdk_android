# Interstitials

## Loading an Interstitial Ad

To load an interstitial ad, create a `Interstitial` instance.
Important: for a single instance of an InterstitialAd, load() and show() can only be called once. Create new instance for every new interstitial ad.

```kotlin
val interstitial = InterstitialAd()
```

Set `InterstitialListener` for receiving all-related events, including loading/displaying and revenue callbacks.

```kotlin
interstitial.setInterstitialListener(object : InterstitialListener {
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

    override fun onRevenuePaid(ad: Ad, adValue: AdValue) {
        // adValue.revenue - ad revenue from mediation
    }
})
interstitial.loadAd(activity = this, pricefloor = otherMediationEcpm) // or use DefaultMinPrice
```

## Displaying interstitial ad

```kotlin
if (interstitial.isReady()) {
    interstitial.showAd(activity = this)
}
```