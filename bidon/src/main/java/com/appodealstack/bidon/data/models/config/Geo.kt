package com.appodealstack.bidon.data.models.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
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
    @SerialName("city")
    var city: String?,
    @SerialName("zip")
    var zip: String?,
    @SerialName("utcoffset")
    var utcOffset: Int
)