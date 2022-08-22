package com.appodealstack.bidon

/**
 * Define adapters classes. Don't forget to add rules to proguard-files.
 */
internal enum class AdapterList(val classPath: String) {
    AdmobAdapter(classPath = "com.appodealstack.admob.AdmobAdapter"),
    BidmachineAdapter(classPath = "com.appodealstack.bidmachine.BidMachineAdapter"),
    ApplovinAdapter(classPath = "com.appodealstack.applovin.ApplovinMaxAdapter"),
//    FyberFairBidAdapter(classPath = "com.appodealstack.fyber.FairBidAdapter"),
    AppsflyerAdapter(classPath = "com.appodealstack.appsflyer.AppsflyerAnalytics"),
//    IronSourceAdapter(classPath = "com.appodealstack.ironsource.IronSourceAdapter"),
}
