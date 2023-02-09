# Integration

This page is describes how to donwload, import and configure the BidOn SDK. 

- [Integration](#integration)
  - [Download](#download)
    - [CocoaPods (Recommended)](#cocoapods-recommended)
    - [Manual](#manual)
  - [Initialize the SDK](#initialize-the-sdk)
  - [Configure Ad Types](#configure-ad-types)
  
## Download 

### CocoaPods (Recommended)

To integrate the BidOn SDK through CocoaPods, first add the following lines to your Podfile:

``` ruby
pod 'BidOn'

# For usage of Demand Sources uncomment following lines
# pod 'BidOnAdapterBidMachine'
# pod 'BidOnGoogleMobileAds'
# pod 'BidOnAdapterAppLovin'

```

Then run the following on the command line:

``` ruby
pod install --repo-update
```

### Manual

> TODO:// Manual integration guiode

## Initialize the SDK

Receive your `app key` in the dashboard app settings. We highly recommend to initialize the BidOn SDK in app delegate's `application:applicationDidFinishLaunching:` method. 

`swift`
```swift
import BidOn

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate
{
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool
    {
        // Register all available demand source adapters.
        BidOnSdk.registerDefaultAdapters()    
        // Configure BidOn
        BidOnSdk.logLevel = .debug
        // Initialize
        BidOnSdk.initialize(appKey: "APP KEY") {
            // Load any ads
        }

        ⋮
```

```obj-c
#import <BidOn/BidOn.h>

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {
    // Register all available demand source adapters.
    [BDNSdk registerDefaultAdapters];
    // Configure BidOn
    [BDNSdk setLogLevel:BDNLoggerLevelDebug];
    // Initialize
    [BDNSdk initializeWithAppKey:@"APP KEY" completion:^{
        // Load any ads
    }];

    ⋮
```

## Configure Ad Types

- [Interstitials](/ad-formats/interstitials.md)
- [Rewarded Ads](/ad-formats/rewarded.md)
- [Banners](/ad-formats/banner.md)
