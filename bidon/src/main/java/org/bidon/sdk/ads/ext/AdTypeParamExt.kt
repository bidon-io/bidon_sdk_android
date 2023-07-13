package org.bidon.sdk.ads.ext

import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.models.BannerRequestBody
import org.bidon.sdk.auction.models.InterstitialRequestBody
import org.bidon.sdk.auction.models.RewardedRequestBody

/**
 * Created by Aleksei Cherniaev on 31/05/2023.
 */

internal fun AdTypeParam.asBannerRequestBody() = when (this) {
    is AdTypeParam.Banner -> {
        BannerRequestBody(
            formatCode = when (this.bannerFormat) {
                BannerFormat.Banner -> BannerRequestBody.StatFormat.Banner320x50
                BannerFormat.LeaderBoard -> BannerRequestBody.StatFormat.LeaderBoard728x90
                BannerFormat.MRec -> BannerRequestBody.StatFormat.MRec300x250
                BannerFormat.Adaptive -> BannerRequestBody.StatFormat.AdaptiveBanner320x50
            }.code,
        )
    }

    is AdTypeParam.Interstitial -> null
    is AdTypeParam.Rewarded -> null
}

internal fun AdTypeParam.asAdRequestBody(): Triple<BannerRequestBody?, InterstitialRequestBody?, RewardedRequestBody?> {
    return when (this) {
        is AdTypeParam.Banner -> {
            Triple(
                first = BannerRequestBody(
                    formatCode = when (this.bannerFormat) {
                        BannerFormat.Banner -> BannerRequestBody.StatFormat.Banner320x50
                        BannerFormat.LeaderBoard -> BannerRequestBody.StatFormat.LeaderBoard728x90
                        BannerFormat.MRec -> BannerRequestBody.StatFormat.MRec300x250
                        BannerFormat.Adaptive -> BannerRequestBody.StatFormat.AdaptiveBanner320x50
                    }.code,
                ),
                second = null,
                third = null
            )
        }

        is AdTypeParam.Interstitial -> {
            Triple(
                first = null,
                second = InterstitialRequestBody(),
                third = null
            )
        }

        is AdTypeParam.Rewarded -> {
            Triple(
                first = null,
                second = null,
                third = RewardedRequestBody()
            )
        }
    }
}