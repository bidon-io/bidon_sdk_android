-keeppackagenames org.bidon.**

-keep class org.bidon.vungle.VungleAdapter { *; }

# Vungle
-dontwarn com.vungle.warren.downloader.DownloadRequestMediator$Status
-dontwarn com.vungle.warren.error.VungleError$ErrorCode

# Google
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# GSON
-keepattributes *Annotation*
-keepattributes Signature
# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
@com.google.gson.annotations.SerializedName <fields>;
}

# OkHttp + Okio
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.ConscryptPlatform

-dontwarn module-info