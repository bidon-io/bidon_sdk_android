package org.bidon.sdk.databinders.geo

import org.bidon.sdk.config.models.Geo
import org.bidon.sdk.databinders.DataBinder
import org.bidon.sdk.databinders.location.LocationDataSource
import org.bidon.sdk.utils.serializer.serialize
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class GeoBinder(
    private val dataSource: LocationDataSource
) : DataBinder<JSONObject> {
    override val fieldName: String = "geo"

    override suspend fun getJsonObject(): JSONObject? = createGeo()?.serialize()

    private fun createGeo(): Geo? {
        return if (dataSource.isLocationAvailable) {
            Geo(
                lat = dataSource.getLatitude(),
                lon = dataSource.getLongitude(),
                accuracy = dataSource.getAccuracy(),
                lastfix = dataSource.getLastFix(),
                country = dataSource.getCountry(),
                city = dataSource.getCity(),
                zip = dataSource.getZip(),
                utcOffset = dataSource.getUtcOffset()
            )
        } else {
            null
        }
    }
}
