# Banners

## Loading an Banners

To load a banner, create a `BannerView` instance. Add (optional) placement if needed, otherwise placement = "default" will be used. 
Important: for a single instance of a banner, load() and show() can only be called once. Create new instance for every new banner (in case refreshing banner by timeout as well).

```kotlin
val banner = BannerView(context, placement = "your_placement")
```

Set `BannerListener` for receiving all-related events, including loading/displaying and revenue callbacks.

```kotlin
banner.setBannerListener(object : BannerListener {
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

    override fun onRevenuePaid(ad: Ad) {
        // ad.price - ad revenue from mediation
    }
})
banner.setAdSize(BannerSize.Banner)
banner.load(minPrice = otherMediationEcpm) // or use DefaultMinPrice
```

## Displaying banners
To show banner, place it to you AdContainer and invoke `show()`

```kotlin
banner.show()
```

## BannerSize

| Ad View Format | Size        | Description                    |
|----------------|-------------|--------------------------------|
| Banner         | 320 x 50    | Fixed size banner for phones   |
| LeaderBoard    | 728 x 90    | Fixed size banner for pads     |
| MRec           | 300 x 250   | Fixed medium rectangle banners |
| Adaptive       | -/- x 50/90 | Flexible width banners         |
