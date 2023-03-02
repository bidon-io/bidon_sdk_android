# Integration

This page is describes how to import and configure the Bidon SDK. 

- [Getting Started](#getting-started) 
- [Initialize the SDK](#initialize-the-sdk)
- [Configure Ad Types](#configure-ad-types)
  
## Getting Started 

To integrate Bidon SDK through Dependencies, firstly add repository fo Bidon SDK dependencies
```ruby
repositories {
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
    implementation 'org.bidon:bidon-sdk:0.1.0-Beta'

    # Demand Sources (AdNetworks)
    implementation 'org.bidon:bidmachine-adapter:0.1.0.1-Beta'
    implementation 'org.bidon:admob-adapter:0.1.0.1-Beta'
    implementation 'org.bidon:applovin-adapter:0.1.0.1-Beta'
    implementation 'org.bidon:dtexchange-adapter:0.1.0.1-Beta'
    implementation 'org.bidon:unityads-adapter:0.1.0.1-Beta'
    
    ... 
}

```
Then sync project.


## Initialize the SDK

Receive your `APP_KEY` in the dashboard app settings. Init Bidon SDK in your MainActivity class.

```kotlin
Bidon
    .registerDefaultAdapters()
    // .registerAdapters("com.example.YourOwnAdapterClass") // for registering your custom Adapter (AdNetwork) by class name
    // .registerAdapters(YourOwnAdapter()) // for registering your custom Adapter (AdNetwork) by instance. Instance should be initialized and ready to work
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
Bidon.setLogLevel(Logger.Level.Verbose)
```

## Configure Ad Types

- [Interstitials](ad-formats/interstitial.md)
- [Rewarded Ads](ad-formats/rewarded.md)
- [Banners](ad-formats/banner.md)
