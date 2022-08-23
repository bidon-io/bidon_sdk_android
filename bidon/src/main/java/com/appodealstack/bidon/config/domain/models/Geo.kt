package com.appodealstack.bidon.config.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Geo(
    @SerialName("lat")
    var lat: Double?,
    @SerialName("lon")
    var lon: Double?,
    @SerialName("accuracy")
    var accuracy: Float?,
    @SerialName("lastfix")
    var lastfix: Long?,
    @SerialName("country")
    var country: String?,
    @SerialName("region")
    var region: String?,
    @SerialName("city")
    var city: String?,
    @SerialName("zip")
    var zip: String?,
    @SerialName("utcoffset")
    var utcOffset: Int
)