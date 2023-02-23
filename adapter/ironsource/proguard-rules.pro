-keepclassmembers class com.ironsource.sdk.controller.IronSourceWebView$JSInterface {
    public *;
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keep public class com.google.android.gms.ads.** {
   public *;
}
-keep class com.ironsource.adapters.** { *;
}
-dontwarn com.ironsource.mediationsdk.**
-dontwarn com.ironsource.adapters.**
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keeppackagenames org.bidon.ironsource.**
-keep class org.bidon.ironsource.**
-keep interface org.bidon.ironsource.ISDecorator {*;}
-keep interface org.bidon.ironsource.ISDecorator$Banner {*;}
-keep interface org.bidon.ironsource.ISDecorator$Banner$BannerView {*;}
-keep interface org.bidon.ironsource.ISDecorator$Impressions {*;}
-keep interface org.bidon.ironsource.ISDecorator$Initializer {*;}
-keep interface org.bidon.ironsource.ISDecorator$Interstitial {*;}
-keep interface org.bidon.ironsource.ISDecorator$Rewarded {*;}
-keep class org.bidon.ironsource.IronSourceDecorator {*;}
-keep class org.bidon.ironsource.IronSourceAdapter {*;}
-keep class org.bidon.ironsource.IronSourceParameters {*;}

-keep interface org.bidon.ironsource.rewarded.IronSourceRewardedListener {*;}
-keep interface org.bidon.ironsource.rewarded.IronSourceLevelPlayRewardedListener {*;}
-keep interface org.bidon.ironsource.interstitial.IronSourceInterstitialListener {*;}
-keep interface org.bidon.ironsource.interstitial.IronSourceLevelPlayInterstitialListener {*;}

-keep interface org.bidon.ironsource.banner.IronSourceBannerListener.** {*;}
-keep interface org.bidon.ironsource.banner.IronSourceLevelPlayBannerListener {
    **[] $VALUES;
    public *;
}