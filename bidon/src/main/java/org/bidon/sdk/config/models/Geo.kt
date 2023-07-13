package org.bidon.sdk.config.models

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Bidon Team on 06/02/2023.
 */
data class Geo(
    @field:JsonName("lat")
    val lat: Double?,
    @field:JsonName("lon")
    val lon: Double?,
    @field:JsonName("accuracy")
    val accuracy: Int?,
    @field:JsonName("lastfix")
    val lastFix: Long?,
    @field:JsonName("country")
    val country: String?,
    @field:JsonName("city")
    val city: String?,
    @field:JsonName("zip")
    val zip: String?,
    @field:JsonName("utcoffset")
    val utcOffset: Int
) : Serializable
