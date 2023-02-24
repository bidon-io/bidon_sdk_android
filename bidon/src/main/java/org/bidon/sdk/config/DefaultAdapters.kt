package org.bidon.sdk.config

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 *
 * Define adapters classes. Don't forget to add rules to proguard-files.
 */
@Suppress("unused")
internal enum class DefaultAdapters(val classPath: String) {
    AdmobAdapter(classPath = "org.bidon.admob.AdmobAdapter"),
    BidmachineAdapter(classPath = "org.bidon.bidmachine.BidMachineAdapter"),
    ApplovinAdapter(classPath = "org.bidon.applovin.ApplovinAdapter"),

//    AppsflyerAdapter(classPath = "org.bidon.appsflyer.AppsflyerAnalytics"),
//    FyberFairBidAdapter(classPath = "org.bidon.fyber.FairBidAdapter"),
//    IronSourceAdapter(classPath = "org.bidon.ironsource.IronSourceAdapter"),
//    MaxAdapter(classPath = "org.bidon.applovin.MaxAdapter"),
}
