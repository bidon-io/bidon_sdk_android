-keeppackagenames org.bidon.sdk.**
-keep, includedescriptorclasses
    class org.bidon.sdk.** { *;}

-keep class org.bidon.sdk.adapter.Adapter {
    public <methods>;
}
-keep public class org.bidon.sdk.BidonSdk { *; }
-keep public interface org.bidon.sdk.logs.logging.Logger { *; }
-keep public enum org.bidon.sdk.logs.logging.Logger$Level { *; }
-keep enum org.bidon.sdk.config.DefaultAdapters { *; }
-keep public class org.bidon.sdk.utils.networking.NetworkSettings { *; }
-keep public class org.bidon.sdk.config.InitializationCallback { *; }
-keep public class org.bidon.sdk.config.BidonError$** { *; }

# Ad types
-keep public interface org.bidon.sdk.ads.AdListener { *; }
-keep public interface org.bidon.sdk.ads.FullscreenAdListener { *; }

-keep public interface org.bidon.sdk.ads.banner.BannerAd { *; }
-keep public interface org.bidon.sdk.ads.banner.PositionedBanner { *; }
-keep public class org.bidon.sdk.ads.banner.BannerManager { *; }
-keep public enum org.bidon.sdk.ads.banner.BannerFormat { *; }
-keep public class org.bidon.sdk.ads.banner.BannerView { *; }
-keep public interface org.bidon.sdk.ads.banner.BannerListener { *; }

-keep public interface org.bidon.sdk.ads.interstitial.Interstitial { *; }
-keep public class org.bidon.sdk.ads.interstitial.InterstitialAd { *; }
-keep public class org.bidon.sdk.ads.interstitial.InterstitialAd$DefaultImpls { *; }
-keep public interface org.bidon.sdk.ads.interstitial.InterstitialListener { *; }

-keep public interface org.bidon.sdk.ads.rewarded.Rewarded { *; }
-keep public class org.bidon.sdk.ads.rewarded.RewardedAd { *; }
-keep public class org.bidon.sdk.ads.rewarded.RewardedAd$DefaultImpls { *; }
-keep public interface org.bidon.sdk.ads.rewarded.RewardedListener { *; }
-keep public interface org.bidon.sdk.ads.rewarded.RewardedAdListener { *; }
-keep public class org.bidon.sdk.ads.rewarded.Reward { *; }

-keep public enum org.bidon.sdk.ads.AdType { *; }
-keep public class org.bidon.sdk.ads.Ad { *; }
-keep public class org.bidon.sdk.ads.AuctionInfo { *; }
-keep public class org.bidon.sdk.adapter.DemandAd { *; }
-keep public class org.bidon.sdk.adapter.DemandId { *; }

-keep public interface org.bidon.sdk.adapter.Adapter { *; }
-keep public interface org.bidon.sdk.adapter.AdapterParameters
-keep public interface org.bidon.sdk.adapter.AdAuctionParams
-keep public interface org.bidon.sdk.adapter.AdProvider { *; }
-keep public interface org.bidon.sdk.adapter.AdProvider$Interstitial { *; }
-keep public interface org.bidon.sdk.adapter.AdProvider$Rewarded { *; }
-keep public interface org.bidon.sdk.adapter.AdProvider$Banner { *; }
-keep public interface org.bidon.sdk.adapter.AdSource { *;}
-keep public interface org.bidon.sdk.adapter.AdSource$** { *;}
-keep public interface org.bidon.sdk.adapter.AdState$** { *; }
-keep public interface org.bidon.sdk.adapter.Initializable { *; }
-keep public interface org.bidon.sdk.adapter.WinLossNotifiable {*;}
-keep public class org.bidon.sdk.adapter.AdapterInfo { *; }
-keep public class org.bidon.sdk.auction.models.AuctionResult { *; }
-keep public class org.bidon.sdk.auction.models.AdUnit { *; }
-keep public class org.bidon.sdk.logs.logging.impl.LogExtKt {
    void logError(java.lang.String, java.lang.String, java.lang.Throwable);
    void logInfo(java.lang.String, java.lang.String);
}

-keep public interface org.bidon.sdk.logs.analytic.AdRevenueListener { *; }
-keep public interface org.bidon.sdk.logs.analytic.AdRevenueLogger { *; }
-keep public interface org.bidon.sdk.logs.analytic.AdValue { *; }
-keep public interface org.bidon.sdk.logs.analytic.Precision { *; }
