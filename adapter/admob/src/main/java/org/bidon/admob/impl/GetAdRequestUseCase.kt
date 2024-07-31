package org.bidon.admob.impl

import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import org.bidon.admob.AdmobBannerAuctionParams
import org.bidon.admob.AdmobFullscreenAdAuctionParams
import org.bidon.admob.AdmobInitParameters
import org.bidon.admob.ext.asBundle
import org.bidon.sdk.BidonSdk

/**
 * Created by Aleksei Cherniaev on 18/08/2023.
 */
internal class GetAdRequestUseCase(private val configParams: AdmobInitParameters?) {
    operator fun invoke(adParams: AdmobFullscreenAdAuctionParams): AdRequest? {
        return when (adParams) {
            is AdmobFullscreenAdAuctionParams.Bidding -> getBiddingAdRequest(adParams.payload)
            is AdmobFullscreenAdAuctionParams.Network -> getDspAdRequest()
        }
    }

    operator fun invoke(adParams: AdmobBannerAuctionParams): AdRequest? {
        return when (adParams) {
            is AdmobBannerAuctionParams.Bidding -> getBiddingAdRequest(adParams.payload)
            is AdmobBannerAuctionParams.Network -> getDspAdRequest()
        }
    }

    private fun getDspAdRequest() = AdRequest.Builder()
        .addNetworkExtrasBundle(AdMobAdapter::class.java, BidonSdk.regulation.asBundle())
        .build()

    private fun getBiddingAdRequest(payload: String?) =
        payload?.let {
            AdRequest.Builder()
                .apply {
                    setAdString(it)
                    configParams?.requestAgent?.let { agent ->
                        setRequestAgent(agent)
                    }
                    val networkExtras = BidonSdk.regulation.asBundle()
                    addNetworkExtrasBundle(AdMobAdapter::class.java, networkExtras)
                }
                .build()
        }
}