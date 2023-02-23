package org.bidon.sdk.config.models

import org.bidon.sdk.utils.json.JsonSerializer
import org.bidon.sdk.utils.json.jsonObject
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
data class Geo(
    var lat: Double?,
    var lon: Double?,
    var accuracy: Float?,
    var lastfix: Long?,
    var country: String?,
    var city: String?,
    var zip: String?,
    var utcOffset: Int
)

internal class GeoSerializer : JsonSerializer<Geo> {
    override fun serialize(data: Geo): JSONObject {
        return jsonObject {
            "lat" hasValue data.lat
            "lon" hasValue data.lon
            "accuracy" hasValue data.accuracy
            "lastfix" hasValue data.lastfix
            "country" hasValue data.country
            "city" hasValue data.city
            "zip" hasValue data.zip
            "utcoffset" hasValue data.utcOffset
        }
    }
}