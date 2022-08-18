package com.appodealstack.bidon

import android.app.Activity
import com.appodealstack.bidon.adapters.Ad
import com.appodealstack.bidon.core.InitializationCallback
import com.appodealstack.bidon.di.DI.initDependencyInjection
import com.appodealstack.bidon.di.get

val BidON: BidOnSdk by lazy {
    initDependencyInjection()
    get()
}

interface BidOnSdk {
    companion object {
        const val DefaultPlacement = "default"
    }

    fun init(
        activity: Activity,
        appKey: String,
        callback: InitializationCallback? = null
    )

    fun isInitialized(): Boolean

    fun setBaseUrl(host: String?): BidOnSdk
    fun logRevenue(ad: Ad)

//    fun updateGDPRUserConsent(consent: GDPRUserConsent)
//    fun updateCCPAUserConsent(consent: CCPAUserConsent)
//    fun isAutoCacheEnabled(adType: Int): Boolean
//    fun setRequestCallbacks(callbacks: AppodealRequestCallbacks?)
//    fun setInterstitialCallbacks(callbacks: InterstitialCallbacks?)
//    fun setRewardedVideoCallbacks(callbacks: RewardedVideoCallbacks?)
//    fun setBannerCallbacks(callbacks: BannerCallbacks?)
//    fun setMrecCallbacks(callbacks: MrecCallbacks?)
//    fun setNativeCallbacks(callbacks: NativeCallbacks?)
//    fun setNativeAdType(adType: Native.NativeAdType)
//    fun getNativeAdType(): Native.NativeAdType?
//    fun cache(activity: Activity, adTypes: Int, count: Int = 1)
//    fun show(activity: Activity, adTypes: Int, placementName: String)
//    fun hide(activity: Activity, adTypes: Int)
//    fun setAutoCache(adTypes: Int, autoCache: Boolean)
//    fun isSharedAdsInstanceAcrossActivities(): Boolean
//    fun setSharedAdsInstanceAcrossActivities(sharedAdsInstanceAcrossActivities: Boolean)
//    fun isLoaded(adTypes: Int): Boolean
//    fun isPrecache(adType: Int): Boolean
//    fun setBannerViewId(bannerViewId: Int)
//    fun getBannerView(context: Context): BannerView?
//    fun setSmartBanners(enabled: Boolean)
//    fun isSmartBannersEnabled(): Boolean
//    fun set728x90Banners(enabled: Boolean)
//    fun setBannerAnimation(animate: Boolean)
//    fun setBannerRotation(leftBannerRotation: Int, rightBannerRotation: Int)
//    fun setUseSafeArea(useSafeArea: Boolean)
//    fun setMrecViewId(mrecViewId: Int)
//    fun getMrecView(context: Context): MrecView?
//    fun setRequiredNativeMediaAssetType(requiredMediaAssetType: Native.MediaAssetType?)
//    fun trackInAppPurchase(context: Context, amount: Double, currency: String)
//    fun getNetworks(context: Context, adTypes: Int): ArrayList<String>
//    fun disableNetwork(network: String)
//    fun disableNetwork(network: String, adTypes: Int)
//    fun setUserId(userId: String)
//    fun getUserId(): String?
//    fun getVersion(): String
//    fun getFrameworkName(): String?
//    fun getPluginVersion(): String?
//    fun getEngineVersion(): String?
//    fun getSegmentId(): Long
//    fun getBuildDate(): Date
//    fun setTesting(testMode: Boolean)
//    fun getLogLevel(): Log.LogLevel
//    fun setLogLevel(logLevel: Log.LogLevel)
//    fun setCustomFilter(name: String, value: Boolean)
//    fun setCustomFilter(name: String, value: Int)
//    fun setCustomFilter(name: String, value: Double)
//    fun setCustomFilter(name: String, value: String)
//    fun setCustomFilter(name: String, value: Any?)
//    fun getNativeAds(count: Int): List<NativeAd>
//    fun getAvailableNativeAdsCount(): Int
//    fun canShow(adTypes: Int, placementName: String = "default"): Boolean
//    fun getRewardParameters(placementName: String = "default"): Pair<Double, String?>
//    fun setFramework(frameworkName: String?, pluginVersion: String?, engineVersion: String? = null)
//    fun muteVideosIfCallsMuted(muteVideosIfCallsMuted: Boolean)
//    fun disableWebViewCacheClear()
//    fun startTestActivity(activity: Activity)
//    fun setChildDirectedTreatment(value: Boolean?)
//    fun destroy(adTypes: Int)
//    fun setExtraData(key: String, value: String)
//    fun setExtraData(key: String, value: Int)
//    fun setExtraData(key: String, value: Double)
//    fun setExtraData(key: String, value: Boolean)
//    fun setExtraData(key: String, value: Any?)
//    fun getPredictedEcpm(adType: Int): Double
//    fun logEvent(eventName: String, params: Map<String, Any?>?)
    // fun validateInAppPurchase(context: Context, purchase: InAppPurchase, callback: InAppPurchaseValidateCallback?)
}