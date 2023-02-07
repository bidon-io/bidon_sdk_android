package com.appodealstack.bidon.data.models.auction

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
@Serializable
data class BannerRequestBody(
    @SerialName("format")
    val formatCode: String,
) {
    enum class Format(val code: String) {
        Banner320x50("BANNER"),
        LeaderBoard728x90("LEADERBOARD"),
        MRec300x250("MREC"),
        AdaptiveBanner320x50("ADAPTIVE"),
    }
}