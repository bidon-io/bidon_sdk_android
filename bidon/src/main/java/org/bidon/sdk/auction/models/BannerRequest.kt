package org.bidon.sdk.auction.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Bidon Team on 06/02/2023.
 *
 * @param [formatCode] is a [BannerRequest.StatFormat.code]
 */
data class BannerRequest(
    @field:JsonName("format")
    val formatCode: String,
) : Serializable {
    enum class StatFormat(val code: String) {
        Banner320x50("BANNER"),
        LeaderBoard728x90("LEADERBOARD"),
        MRec300x250("MREC"),
        AdaptiveBanner320x50("ADAPTIVE"),
    }
}
