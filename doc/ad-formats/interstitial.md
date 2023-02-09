# Interstitials

## Loading an Interstitial Ad

To load an interstitial ad, instantiate an `BDNInterstitial` with `placement` configured in the app settings. Implement `BDNFullscreenAdDelegate` that you are notified when your ad is ready and of other ad-related events. In the load method you will need to specify the pricefloor. This argument can be ad revenue value from mediaton.

```swift
class ViewController: UIViewController {
    var interstitialAd: BidOn.Interstitial!
    
    func loadInterstitialAd() {
        interstitialAd = BidOn.Interstitial(placement: "PLACEMENT")
        interstitialAd.delegate = self
        
        interstitialAd.loadAd(with: 0.1)
    }
}


extension ViewController: BidOn.FullscreenAdDelegate {
    func adObject(_ adObject: BidOn.AdObject, didLoadAd ad: BidOn.Ad) {}
    
    func adObject(_ adObject: BidOn.AdObject, didFailToLoadAd error: Error) {}
    
    func fullscreenAd(_ fullscreenAd: BidOn.FullscreenAdObject, willPresentAd ad: BidOn.Ad) {}
    
    func fullscreenAd(_ fullscreenAd: BidOn.FullscreenAdObject, didFailToPresentAd error: Error) {}
    
    func fullscreenAd(_ fullscreenAd: BidOn.FullscreenAdObject, didDismissAd ad: BidOn.Ad) {}
}
```

```obj-c
#import "ViewController.h"
#import <BidOn/BidOn.h>


@interface ViewController() <BDNFullscreenAdDelegate>

@property (nonatomic, strong) BDNInterstitial *interstitial;

@end


@implementation ViewController

- (void)loadInterstitialAd {
    self.interstitial = [[BDNInterstitial alloc] initWithPlacement:@"PLACEMENT"];
    self.interstitial.delegate = self;

    [self.interstitial loadAdWith:0.1];
}

#pragma mark - BDNFullscreenAdDelegate

- (void)adObject:(id<BDNAdObject>)adObject didFailToLoadAd:(NSError *)error {}

- (void)adObject:(id<BDNAdObject>)adObject didLoadAd:(id<BNAd>)ad {}

- (void)fullscreenAd:(id<BDNFullscreenAd>)fullscreenAd didDismissAd:(id<BNAd>)ad {}

- (void)fullscreenAd:(id<BDNFullscreenAd>)fullscreenAd didFailToPresentAd:(NSError *)error {}

- (void)fullscreenAd:(id<BDNFullscreenAd>)fullscreenAd willPresentAd:(id<BNAd>)ad {}

@end
```

## Showing an Interstitial Ad

```swift
func showInterstitialAd() {
    guard interstitialAd.isReady else { return }
    interstitialAd.show(from: self)
}
```

```obj-c
- (void)showInterstitialAd {
    if ([self.interstitial isReady]) {
        [self.interstitial showFrom:self];
    }
}
```
