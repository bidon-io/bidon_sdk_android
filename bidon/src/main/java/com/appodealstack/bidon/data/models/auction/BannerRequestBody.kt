package com.appodealstack.bidon.data.models.auction

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BannerRequestBody(
    @SerialName("format")
    val formatCode: Int,
) {
    enum class Format(val code: Int) {
        Banner320x50(0),
        LeaderBoard728x90(1),
        MRec300x250(2),
        AdaptiveBanner320x50(3),
    }
}