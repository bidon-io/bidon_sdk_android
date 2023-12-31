# Integration

This page is describes how to import and configure the Bidon SDK. 

- [Getting Started](#getting-started) 
- [Initialize the SDK](#initialize-the-sdk)
- [Configure Ad Types](#configure-ad-types)
  
## Getting Started 

To integrate Bidon SDK through Dependencies, firstly add repository fo Bidon SDK dependencies
```ruby
repositories {

    // For using Bidon Artefactory
    maven { url = uri("https://artifactory.bidon.org/bidon") }
    
    // Alternatively, you have the option to download Bidon SDK from GitHub Packages using your credential
    maven {
        url = uri("https://maven.pkg.github.com/bidon-io/bidon-sdk-android")
        credentials {
            username = YOUR_GITHUB_USERNAME
            password = YOUR_GITHUB_TOKEN
        }
    }
}        
```

secondly add the following lines to your `build.gradle` (:app):

``` ruby
dependencies {
    # Bidon SDK Library
    implementation 'org.bidon:bidon-sdk:$BIDON_VERSION'

    # Demand Sources (AdNetworks)
    implementation 'org.bidon:bidmachine-adapter:$BIDON_ADAPTER_VERSION'
    implementation 'org.bidon:admob-adapter:$BIDON_ADAPTER_VERSION'
    implementation 'org.bidon:applovin-adapter:$BIDON_ADAPTER_VERSION'
    implementation 'org.bidon:dtexchange-adapter:$BIDON_ADAPTER_VERSION'
    implementation 'org.bidon:unityads-adapter:$BIDON_ADAPTER_VERSION'
    
    ... 
}

```
Then sync project.


## Initialize the SDK

Receive your `APP_KEY` in the dashboard app settings. Init Bidon SDK in your MainActivity class.

```kotlin
BidonSdk
    .registerDefaultAdapters()
    // .registerAdapters("com.example.YourOwnAdapterClass") // for registering your custom Adapter (AdNetwork) by class name
    // .registerAdapters(YourOwnAdapter()) // for registering your custom Adapter (AdNetwork) by instance. Instance should be initialized and ready to work
    .setBaseUrl("https://api.bidon.org")
    .setInitializationCallback {
        //  Bidon is initialized and ready to work
    }
    .initialize(
        activity = this@MainActivity,
        appKey = "APP_KEY",
    )
```

Set logging.
```kotlin
BidonSdk.setLogLevel(Logger.Level.Verbose)
```

## Configure Ad Types

- [Interstitials](ad-formats/interstitial.md)
- [Rewarded Ads](ad-formats/rewarded.md)
- [BannerManager](ad-formats/banner_manager.md)
- [Banners](ad-formats/banner.md)
