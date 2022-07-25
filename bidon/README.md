# AppLovin MAX + Bidon

### Table of contents

  - [Installation](#installation)
  - [Demand Sources](#demand-sources)
  - [MMP](#mmp)
  - [Initialize](#initialize-the-sdk)
  - [Interstitial](#interstitial)
  - [Rewarded Video](#rewarded-video)
  - [Banner](#banner)
  
>  [AppLovin MAX](https://dash.applovin.com/documentation/mediation/android/getting-started/integration) must already be integrated into to your project. 

## Installation

### Dependencies (Recommended)

To integrate the AppLovin Decorator through Dependencies, first add the following lines to your `build.gradle` (:app):

``` kotlin
dependencies {
    implementation 'com.appodealstack.bidon:bidon-sdk:1.0.0'
    implementation 'com.appodealstack.bidon:applovin-decorator:1.0.0'

# For usage of Demand Sources uncomment following lines
    implementation 'com.appodealstack.bidon:bidmachine-adapter:1.0.0'
    implementation 'com.appodealstack.bidon:admob-adapter:1.0.0'

# For usage of MMP uncomment following lines
    implementation 'com.appodealstack.bidon:appsflyer-adapter:1.0.0'
   
    ...
}

```

Then sync project.


### Manual

> TODO:// Manual integration guiode

## Demand Sources

For using of BidMachine and GoogleMobileAds SDK in postbid you will need to register their's adapters before initialization of the SDK

```kotlin
AppLovinDecorator
    .register(
        AdmobAdapter::class.java,
        AdmobParameters(
            interstitials = mapOf(
                priceFloor1Float to "AdMob interstitial ad unit id",
            ),
            rewarded = mapOf(
                priceFloor2Float to "AdMob rewarded ad unit id",
            ),
            banners = mapOf(
                priceFloor3Float to "AdMob banner ad unit id",
            ),
        )
    )
    .register(
        BidMachineAdapter::class.java,
        BidMachineParameters(sourceId = YOUR_APP_SOURCE_ID)
    )
```


## MMP

For using of AppsFlyer as ad revenue tracking partner you will need to register its adapter before initialization of the SDK

```kotlin
AppLovinDecorator
    .register(
        AppsflyerAnalytics::class.java,
        AppsflyerParameters.DevKey(APPSFLYER_DEV_KEY)
    )

```



## Initialize the SDK

```kotlin
AppLovinDecorator.getInstance(activity).mediationProvider = "max"
AppLovinDecorator
    .initializeSdk(activity) { appLovinSdkConfiguration ->
        // initialization finished callback
    }
```

## Full initialization code

```kotlin
AppLovinDecorator.getInstance(activity).mediationProvider = "max"
AppLovinDecorator
    .register(
        AdmobAdapter::class.java,
        AdmobParameters(
            interstitials = mapOf(
                priceFloor1Float to "AdMob interstitial ad unit id",
            ),
            rewarded = mapOf(
                priceFloor2Float to "AdMob rewarded ad unit id",
            ),
            banners = mapOf(
                priceFloor3Float to "AdMob banner ad unit id",
            ),
        )
    )
    .register(
        BidMachineAdapter::class.java,
        BidMachineParameters(sourceId = YOUR_APP_SOURCE_ID)
    )
    .register(
        AppsflyerAnalytics::class.java,
        AppsflyerParameters.DevKey(APPSFLYER_DEV_KEY)
    )
    .initializeSdk(activity) { appLovinSdkConfiguration ->
        // initialization finished callback
    }
```

## Interstitial

### Loading an Interstitial ad

To load an interstitial ad, instantiate an `BNMaxInterstitialAd` corresponding to your ad unit.

Call `loadAd()` for loading. 

Call `showAd()` for displaying.

Add listener to be notified when your ad is ready and of other ad-related events with 
`setListener(bnInterstitialListener: BNInterstitialListener)`

```kotlin
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.appodealstack.applovin.interstitial.BNInterstitialListener
import com.appodealstack.applovin.interstitial.BNMaxInterstitialAd
import com.appodealstack.mads.demands.Ad

class MainActivity : FragmentActivity() {
    private val interstitialAd by lazy {
        BNMaxInterstitialAd("applovin ad unit id", this)
    }

    private val interstitialListener = object : BNInterstitialListener {
        override fun onAdLoaded(ad: Ad) {
            // Interstitial ad is ready to be shown. 'interstitialAd.isReady' will now return 'true'
            interstitialAd.showAd()
        }

        override fun onAdLoadFailed(cause: Throwable) {
            // Interstitial ad failed to load
            // We recommend retrying with exponentially higher delays up to a maximum delay (in this case 64 seconds)
        }

        override fun onAdDisplayed(ad: Ad) {
        }

        override fun onAdDisplayFailed(cause: Throwable) {
            // Interstitial ad failed to display. We recommend loading the next ad with [interstitialAd.loadAd()]
        }

        override fun onAdClicked(ad: Ad) {
        }

        override fun onAdHidden(ad: Ad) {
            // Interstitial ad is hidden. Pre-load the next ad with [interstitialAd.loadAd()]
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        interstitialAd.setListener(interstitialListener)
        interstitialAd.loadAd()
    }
}
```

## Rewarded Video

### Loading a Rewarded Ad

To load a rewarded ad, get an instance of a `BNMaxRewardedAd` object that corresponds to your rewarded ad unit and then call its `loadAd` method. Implement `BNRewardedListener` so that you are notified when your ad is ready and of other ad-related events.

```kotlin
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.appodealstack.applovin.rewarded.BNMaxRewardedAd
import com.appodealstack.applovin.rewarded.BNRewardedListener
import com.appodealstack.mads.demands.Ad
import com.appodealstack.mads.demands.RewardedAdListener

class MainActivity : FragmentActivity() {
    private val rewardedAd by lazy {
        BNMaxRewardedAd("applovin ad unit id", this)
    }

    private val rewardedListener = object : BNRewardedListener {
        override fun onRewardedStarted(ad: Ad) {}
        override fun onRewardedCompleted(ad: Ad) {}

        override fun onUserRewarded(ad: Ad, reward: RewardedAdListener.Reward?) {
            // Rewarded ad was displayed and user should receive the reward
        }

        override fun onAdLoaded(ad: Ad) {
            // Rewarded ad is ready to be shown. 'interstitialAd.isReady' will now return 'true'
            rewardedAd.showAd()
        }

        override fun onAdLoadFailed(cause: Throwable) {
            // Rewarded ad failed to load
            // We recommend retrying with exponentially higher delays up to a maximum delay (in this case 64 seconds)
        }

        override fun onAdDisplayed(ad: Ad) {}

        override fun onAdDisplayFailed(cause: Throwable) {
            // Rewarded ad failed to display. We recommend loading the next ad with [interstitialAd.loadAd()]
        }

        override fun onAdClicked(ad: Ad) {}

        override fun onAdHidden(ad: Ad) {
            // Rewarded ad is hidden. Pre-load the next ad with [interstitialAd.loadAd()]
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rewardedAd.setListener(rewardedListener)
        rewardedAd.loadAd()
    }
}
```

### Accessing the Amount and Label for a Rewarded Ad

If demand-winner exposes rewarded data, you will be notified about it in `onUserRewarded(ad: Ad, reward: Reward?)`-callback.

```kotlin
data class Reward(
    val label: String,
    val amount: Int
)
```

### Source Applovin Ad objects

Callbacks `BNRewardedListener`, `BNInterstitialListener` and `BNMaxAdViewAdListener` return `Ad`-object with parameter `adSource: Any`, which contains a source demand-winner Ad-object.

If Applovin wins, you can retrieve its source Ad objects:

```kotlin
val maxInterstitialAd = ad.sourceAd as? com.applovin.mediation.ads.MaxInterstitialAd // for interstitial
val maxRewardedAd = ad.sourceAd as? com.applovin.mediation.ads.MaxRewardedAd // for rewarded
val maxAdView = ad.sourceAd as? com.applovin.mediation.ads.MaxAdView // for banner
```




## Banner
### Loading and Showing Banners programmatically

To load a banner AdView, get an instance of a `BNMaxAdView` class that corresponds to your rewarded ad unit and then call its `loadAd` method. Implement `BNMaxAdViewAdListener` to be notified about banner-related events.

```kotlin
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.appodealstack.applovin.banner.BNMaxAdView
import com.appodealstack.applovin.impl.BNMaxAdViewAdListener
import com.appodealstack.mads.demands.Ad

class MainActivity : FragmentActivity() {
    private val bannerListener = object : BNMaxAdViewAdListener {
        override fun onAdExpanded(ad: Ad) {}
        override fun onAdCollapsed(ad: Ad) {}
        override fun onAdLoaded(ad: Ad) {}
        override fun onAdDisplayFailed(error: Throwable) {}
        override fun onAdClicked(ad: Ad) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bnMaxAdView = BNMaxAdView("YOUR_AD_UNIT_ID", BannerSize.Banner, this)
        bnMaxAdView.setListener(bannerListener)
        rootView.addView(bnMaxAdView)

        bnMaxAdView.loadAd()
    }
}
```


### Loading and Showing Banners in Layout Editor 

Alternatively, you can add BNMaxAdView banners to your view layout XML. 

```xml
<com.appodealstack.applovin.banner.BNMaxAdView
    android:id="@+id/bannerAdView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:adUnitId="YOUR_AD_UNIT_ID"
    app:adFormat="banner" />
```

```kotlin
class MainActivity : FragmentActivity() {
    private val bannerListener = object : BNMaxAdViewAdListener {
        override fun onAdExpanded(ad: Ad) {}
        override fun onAdCollapsed(ad: Ad) {}
        override fun onAdLoaded(ad: Ad) {}
        override fun onAdDisplayFailed(error: Throwable) {}
        override fun onAdClicked(ad: Ad) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bnMaxAdView = findViewById<BNMaxAdView>(R.id.bannerAdView)
        bnMaxAdView.setListener(bannerListener)
        
        bnMaxAdView.loadAd()
    }
}
```

### Stopping and Starting Auto Refresh

SDK handles auto refresh. By default auto refresh timeout is 10 sec. You can modify default logic by using these methods:

```kotlin
// start auto refresh
bnMaxAdView.startAutoRefresh()

// stop auto refresh
bnMaxAdView.stopAutoRefresh()

//start auto refresh with timeout
bnMaxAdView.setAutoRefreshTimeout(TIMEOUT)
```
