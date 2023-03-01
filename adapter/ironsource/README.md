# ironSource + Bidon

### Table of contents

  - [Installation](#installation)
  - [Demand Sources](#demand-sources)
  - [MMP](#mmp)
  - [Initialize](#initialize-the-sdk)
  - [Interstitial](#interstitial)
  - [Rewarded Video](#rewarded-video)
  - [Banner](#banner)
  
>  [ironSource](https://developers.is.com/ironsource-mobile/android/android-sdk/) must already be integrated into to your project. 

## Installation

### Dependencies (Recommended)

Add Bidon-repository to `settings.gradle.kts`:
```ruby
dependencyResolutionManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/io-bidon/bidon-sdk-Android")
        }
        ...
    }
}
```

To integrate the ironSource Decorator through Dependencies, first add the following lines to your `build.gradle` (:app):

``` ruby
dependencies {
    implementation 'org.bidon.sdk:bidon-sdk:1.0.0'
    implementation 'org.bidon.sdk:ironsource-decorator:1.0.0'

    # Demand Sources
    implementation 'org.bidon.sdk:bidmachine-adapter:1.0.0'
    implementation 'org.bidon.sdk:admob-adapter:1.0.0'

    # MMP
    implementation 'org.bidon.sdk:appsflyer-adapter:1.0.0'
   
    ...
}

```

Then sync project.


### Manual

> TODO:// Manual integration guiode

## Demand Sources

For using of BidMachine and GoogleMobileAds SDK in postbid you will need to register their's adapters before initialization of the SDK

```kotlin
IronSourceDecorator
    .register(
        AdmobAdapter::class.java,
        AdmobParameters(
            interstitials = mapOf(
                pricefloor1Float to "AdMob interstitial ad unit id",
            ),
            rewarded = mapOf(
                pricefloor2Float to "AdMob rewarded ad unit id",
            ),
            banners = mapOf(
                pricefloor3Float to "AdMob banner ad unit id",
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
IronSourceDecorator
    .register(
        AppsflyerAnalytics::class.java,
        AppsflyerParameters.DevKey(APPSFLYER_DEV_KEY)
    )

```



## Initialize the SDK

```kotlin
IronSourceDecorator
    .init(
        activity = activity,
        appKey = IRONSOURCE_APP_KEY,
        listener = {
            // initialization finished
        }
    )
```

## Full initialization code

```kotlin
IronSourceDecorator
    .register(
        AdmobAdapter::class.java,
        AdmobParameters(
            interstitials = mapOf(
                pricefloor1Float to "AdMob interstitial ad unit id",
            ),
            rewarded = mapOf(
                pricefloor2Float to "AdMob rewarded ad unit id",
            ),
            banners = mapOf(
                pricefloor3Float to "AdMob banner ad unit id",
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
    .init(
        activity = activity,
        appKey = IRONSOURCE_APP_KEY,
        listener = {
            // initialization finished
        }
    )
```

## Interstitial

### Loading an Interstitial ad

To load an interstitial ad, invoke an `IronSourceDecorator.loadInterstitial()`.

Call `IronSourceDecorator.showInterstitial()` for displaying.

Add listener to be notified when your ad is ready and of other ad-related events with 
`IronSourceDecorator.setLevelPlayInterstitialListener(listener: IronSourceLevelPlayInterstitialListener)`
or
`IronSourceDecorator.setInterstitialListener(listener: IronSourceInterstitialListener)`.

```kotlin
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import org.bidon.ironsource.IronSourceDecorator
import org.bidon.ironsource.interstitial.IronSourceLevelPlayInterstitialListener
import com.appodealstack.mads.demands.Ad

class MainActivity : FragmentActivity() {
    private val interstitialListener = object : IronSourceLevelPlayInterstitialListener {
        override fun onAdReady(ad: Ad) {
            // Invoked when Interstitial Ad is ready to be shown after load function was called.
            IronSourceDecorator.showInterstitial()
        }

        override fun onAdLoadFailed(cause: Throwable) {
            // Invoked when there is no Interstitial Ad available after calling load function.
        }

        override fun onAdOpened(ad: Ad) {}
        override fun onAdShowSucceeded(ad: Ad) {}
        override fun onAdShowFailed(cause: Throwable) {}
        override fun onAdClicked(ad: Ad) {}
        override fun onAdClosed(ad: Ad) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        IronSourceDecorator.setLevelPlayInterstitialListener(interstitialListener)
        IronSourceDecorator.loadInterstitial()
    }
}
```

## Rewarded Video

### Loading a Rewarded Ad

To load an interstitial ad, invoke an `IronSourceDecorator.loadRewardedVideo()`.

Call `IronSourceDecorator.showRewardedVideo()` for displaying.

Add listener to be notified when your ad is ready and of other ad-related events with 
`IronSourceDecorator.setLevelPlayRewardedVideoListener(listener: IronSourceLevelPlayRewardedListener)`
or
`IronSourceDecorator.setRewardedListener(listener: IronSourceRewardedListener)`.



```kotlin
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import org.bidon.ironsource.IronSourceDecorator
import org.bidon.ironsource.rewarded.IronSourceLevelPlayRewardedListener
import com.appodealstack.mads.demands.Ad
import com.appodealstack.mads.demands.RewardedAdListener.Reward

class MainActivity : FragmentActivity() {
    private val rewardedListener = object : IronSourceLevelPlayRewardedListener {
        override fun onAdReady(ad: Ad) {
            /**
             * Invoked when there is a change in the ad availability status.
             */
             IronSourceDecorator.showRewardedVideo()
        }

        override fun onAdLoadFailed(cause: Throwable) {}

        override fun onAdOpened(ad: Ad) {
            /**
             * Invoked when the RewardedVideo ad view has opened.
             * Your Activity will lose focus. Please avoid performing heavy
             * tasks till the video ad will be closed.
             */
        }

        override fun onAdClicked(ad: Ad) {
            /**
             * Invoked when the end user clicked on the RewardedVideo ad
             */
        }

        override fun onAdRewarded(ad: Ad, reward: Reward?) {
            /**
             * Invoked when the user completed the video and should be rewarded.
             * If using server-to-server callbacks you may ignore this events and wait *for the callback from the ironSource server.
             *
             * @reward - Reward, if demand-winner exposes reward info
             */
        }

        override fun onAdClosed(ad: Ad) {
            /** Invoked when the RewardedVideo ad view is about to be closed.
             * Your activity will now regain its focus.*/
        }

        override fun onAdShowFailed(cause: Throwable) {
            /**
             * Invoked when RewardedVideo call to show a rewarded video has failed
             * IronSourceError contains the reason for the failure.
             */
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        IronSourceDecorator.setLevelPlayRewardedVideoListener(rewardedListener)
        IronSourceDecorator.loadRewardedVideo()
    }
}
```

### Accessing the Amount and Label for a Rewarded Ad

If demand-winner exposes rewarded data, you will be notified about it in `onAdRewarded(ad: Ad, reward: Reward?)`-callback.

```kotlin
data class Reward(
    val label: String,
    val amount: Int
)
```

### Source ironSource AdInfo objects

Callbacks `IronSourceLevelPlayInterstitialListener`, `IronSourceLevelPlayRewardedListener` and `IronSourceLevelPlayBannerListener` return `Ad`-object with parameter `adSource: Any`, which contains a source demand-winner Ad-object.

If ironSource wins, you can retrieve its source Ad objects:

```kotlin
val adInfo = ad.sourceAd as? com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
```




## Banner
### Loading and Showing Banners programmatically

#### Step 1. Create Banner Layout 

First, you’ll need to create a Banner view to configure the Banner.

Initiate the Banner view by calling this method (in this example it’s the BANNER banner size):
> BannerSize.Banner – standart banner 320 x 50
> BannerSize.Large – large banner 320 x 90
> BannerSize.Mrec – medium rectangular (MREC)	300 x 250

```kotlin
val bannerViewLayout = IronSourceDecorator.createBanner(this, BannerSize.Banner)
```


#### Step 2. Implement the Listener

Next, implement the Banner Listener in your code. The ironSource SDK fires several callbacks to inform you of Banner activity. 

The SDK will notify your Listener of all possible events listed below:

```kotlin
val bannerListener = object : IronSourceLevelPlayBannerListener {
    override fun onAdLoaded(ad: Ad) {
        /**
         * Invoked when there is a change in the ad availability status.
         */
    }

    override fun onAdLoadFailed(cause: Throwable) {}

    override fun onAdClicked(ad: Ad) {
        /**
         * Invoked when the end user clicked on the RewardedVideo ad
         */
    }
    override fun onAdLeftApplication(ad: Ad) {}
    override fun onAdScreenPresented(ad: Ad) {}
    override fun onAdScreenDismissed(ad: Ad) {}
}
bannerViewLayout.setLevelPlayBannerListener(bannerListener)
```

#### Step 3. Load Banner Ad Settings

To load a Banner ad with the default settings, call the following method:

```kotlin
IronSourceDecorator.loadBanner(banner)
```

We support placements, pacing and capping for Banners on the ironSource dashboard. Learn how to set up placements, capping and pacing for Banners to optimize your app’s user experience here.If you’ve set up placements for your Banner, call the following method to serve a Banner ad in a specific location:

```kotlin
IronSourceDecorator.loadBanner(banner, placement)
```


#### Done
You are now all set up to serve Banners in your application.

```kotlin
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import org.bidon.ironsource.IronSourceDecorator
import org.bidon.ironsource.banner.IronSourceLevelPlayBannerListener
import com.appodealstack.mads.demands.Ad
import com.appodealstack.mads.demands.banners.BannerSize

class MainActivity : FragmentActivity() {
    private val bannerListener = object : IronSourceLevelPlayBannerListener {
        override fun onAdLoaded(ad: Ad) {
            /**
             * Invoked when there is a change in the ad availability status.
             */
        }

        override fun onAdLoadFailed(cause: Throwable) {}

        override fun onAdClicked(ad: Ad) {
            /**
             * Invoked when the end user clicked on the RewardedVideo ad
             */
        }
        override fun onAdLeftApplication(ad: Ad) {}
        override fun onAdScreenPresented(ad: Ad) {}
        override fun onAdScreenDismissed(ad: Ad) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bannerViewLayout = IronSourceDecorator.createBanner(this, BannerSize.Banner)
        rootView.addView(bannerViewLayout)

        bannerViewLayout.setLevelPlayBannerListener(bannerListener)
        IronSourceDecorator.loadBanner(bannerViewLayout)
    }
}
```


### Stopping and Starting Auto Refresh

SDK handles Auto Refresh. By default auto refresh timeout is 10 sec. You can modify default this logic by using these methods:

```kotlin
// start auto refresh
bannerViewLayout.startAutoRefresh()

// stop auto refresh
bannerViewLayout.stopAutoRefresh()

//start auto refresh with timeout
bannerViewLayout.setAutoRefreshTimeout(TIMEOUT)
```
