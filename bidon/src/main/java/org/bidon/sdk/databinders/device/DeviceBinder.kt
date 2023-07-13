package org.bidon.sdk.databinders.device

import org.bidon.sdk.config.models.Device
import org.bidon.sdk.config.models.Geo
import org.bidon.sdk.databinders.DataBinder
import org.bidon.sdk.databinders.location.LocationDataSource
import org.bidon.sdk.utils.serializer.serialize
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class DeviceBinder(
    private val deviceDataSource: DeviceDataSource,
    private val locationDataSource: LocationDataSource
) : DataBinder<JSONObject> {
    override val fieldName: String = "device"

    override suspend fun getJsonObject(): JSONObject = createDevice().serialize()

    private fun createDevice(): Device {
        return Device(
            geo = if (locationDataSource.isLocationAvailable) {
                Geo(
                    lat = locationDataSource.getLatitude(),
                    lon = locationDataSource.getLongitude(),
                    accuracy = locationDataSource.getAccuracy(),
                    lastFix = locationDataSource.getLastFix(),
                    country = locationDataSource.getCountry(),
                    city = locationDataSource.getCity(),
                    zip = locationDataSource.getZip(),
                    utcOffset = locationDataSource.getUtcOffset()
                )
            } else {
                null
            },
            userAgent = deviceDataSource.getUserAgent(),
            manufacturer = deviceDataSource.getManufacturer(),
            deviceModel = deviceDataSource.getDeviceModel(),
            os = deviceDataSource.getOs(),
            osVersion = deviceDataSource.getOsVersion(),
            hardwareVersion = deviceDataSource.getHardwareVersion(),
            width = deviceDataSource.getScreenWidth(),
            height = deviceDataSource.getScreenHeight(),
            ppi = deviceDataSource.getPpi(),
            pxRatio = deviceDataSource.getPxRatio(),
            javaScriptSupport = deviceDataSource.getJavaScriptSupport(),
            language = deviceDataSource.getLanguage(),
            carrier = deviceDataSource.getCarrier(),
            mccmnc = deviceDataSource.getPhoneMCCMNC(),
            connectionType = deviceDataSource.getConnectionTypeCode(),
            type = DeviceType.getType(deviceDataSource.isTablet()).code,
            osApiLevel = deviceDataSource.getApiLevel()
        )
    }

    enum class DeviceType(val code: String) {
        Phone("PHONE"),
        Tablet("TABLET");

        companion object {
            fun getType(isTablet: Boolean) = if (isTablet) Tablet else Phone
        }
    }
}