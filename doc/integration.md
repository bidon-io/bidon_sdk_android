# Integration

This page is describes how to import and configure the BidOn SDK. 

- [Getting Started](#getting-started) 
- [Initialize the SDK](#initialize-the-sdk)
- [Configure Ad Types](#configure-ad-types)
  
## Getting Started 

To integrate BidOn SDK through Dependencies, first add the following lines to your `build.gradle` (:app):

``` ruby
dependencies {
    # BidOn SDK Library
    implementation 'com.appodealstack.bidon:bidon-sdk:0.1.0'

    # Demand Sources (AdNetworks)
    implementation 'com.appodealstack.bidon:bidmachine-adapter:0.1.0'
    implementation 'com.appodealstack.bidon:admob-adapter:0.1.0'
    
    ... 
}

```
Then sync project.


## Initialize the SDK

Receive your `APP_KEY` in the dashboard app settings. Init Bidon SDK in your MainActivity class.

```kotlin
BidOn
    .setDefaultAdapters()
    // .setAdapters(YourOwnAdapter()) // for registering your custom Adapter (AdNetwork)
    .setInitializationCallback {
        //  BidOn is initialized and ready to work
    }
    .init(
        activity = this@MainActivity,
        appKey = "APP_KEY",
    )
```

## Configure Ad Types

- [Interstitials](/ad-formats/interstitials.md)
- [Rewarded Ads](/ad-formats/rewarded.md)
- [Banners](/ad-formats/banner.md)
