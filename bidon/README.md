# AppLovin MAX + Bidon

### Table of contents

  - [Installation](#installation)
    - [CocoaPods](#cocoapods-recommended)
    - [Manual](#manual)
  - [Demand Sources](#demand-sources)
  - [MMP](#mmp)
  - [Initialize](#initialize-the-sdk)
  - [Interstitial](#interstitial)
  - [Rewarded Video](#rewarded-video)
  - [Banner](#banner)
  
>  A project should already has integrated [AppLovin MAX](https://dash.applovin.com/documentation/mediation/ios).

## Installation

### CocoaPods (Recommended)

To integrate the AppLovin Decorator through CocoaPods, first add the following lines to your Podfile:

``` ruby
pod 'Bidon/AppLovinDecorator'

# For usage of Demand Sources uncomment following lines
# pod 'Bidon/BidMachineAdapter'
# pod 'Bidon/GoogleMobileAdsAdapter'

# For usage of MMP uncomment following lines
# pod 'Bidon/AppsFlyerAdapter'

```

Then run the following on the command line:

``` ruby
pod install --repo-update
```

### Manual

> TODO:// Manual integration guiode

## Demand Sources

For using of BidMachine and GoogleMobileAds SDK in postbid you will need to register their's adapters before initialization of the SDK

_Swift_ example

```swift
import AppLovinSDK
import AppLovinDecorator
import GoogleMobileAdsAdapter
import BidMachineAdapter

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate
{
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool
    {
        ⋮
        
        ALSdk.shared()!.bid.register(
            adapter: BidMachineDemandSourceAdapter.self,
            parameters: BidMachineParameters(
                sellerId: "BidMachine seller id"
            )
        )
        
        ALSdk.shared()!.bid.register(
            adapter: GoogleMobileAdsDemandSourceAdapter.self,
            parameters: GoogleMobileAdsParameters(
                interstitial: [
                    LineItem(0.1, adUnitId: "AdMob interstitial ad unit id"),
                ],
                rewardedAd: [
                    LineItem(0.1, adUnitId: "AdMob rewarded ad unit id"),
                ],
                banner: [
                    LineItem(0.1, adUnitId: "AdMob banner ad unit id"),
                ]
            )
        )

        ⋮

        ALSdk.shared()!.bid.initializeSdk { (configuration: ALSdkConfiguration) in
            // Start loading ads
        }

        ⋮
```

_Objective C_ example

```objc
#import <AppLovinSDK/AppLovinSDK.h>
#import <AppLovinDecorator/AppLovinDecorator.h>
#import <GoogleMobileAdsAdapter/GoogleMobileAdsAdapter.h>
#import <BidMachineAdapter/BidMachineAdapter.h>

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    ⋮

    // BidMachine
    NSDictionary *bidMachineParameters = @{
        @"seller_id": @"BidMachine seller id"
    };
    NSData *bidMachineParametersRaw = [NSJSONSerialization dataWithJSONObject:bidMachineParameters options:0 error:&error];
    BidMachineDemandSourceAdapter *bidMachineAdapter = [[BidMachineDemandSourceAdapter alloc] initWithRawParameters:bidMachineParametersRaw error:&error];
    
    [[[ALSdk shared] bid] registerWithAdapter:(id<Adapter>)bidMachineAdapter error:&error];

    // GoogleMobileAds
    NSDictionary *adMobParameters = @{
        @"line_items": @{
            @"interstitial": @[
                @{
                    @"pricefloor": @0.01,
                    @"ad_unit_id": @"AdMob interstitial ad unit id"
                }
            ],
            @"rewarded_ad": @[
                @{
                    @"pricefloor": @0.01,
                    @"ad_unit_id": @"AdMob rewarded ad unit id"
                }
            ],
            @"banner": @[
                @{
                    @"pricefloor": @0.01,
                    @"ad_unit_id": @"AdMob banner ad unit id"
                }
            ]
        }
    };
    
    NSData *adMobParametersRaw = [NSJSONSerialization dataWithJSONObject:adMobParameters options:0 error:&error];
    GoogleMobileAdsDemandSourceAdapter *adMobAdapter = [[GoogleMobileAdsDemandSourceAdapter alloc] initWithRawParameters:adMobParametersRaw error:&error];
    
    [[[ALSdk shared] bid] registerWithAdapter:(id<Adapter>)adMobAdapter error:&error];

    ⋮

    [[[ALSdk shared] bid] initializeSdkWithCompletionHandler:^(ALSdkConfiguration *configuration) {
        // Start loading ads
    }];

    ⋮
```

## MMP

For using of AppsFlyer as ad revenue tracking partner you will need to register its adapter before initialization of the SDK

_Swift_ example

```swift
import AppLovinSDK
import AppLovinDecorator
import GoogleMobileAdsAdapter
import BidMachineAdapter

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate
{
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool
    {
        ⋮
        
         ALSdk.shared()!.bid.register(
            adapter: AppsFlyerMobileMeasurementPartnerAdapter.self,
            parameters: AppsFlyerParameters(
                devKey: "some key",
                appId: "some app id"
            )
        )

        ⋮

        ALSdk.shared()!.bid.initializeSdk { (configuration: ALSdkConfiguration) in
            // Start loading ads
        }

        ⋮
```

_Objective C_ example

```objc
#import <AppLovinSDK/AppLovinSDK.h>
#import <AppLovinDecorator/AppLovinDecorator.h>
#import <GoogleMobileAdsAdapter/GoogleMobileAdsAdapter.h>
#import <AppsFlyerAdapter/AppsFlyerAdapter.h>

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    ⋮

    NSDictionary *appsFlyerParameters = @{
        @"dev_key": @"dev key",
        @"app_id": @"app id"
    };
    
    NSData *appsFlyerParametersRaw = [NSJSONSerialization dataWithJSONObject:appsFlyerParameters options:0 error:&error];
    AppsFlyerMobileMeasurementPartnerAdapter *appsFlyerAdapter = [[AppsFlyerMobileMeasurementPartnerAdapter alloc] initWithRawParameters:appsFlyerParametersRaw error:&error];
        
    [[[ALSdk shared] bid] registerWithAdapter:(id<Adapter>)appsFlyerAdapter error:&error];

    ⋮

    [[[ALSdk shared] bid] initializeSdkWithCompletionHandler:^(ALSdkConfiguration *configuration) {
        // Start loading ads
    }];

    ⋮
```

## Initialize the SDK

_Swift_ example

```swift
import AppLovinSDK
import AppLovinDecorator

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate
{
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool
    {
        // Please make sure to set the mediation provider value to "max" to ensure proper functionality
        ALSdk.shared()!.mediationProvider = "max"
        
        ALSdk.shared()!.userIdentifier = "USER_ID"
        
        // Setup Demand Sources and MMP
        ⋮
        
        ALSdk.shared()!.bid.initializeSdk { (configuration: ALSdkConfiguration) in
            // Start loading ads
        }

        ⋮
```

_Objective C_ example

```objc
#import <AppLovinSDK/AppLovinSDK.h>
#import <AppLovinDecorator/AppLovinDecorator.h>

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
    // Please make sure to set the mediation provider value to @"max" to ensure proper functionality
    [ALSdk shared].mediationProvider = @"max";
    
    [ALSdk shared].userIdentifier = @"USER_ID";
    
    // Setup Demand Sources and MMP
    ⋮

    [[[ALSdk shared] bid] initializeSdkWithCompletionHandler:^(ALSdkConfiguration *configuration) {
        // Start loading ads
    }];

    ⋮
```

## Interstitial

### Loading an Interstitial ad

To load an interstitial ad, instantiate an `BNMAInterstitialAdobject` corresponding to your ad unit and call its `loadAdmethod`. Implement `BNMAAdDelegate` so that you are notified when your ad is ready and of other ad-related events.

_Swift_ example

```swift
import AppLovinDecorator
import MobileAdvertising
import AppLovinSDK

class ExampleViewController: UIViewController, BNMAAdDelegate
{
    var interstitialAd: BNMAInterstitialAd!
    var retryAttempt = 0.0

    func createInterstitialAd()
    {
        interstitialAd = BNMAInterstitialAd(adUnitIdentifier: "YOUR_AD_UNIT_ID")
        interstitialAd.delegate = self

        // Load the first ad
        interstitialAd.load()
    }

    // MARK: BNMAAdDelegate Protocol

    func didLoad(_ ad: Ad)
    {
        // Interstitial ad is ready to be shown. 'interstitialAd.isReady' will now return 'true'
        
        // Reset retry attempt
        retryAttempt = 0
    }

    func didFailToLoadAd(forAdUnitIdentifier adUnitIdentifier: String, withError error: Error)
    {
        // Interstitial ad failed to load 
        // We recommend retrying with exponentially higher delays up to a maximum delay (in this case 64 seconds)
        
        retryAttempt += 1
        let delaySec = pow(2.0, min(6.0, retryAttempt))
        
        DispatchQueue.main.asyncAfter(deadline: .now() + delaySec) {
            self.interstitialAd.load()
        }
    }

    func didDisplay(_ ad: Ad) {}

    func didClick(_ ad: Ad) {}

    func didHide(_ ad: Ad)
    {
        // Interstitial ad is hidden. Pre-load the next ad
        interstitialAd.load()
    }

    func didFail(toDisplay ad: Ad, withError error: Error)
    {
        // Interstitial ad failed to display. We recommend loading the next ad
        interstitialAd.load()
    }
}
```

_Objective C_ example

```objc
#import "ExampleViewController.h"
#import <AppLovinSDK/AppLovinSDK.h>
#import <AppLovinDecorator/AppLovinDecorator.h>
#import <MobileAdvertising/MobileAdvertising.h>


@interface ExampleViewController()<BNMAAdDelegate>
@property (nonatomic, strong) BNMAInterstitialAd *interstitialAd;
@property (nonatomic, assign) NSInteger retryAttempt;
@end

@implementation ExampleViewController

- (void)createInterstitialAd
{
    self.interstitialAd = [[BNMAInterstitialAd alloc] initWithAdUnitIdentifier: @"YOUR_AD_UNIT_ID"];
    self.interstitialAd.delegate = self;

    // Load the first ad
    [self.interstitialAd loadAd];
}

#pragma mark - MAAdDelegate Protocol

- (void)didLoadAd:(id<Ad>)ad
{
    // Interstitial ad is ready to be shown. '[self.interstitialAd isReady]' will now return 'YES'

    // Reset retry attempt
    self.retryAttempt = 0;
}

- (void)didFailToLoadAdForAdUnitIdentifier:(NSString *)adUnitIdentifier withError:(NSError *)error
{
    // Interstitial ad failed to load
    // We recommend retrying with exponentially higher delays up to a maximum delay (in this case 64 seconds)
    
    self.retryAttempt++;
    NSInteger delaySec = pow(2, MIN(6, self.retryAttempt));
    
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, delaySec * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
        [self.interstitialAd loadAd];
    });
}

- (void)didDisplayAd:(id<Ad>)ad {}

- (void)didClickAd:(id<Ad>)ad {}

- (void)didHideAd:(id<Ad>)ad
{
    // Interstitial ad is hidden. Pre-load the next ad
    [self.interstitialAd loadAd];
}

- (void)didFailToDisplayAd:(id<Ad>)ad withError:(NSError *)error
{
    // Interstitial ad failed to display. We recommend loading the next ad
    [self.interstitialAd loadAd];
}

@end
```

### Showing an Interstitial ad

To show an interstitial ad, call `showAd` on the `BNMAInterstitialAd` object that you instantiated.

_Swift_ example

```swift
if interstitialAd.isReady
{
    interstitialAd.show()
}
```

_Objective C_ example

```objc
if ( [self.interstitialAd isReady] )
{
    [self.interstitialAd showAd];
}
```

## Rewarded Video

### Loading a Rewarded Ad

To load a rewarded ad, get an instance of a `BNMARewardedAd` object that corresponds to your rewarded ad unit and then call its `loadAd` method. Implement `BNMARewardedAdDelegate` so that you are notified when your ad is ready and of other ad-related events.

_Swift_ example

```swift
import AppLovinDecorator
import MobileAdvertising
import AppLovinSDK

class ExampleViewController : UIViewController, BNMARewardedAdDelegate
{
    var rewardedAd: BNMARewardedAd!
    var retryAttempt = 0.0

    func createRewardedAd()
    {
        rewardedAd = BNMARewardedAd.shared(withAdUnitIdentifier: "YOUR_AD_UNIT_ID")
        rewardedAd.delegate = self

        // Load the first ad
        rewardedAd.load()
    }

    // MARK: BNMAAdDelegate Protocol

    func didLoad(_ ad: Ad)
    {
        // Rewarded ad is ready to be shown. '[self.rewardedAd isReady]' will now return 'YES'
        
        // Reset retry attempt
        retryAttempt = 0
    }

    func didFailToLoadAd(forAdUnitIdentifier adUnitIdentifier: String, withError error: Error)
    {
        // Rewarded ad failed to load 
        // We recommend retrying with exponentially higher delays up to a maximum delay (in this case 64 seconds)
        
        retryAttempt += 1
        let delaySec = pow(2.0, min(6.0, retryAttempt))
        
        DispatchQueue.main.asyncAfter(deadline: .now() + delaySec) {
            self.rewardedAd.load()
        }
    }

    func didDisplay(_ ad: Ad) {}

    func didClick(_ ad: Ad) {}

    func didHide(_ ad: Ad)
    {
        // Rewarded ad is hidden. Pre-load the next ad
        rewardedAd.load()
    }

    func didFail(toDisplay ad: Ad, withError error: Error)
    {
        // Rewarded ad failed to display. We recommend loading the next ad
        rewardedAd.load()
    }

    // MARK: BNMARewardedAdDelegate Protocol

    func didStartRewardedVideo(for ad: Ad) {}

    func didCompleteRewardedVideo(for ad: Ad) {}

    func didRewardUser(for ad: Ad, with reward: Reward)
    {
        // Rewarded ad was displayed and user should receive the reward
    }
}
```

_Objective C_ example

```objc
#import "ExampleViewController.h"
#import <AppLovinSDK/AppLovinSDK.h>
#import <AppLovinDecorator/AppLovinDecorator.h>
#import <MobileAdvertising/MobileAdvertising.h>

@interface ExampleViewController()<BNMARewardedAdDelegate>
@property (nonatomic, strong) BNMARewardedAd *rewardedAd;
@property (nonatomic, assign) NSInteger retryAttempt;
@end

@implementation ExampleViewController

- (void)createRewardedAd
{
    self.rewardedAd = [BNMARewardedAd sharedWithAdUnitIdentifier: @"YOUR_AD_UNIT_ID"];
    self.rewardedAd.delegate = self;

    // Load the first ad
    [self.rewardedAd loadAd];
}

#pragma mark - BNMAAdDelegate Protocol

- (void)didLoadAd:(id<Ad>)ad
{
    // Rewarded ad is ready to be shown. '[self.rewardedAd isReady]' will now return 'YES'
    
    // Reset retry attempt
    self.retryAttempt = 0;
}

- (void)didFailToLoadAdForAdUnitIdentifier:(NSString *)adUnitIdentifier withError:(NSError *)error
{
    // Rewarded ad failed to load 
    // We recommend retrying with exponentially higher delays up to a maximum delay (in this case 64 seconds)
    
    self.retryAttempt++;
    NSInteger delaySec = pow(2, MIN(6, self.retryAttempt));
    
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, delaySec * NSEC_PER_SEC), dispatch_get_main_queue(), ^{
        [self.rewardedAd loadAd];
    });
}

- (void)didDisplayAd:(id<Ad>)ad {}

- (void)didClickAd:(id<Ad>)ad {}

- (void)didHideAd:(id<Ad>)ad
{
    // Rewarded ad is hidden. Pre-load the next ad
    [self.rewardedAd loadAd];
}

- (void)didFailToDisplayAd:(id<Ad>)ad withError:(NSError *)error
{
    // Rewarded ad failed to display. We recommend loading the next ad
    [self.rewardedAd loadAd];
}

#pragma mark - BNMARewardedAdDelegate Protocol

- (void)didStartRewardedVideoFor:(id<Ad>)ad {}

- (void)didCompleteRewardedVideoFor:(id<Ad>)ad {}

- (void)didRewardUserFor:(id<Ad>)ad withReward:(id<Reward>)reward
{
    // Rewarded ad was displayed and user should receive the reward
}

@end
```

### Showing a Rewarded Ad

To show a rewarded ad, call `showAd` on the `BNMARewardedAd` object you retrieved.

_Swift_ example

```swift
func didRewardUser(for ad: Ad, with reward: Reward)
{
    print("Rewarded user: \(reward.amount) \(reward.label)")
}
```

_Objective C_ example

```objc
- (void)didRewardUserForAd:(id<Ad>)ad withReward:(id<Reward>)reward
{
    NSLog(@"Rewarded user: %d %@", reward.amount, reward.label);
}
```

### Accessing the Amount and Currency for a Rewarded Ad

To access the reward amount and currency, override the `-[BNMARewardedAdDelegate didRewardUserForAd:withReward:]` callback:

```swift
if rewardedAd.isReady
{
    rewardedAd.show()
}
```

_Objective C_ example

```objc
if ( [self.rewardedAd isReady] )
{
    [self.rewardedAd show];
}
```

## Banner

> // TODO: Add docs for banners
