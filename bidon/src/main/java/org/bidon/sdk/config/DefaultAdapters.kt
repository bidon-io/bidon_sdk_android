package org.bidon.sdk.config

/**
 * Created by Bidon Team on 06/02/2023.
 *
 * Define adapters classes. Don't forget to add rules to proguard-files.
 */
@Suppress("unused")
enum class DefaultAdapters(val classPath: String) {
    AdmobAdapter(classPath = "org.bidon.admob.AdmobAdapter"),
    AmazonAdapter(classPath = "org.bidon.amazon.AmazonAdapter"),
    ApplovinAdapter(classPath = "org.bidon.applovin.ApplovinAdapter"),
    BidmachineAdapter(classPath = "org.bidon.bidmachine.BidMachineAdapter"),
    BigoAdsAdapter(classPath = "org.bidon.bigoads.BigoAdsAdapter"),
    DTExchangeAdapter(classPath = "org.bidon.dtexchange.DTExchangeAdapter"),
    GoogleAdManagerAdapter(classPath = "org.bidon.gam.GamAdapter"),
    InmobiAdapter(classPath = "org.bidon.inmobi.InmobiAdapter"),
    MetaAdapter(classPath = "org.bidon.meta.MetaAudienceAdapter"),
    MintegralAdapter(classPath = "org.bidon.mintegral.MintegralAdapter"),
    MobilefuseAdapter(classPath = "org.bidon.mobilefuse.MobileFuseAdapter"),
    UnityAdsAdapter(classPath = "org.bidon.unityads.UnityAdsAdapter"),
    VkAdsAdapter(classPath = "org.bidon.vkads.VkAdsAdapter"),
    VungleAdapter(classPath = "org.bidon.vungle.VungleAdapter"),
    YandexAdapter(classPath = "org.bidon.yandex.YandexAdapter"),
}
