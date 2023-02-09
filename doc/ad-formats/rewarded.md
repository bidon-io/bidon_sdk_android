# Rewarded Ads

## Loading an Rewarded Ad

To load an interstitial ad, instantiate an `BDNRewardedAd` with `placement` configured in the app settings. Implement `BDNRewardedAdDelegate` that you are notified when your ad is ready and of other ad-related events. In the load method you will need to specify the pricefloor. This argument can be ad revenue value from mediaton.

```swift
class ViewController: UIViewController {
    var rewardedAd: BidOn.RewardedAd!
    
    func loadRewardedAd() {
        rewardedAd = BidOn.RewardedAd(placement: "PLACEMENT")
        rewardedAd.delegate = self
        
        rewardedAd.loadAd(with: 0.1)
    }
}


extension ViewController: BidOn.RewardedAdDelegate {
    func adObject(_ adObject: BidOn.AdObject, didLoadAd ad: BidOn.Ad) {}
    
    func adObject(_ adObject: BidOn.AdObject, didFailToLoadAd error: Error) {}
    
    func fullscreenAd(_ fullscreenAd: BidOn.FullscreenAdObject, willPresentAd ad: BidOn.Ad) {}
    
    func fullscreenAd(_ fullscreenAd: BidOn.FullscreenAdObject, didFailToPresentAd error: Error) {}
    
    func fullscreenAd(_ fullscreenAd: BidOn.FullscreenAdObject, didDismissAd ad: BidOn.Ad) {}
    
    func rewardedAd(_ rewardedAd: BidOn.RewardedAdObject, didRewardUser reward: BidOn.Reward) {}
}
```

```obj-c
#import "ViewController.h"
#import <BidOn/BidOn.h>


@interface ViewController() <BDNRewardedAdDelegate>

@property (nonatomic, strong) BDNRewardedAd *rewardedAd;

@end


@implementation ViewController

- (void)loadRewardedAd {
    self.rewardedAd = [[BDNRewardedAd alloc] initWithPlacement:@"PLACEMENT"];
    self.rewardedAd.delegate = self;

    [self.rewardedAd loadAdWith:0.1];
}

#pragma mark - BDNRewardedAdDelegate

- (void)adObject:(id<BDNAdObject>)adObject didFailToLoadAd:(NSError *)error {}

- (void)adObject:(id<BDNAdObject>)adObject didLoadAd:(id<BNAd>)ad {}

- (void)fullscreenAd:(id<BDNFullscreenAd>)fullscreenAd didDismissAd:(id<BNAd>)ad {}

- (void)fullscreenAd:(id<BDNFullscreenAd>)fullscreenAd didFailToPresentAd:(NSError *)error {}

- (void)fullscreenAd:(id<BDNFullscreenAd>)fullscreenAd willPresentAd:(id<BNAd>)ad {}

- (void)rewardedAd:(id<BDNRewardedAd>)rewardedAd didRewardUser:(id<BNReward>)reward {}

@end
```

## Showing an Rewarded Ad

```swift
func showRewardedAd() {
    guard rewardedAd.isReady else { return }
    rewardedAd.show(from: self)
}
```

```obj-c
- (void)showRewardedAd {
    if ([self.rewardedAd isReady]) {
        [self.rewardedAd showFrom:self];
    }
}
```

## Handle a User Reward

Once a Rewarded Ad did reward user, delegate method `rewardedAd:didRewardUser` will fire. You will be able to receive reward amount and currency.

```swift
func rewardedAd(_ rewardedAd: BidOn.RewardedAdObject, didRewardUser reward: BidOn.Reward) {
    let amount: Int = reward.amount
    let currency: String = reward.label
} 
```

```obj-c
- (void)rewardedAd:(id<BDNRewardedAd>)rewardedAd didRewardUser:(id<BNReward>)reward {
    NSInteger amount = [reward amount];
    NSString *currenct = [reward label];
}
```
