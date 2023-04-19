package org.bidon.sdk.auction.models

import org.bidon.sdk.ads.banner.BannerFormat
import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 *
 * @param [formatCode] is a [BannerRequestBody.StatFormat.code]
 */
data class BannerRequestBody(
    @field:JsonName("format")
    val formatCode: String,
) : Serializable {
    enum class StatFormat(val code: String) {
        Banner320x50("BANNER"),
        LeaderBoard728x90("LEADERBOARD"),
        MRec300x250("MREC"),
        AdaptiveBanner320x50("ADAPTIVE"),
    }

    companion object {
        fun BannerFormat.asStatBannerFormat() = when (this) {
            BannerFormat.Banner -> StatFormat.Banner320x50
            BannerFormat.LeaderBoard -> StatFormat.LeaderBoard728x90
            BannerFormat.MRec -> StatFormat.MRec300x250
            BannerFormat.Adaptive -> StatFormat.AdaptiveBanner320x50
        }
    }
}
