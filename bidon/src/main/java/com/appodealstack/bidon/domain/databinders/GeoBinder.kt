package com.appodealstack.bidon.domain.databinders

import com.appodealstack.bidon.data.binderdatasources.location.LocationDataSource
import com.appodealstack.bidon.data.json.BidonJson
import com.appodealstack.bidon.data.models.config.Geo
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class GeoBinder(
    private val dataSource: LocationDataSource
) : DataBinder {
    override val fieldName: String = "geo"

    override suspend fun getJsonElement(): JsonElement = BidonJson.encodeToJsonElement(createGeo())

    private fun createGeo(): Geo {
        return Geo(
            lat = dataSource.getLatitude(),
            lon = dataSource.getLongitude(),
            accuracy = dataSource.getAccuracy(),
            lastfix = dataSource.getLastFix(),
            country = dataSource.getCountry(),
            city = dataSource.getCity(),
            zip = dataSource.getZip(),
            utcOffset = dataSource.getUtcOffset()
        )
    }
}
