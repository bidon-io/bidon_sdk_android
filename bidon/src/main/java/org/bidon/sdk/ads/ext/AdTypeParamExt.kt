package org.bidon.sdk.ads.ext

import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.BannerRequest
import org.bidon.sdk.auction.models.InterstitialRequest
import org.bidon.sdk.auction.models.RewardedRequest

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */

internal fun AdTypeParam.asBannerRequestBody() = when (this) {
    is AdTypeParam.Banner -> {
        BannerRequest(
            formatCode = when (this.bannerFormat) {
                BannerFormat.Banner -> BannerRequest.StatFormat.Banner320x50
                BannerFormat.LeaderBoard -> BannerRequest.StatFormat.LeaderBoard728x90
                BannerFormat.MRec -> BannerRequest.StatFormat.MRec300x250
                BannerFormat.Adaptive -> BannerRequest.StatFormat.AdaptiveBanner320x50
            }.code,
        )
    }

    is AdTypeParam.Interstitial -> null
    is AdTypeParam.Rewarded -> null
}

internal fun AdTypeParam.asAdRequestBody(): Triple<BannerRequest?, InterstitialRequest?, RewardedRequest?> {
    return when (this) {
        is AdTypeParam.Banner -> {
            Triple(
                first = BannerRequest(
                    formatCode = when (this.bannerFormat) {
                        BannerFormat.Banner -> BannerRequest.StatFormat.Banner320x50
                        BannerFormat.LeaderBoard -> BannerRequest.StatFormat.LeaderBoard728x90
                        BannerFormat.MRec -> BannerRequest.StatFormat.MRec300x250
                        BannerFormat.Adaptive -> BannerRequest.StatFormat.AdaptiveBanner320x50
                    }.code,
                ),
                second = null,
                third = null
            )
        }

        is AdTypeParam.Interstitial -> {
            Triple(
                first = null,
                second = InterstitialRequest(),
                third = null
            )
        }

        is AdTypeParam.Rewarded -> {
            Triple(
                first = null,
                second = null,
                third = RewardedRequest()
            )
        }
    }
}