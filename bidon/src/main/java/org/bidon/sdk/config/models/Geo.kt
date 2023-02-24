package org.bidon.sdk.config.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
data class Geo(
    @field:JsonName("lat")
    var lat: Double?,
    @field:JsonName("lon")
    var lon: Double?,
    @field:JsonName("accuracy")
    var accuracy: Float?,
    @field:JsonName("lastfix")
    var lastfix: Long?,
    @field:JsonName("country")
    var country: String?,
    @field:JsonName("city")
    var city: String?,
    @field:JsonName("zip")
    var zip: String?,
    @field:JsonName("utcoffset")
    var utcOffset: Int
) : Serializable
