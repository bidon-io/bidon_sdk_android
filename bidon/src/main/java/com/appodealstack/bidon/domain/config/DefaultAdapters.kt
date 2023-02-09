package com.appodealstack.bidon.domain.config

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 *
 * Define adapters classes. Don't forget to add rules to proguard-files.
 */
internal enum class DefaultAdapters(val classPath: String) {
    AdmobAdapter(classPath = "com.appodealstack.admob.AdmobAdapter"),
    BidmachineAdapter(classPath = "com.appodealstack.bidmachine.BidMachineAdapter"),

//    ApplovinAdapter(classPath = "com.appodealstack.applovin.ApplovinAdapter"),
//    AppsflyerAdapter(classPath = "com.appodealstack.appsflyer.AppsflyerAnalytics"),
//    FyberFairBidAdapter(classPath = "com.appodealstack.fyber.FairBidAdapter"),
//    IronSourceAdapter(classPath = "com.appodealstack.ironsource.IronSourceAdapter"),
//    MaxAdapter(classPath = "com.appodealstack.applovin.MaxAdapter"),
}
