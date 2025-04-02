-keeppackagenames org.bidon.**

-keep class org.bidon.amazon.AmazonAdapter { *; }
-keep class com.amazon.device.ads.DTBActivityMonitor { *; }

-keep class com.amazon.** { *; }
-keep public class com.google.android.gms.ads.** {
    public *;
}
-keep class com.iabtcf.** {*;}