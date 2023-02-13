package com.appodealstack.bidon.databinders.geo

import com.appodealstack.bidon.config.models.Geo
import com.appodealstack.bidon.databinders.DataBinder
import com.appodealstack.bidon.databinders.location.LocationDataSource
import com.appodealstack.bidon.utils.json.JsonParsers
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class GeoBinder(
    private val dataSource: LocationDataSource
) : DataBinder<JSONObject> {
    override val fieldName: String = "geo"

    override suspend fun getJsonObject(): JSONObject = JsonParsers.serialize(createGeo())

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
