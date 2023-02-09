# Banners

## Loading an Banners

To load banner, create a `BDNBanner`.  Implement `BDNAdViewDelegate` that you are notified when your ad is ready and of other ad-related events. This argument can be ad revenue value from mediaton.

> Set `rootViewController` property before attemp to call loadAd method!

```swift
class ViewController: UIViewController {
    var banner: BidOn.Banner!
    
    func loadBanner() {
        banner = BidOn.Banner(frame: .zero)
        banner.rootViewController = self
        banner.format = .banner
        banner.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(banner)
        NSLayoutConstraint.activate([
            banner.heightAnchor.constraint(equalToConstant: 50),
            banner.bottomAnchor.constraint(equalTo: view.safeAreaLayoutGuide.bottomAnchor),
            banner.leftAnchor.constraint(equalTo: view.leftAnchor),
            banner.rightAnchor.constraint(equalTo: view.rightAnchor)
        ])
        
        banner.loadAd(with: 0.1)
    }
}


extension ViewController: BidOn.AdViewDelegate {
    func adView(_ adView: UIView & BidOn.AdView, willPresentScreen ad: BidOn.Ad) {}
    
    func adView(_ adView: UIView & BidOn.AdView, didDismissScreen ad: BidOn.Ad) {}
    
    func adView(_ adView: UIView & BidOn.AdView, willLeaveApplication ad: BidOn.Ad) {}
    
    func adObject(_ adObject: BidOn.AdObject, didLoadAd ad: BidOn.Ad) {}
    
    func adObject(_ adObject: BidOn.AdObject, didFailToLoadAd error: Error) {}
}
```

```obj-c
#import "ViewController.h"
#import <AppLovinSDK/AppLovinSDK.h>
#import <BidOn/BidOn.h>


@interface ViewController() <BDNAdViewDelegate>

@property (nonatomic, strong) BDNBanner *banner;

@end


@implementation ViewController

- (void)createBanner {
    self.banner = [[BDNBanner alloc] initWithFrame: CGRectZero];
    self.banner.format = BDNAdViewFormatBanner;
    self.banner.rootViewController = self;
    self.banner.delegate = self;
    self.banner.translatesAutoresizingMaskIntoConstraints = NO;
    
    [self.view addSubview: self.banner];
    [NSLayoutConstraint activateConstraints:@[
        [self.banner.heightAnchor constraintEqualToConstant:50],
        [self.banner.bottomAnchor constraintEqualToAnchor:self.view.safeAreaLayoutGuide.bottomAnchor],
        [self.banner.leftAnchor constraintEqualToAnchor:self.view.leftAnchor],
        [self.banner.rightAnchor constraintEqualToAnchor:self.view.rightAnchor]
    ]];
    
    [self.banner loadAdWith:0.1];
}

#pragma mark - BDNAdViewDelegate Protocol

- (void)adObject:(id<BDNAdObject>)adObject didFailToLoadAd:(NSError *)error {}

- (void)adObject:(id<BDNAdObject>)adObject didLoadAd:(id<BNAd>)ad {}

- (void)adView:(UIView<BDNAdView> *)adView didDismissScreen:(id<BNAd>)ad {}

- (void)adView:(UIView<BDNAdView> *)adView willLeaveApplication:(id<BNAd>)ad {}

- (void)adView:(UIView<BDNAdView> *)adView willPresentScreen:(id<BNAd>)ad {}

@end
```

## Ad View Format

| Ad View Format | Size | Description  |
|---|---|---|
| banner  | 320 x 50   | Fixed size banner for phones |
| leaderboard | 728 x 90  | Fixed size banner for pads  |
| mrec | 300 x 250 | Fixed medium rectangle banners  |
| adaptive | -/- x 50/90 | Flexible width banners |
