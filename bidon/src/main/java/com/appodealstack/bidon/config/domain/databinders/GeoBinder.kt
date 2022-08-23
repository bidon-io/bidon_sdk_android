package com.appodealstack.bidon.config.domain.databinders

import com.appodealstack.bidon.config.domain.DataBinder
import com.appodealstack.bidon.config.domain.models.Geo
import com.appodealstack.bidon.core.BidonJson
import com.appodealstack.bidon.utilities.datasource.location.LocationDataSource
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

internal class GeoBinder(
    private val dataSource: LocationDataSource
) : DataBinder {
    override val fieldName: String = "geo"

    override suspend fun getJsonElement(): JsonElement = BidonJson.encodeToJsonElement(createGeo())

    private fun createGeo(): Geo {
        return Geo(
            lat = dataSource.getLat(),
            lon = dataSource.getLon(),
            accuracy = dataSource.getAccuracy(),
            lastfix = dataSource.getLastFix(),
            country = dataSource.getCountry(),
            region = dataSource.getRegion(),
            city = dataSource.getCity(),
            zip = dataSource.getZip(),
            utcOffset = dataSource.getUtcOffset()
        )
    }
}