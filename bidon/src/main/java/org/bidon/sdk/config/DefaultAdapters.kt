package org.bidon.sdk.config

/**
 * Created by Bidon Team on 06/02/2023.
 *
 * Define adapters classes. Don't forget to add rules to proguard-files.
 */
@Suppress("unused")
enum class DefaultAdapters(val classPath: String) {
    AdmobAdapter(classPath = "org.bidon.admob.AdmobAdapter"),
    BidmachineAdapter(classPath = "org.bidon.bidmachine.BidMachineAdapter"),
    ApplovinAdapter(classPath = "org.bidon.applovin.ApplovinAdapter"),
    DTExchangeAdapter(classPath = "org.bidon.dtexchange.DTExchangeAdapter"),
    UnityAdsAdapter(classPath = "org.bidon.unityads.UnityAdsAdapter"),
    BigoAdsAdapter(classPath = "org.bidon.bigoads.BigoAdsAdapter"),
    MintegralAdapter(classPath = "org.bidon.mintegral.MintegralAdapter"),
    VungleAdapter(classPath = "org.bidon.vungle.VungleAdapter"),
    MetaAdapter(classPath = "org.bidon.meta.MetaAudienceAdapter"),
}
