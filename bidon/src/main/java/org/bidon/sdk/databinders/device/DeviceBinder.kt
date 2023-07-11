package org.bidon.sdk.databinders.device

import org.bidon.sdk.config.models.Device
import org.bidon.sdk.databinders.DataBinder
import org.bidon.sdk.utils.serializer.serialize
import org.json.JSONObject

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal class DeviceBinder(
    private val dataSource: DeviceDataSource,
) : DataBinder<JSONObject> {
    override val fieldName: String = "device"

    override suspend fun getJsonObject(): JSONObject = createDevice().serialize()

    private fun createDevice(): Device {
        return Device(
            userAgent = dataSource.getUserAgent(),
            manufacturer = dataSource.getManufacturer(),
            deviceModel = dataSource.getDeviceModel(),
            os = dataSource.getOs(),
            osVersion = dataSource.getOsVersion(),
            hardwareVersion = dataSource.getHardwareVersion(),
            width = dataSource.getScreenWidth(),
            height = dataSource.getScreenHeight(),
            ppi = dataSource.getPpi(),
            pxRatio = dataSource.getPxRatio(),
            javaScriptSupport = dataSource.getJavaScriptSupport(),
            language = dataSource.getLanguage(),
            carrier = dataSource.getCarrier(),
            mccmnc = dataSource.getPhoneMCCMNC(),
            connectionType = dataSource.getConnectionTypeCode(),
            type = DeviceType.getType(dataSource.isTablet()).code,
            osApiLevel = dataSource.getApiLevel()
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