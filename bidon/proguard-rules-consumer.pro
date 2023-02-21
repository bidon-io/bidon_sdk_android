-keeppackagenames com.appodealstack.bidon.**
-keep class com.appodealstack.bidon.adapter.Adapter {
    public <methods>;
}
-keep class com.appodealstack.bidon.BidOn { *; }
-keep public interface com.appodealstack.bidon.BidOnSdk { *; }
-keep public interface com.appodealstack.bidon.BidOnBuilder { *; }
-keep public interface com.appodealstack.bidon.logs.logging.Logger { *; }
-keep public enum com.appodealstack.bidon.logs.logging.Logger$Level { *; }
-keep enum com.appodealstack.bidon.config.DefaultAdapters { *; }
-keep public class com.appodealstack.bidon.utils.networking.NetworkSettings { *; }
-keep public class com.appodealstack.bidon.config.InitializationCallback { *; }
-keep public class com.appodealstack.bidon.config.BidonError$** { *; }

# Ad types
-keep public interface com.appodealstack.bidon.ads.AdListener { *; }
-keep public interface com.appodealstack.bidon.ads.FullscreenAdListener { *; }
-keep public interface com.appodealstack.bidon.auction.AuctionListener { *; }
-keep public interface com.appodealstack.bidon.auction.RoundsListener { *; }

-keep public interface com.appodealstack.bidon.ads.banner.BannerAd { *; }
-keep public enum com.appodealstack.bidon.ads.banner.BannerFormat { *; }
-keep public class com.appodealstack.bidon.ads.banner.BannerView { *; }
-keep public interface com.appodealstack.bidon.ads.banner.BannerListener { *; }

-keep public class com.appodealstack.bidon.ads.interstitial.Interstitial { *; }
-keep public interface com.appodealstack.bidon.ads.interstitial.InterstitialAd { *; }
-keep public class com.appodealstack.bidon.ads.interstitial.InterstitialAd$DefaultImpls { *; }
-keep public interface com.appodealstack.bidon.ads.interstitial.InterstitialListener { *; }

-keep public class com.appodealstack.bidon.ads.rewarded.Rewarded { *; }
-keep public interface com.appodealstack.bidon.ads.rewarded.RewardedAd { *; }
-keep public class com.appodealstack.bidon.ads.rewarded.RewardedAd$DefaultImpls { *; }
-keep public interface com.appodealstack.bidon.ads.rewarded.RewardedListener { *; }
-keep public interface com.appodealstack.bidon.ads.rewarded.RewardedAdListener { *; }
-keep public class com.appodealstack.bidon.ads.rewarded.Reward { *; }

-keep public enum com.appodealstack.bidon.ads.AdType { *; }
-keep public class com.appodealstack.bidon.ads.Ad { *; }
-keep public class com.appodealstack.bidon.adapter.DemandAd { *; }
-keep public class com.appodealstack.bidon.adapter.DemandId { *; }

-keep public interface com.appodealstack.bidon.adapter.Adapter { *; }
-keep public interface com.appodealstack.bidon.adapter.AdapterParameters
-keep public interface com.appodealstack.bidon.adapter.AdAuctionParams
-keep public interface com.appodealstack.bidon.adapter.AdProvider { *; }
-keep public interface com.appodealstack.bidon.adapter.AdProvider$Interstitial { *; }
-keep public interface com.appodealstack.bidon.adapter.AdProvider$Rewarded { *; }
-keep public interface com.appodealstack.bidon.adapter.AdProvider$Banner { *; }
-keep public interface com.appodealstack.bidon.adapter.AdSource { *;}
-keep public interface com.appodealstack.bidon.adapter.AdSource$** { *;}
-keep public interface com.appodealstack.bidon.adapter.AdState$** { *; }
-keep public interface com.appodealstack.bidon.adapter.Initializable { *; }
-keep public interface com.appodealstack.bidon.adapter.WinLossNotifiable {*;}
-keep public class com.appodealstack.bidon.adapter.AdapterInfo { *; }
-keep public class com.appodealstack.bidon.auction.AuctionResult { *; }
-keep public class com.appodealstack.bidon.logs.logging.impl.LogExtKt {
    void logError(java.lang.String, java.lang.String, java.lang.Throwable);
    void logInfo(java.lang.String, java.lang.String);
}

-keep public interface com.appodealstack.bidon.logs.analytic.AdRevenueListener { *; }
-keep public interface com.appodealstack.bidon.logs.analytic.AdRevenueLogger { *; }
-keep public interface com.appodealstack.bidon.logs.analytic.AdValue { *; }
-keep public interface com.appodealstack.bidon.logs.analytic.Precision { *; }
