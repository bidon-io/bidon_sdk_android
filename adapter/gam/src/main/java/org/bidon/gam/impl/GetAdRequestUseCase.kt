package org.bidon.gam.impl

import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import org.bidon.gam.GamBannerAuctionParams
import org.bidon.gam.GamFullscreenAdAuctionParams
import org.bidon.gam.GamInitParameters
import org.bidon.gam.ext.asBundle
import org.bidon.sdk.BidonSdk

internal class GetAdRequestUseCase(private val configParams: GamInitParameters?) {
    operator fun invoke(adParams: GamFullscreenAdAuctionParams): AdManagerAdRequest? {
        return when (adParams) {
            is GamFullscreenAdAuctionParams.Bidding -> getBiddingAdRequest(adParams.payload)
            is GamFullscreenAdAuctionParams.Network -> getDspAdRequest()
        }
    }

    operator fun invoke(adParams: GamBannerAuctionParams): AdManagerAdRequest? {
        return when (adParams) {
            is GamBannerAuctionParams.Bidding -> getBiddingAdRequest(adParams.payload)
            is GamBannerAuctionParams.Network -> getDspAdRequest()
        }
    }

    private fun getDspAdRequest(): AdManagerAdRequest = AdManagerAdRequest.Builder()
        .build()

    private fun getBiddingAdRequest(payload: String?) =
        payload?.let {
            AdManagerAdRequest.Builder()
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