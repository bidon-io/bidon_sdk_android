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

-keeppackagenames com.appodealstack.ironsource.**
-keep class com.appodealstack.ironsource.**
-keep interface com.appodealstack.ironsource.ISDecorator {*;}
-keep interface com.appodealstack.ironsource.ISDecorator$Banner {*;}
-keep interface com.appodealstack.ironsource.ISDecorator$Banner$BannerView {*;}
-keep interface com.appodealstack.ironsource.ISDecorator$Impressions {*;}
-keep interface com.appodealstack.ironsource.ISDecorator$Initializer {*;}
-keep interface com.appodealstack.ironsource.ISDecorator$Interstitial {*;}
-keep interface com.appodealstack.ironsource.ISDecorator$Rewarded {*;}
-keep class com.appodealstack.ironsource.IronSourceDecorator {*;}
-keep class com.appodealstack.ironsource.IronSourceAdapter {*;}
-keep class com.appodealstack.ironsource.IronSourceParameters {*;}

-keep class com.appodealstack.ironsource.rewarded.IronSourceRewardedListener {*;}
-keep class com.appodealstack.ironsource.rewarded.IronSourceLevelPlayRewardedListener {*;}
-keep class com.appodealstack.ironsource.interstitial.IronSourceInterstitialListener {*;}
-keep class com.appodealstack.ironsource.interstitial.IronSourceLevelPlayInterstitialListener {*;}
-keep class com.appodealstack.ironsource.banner.IronSourceBannerListener {*;}
-keep class com.appodealstack.ironsource.banner.IronSourceLevelPlayBannerListener {*;}