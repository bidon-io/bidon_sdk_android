package com.appodealstack.bidon.databinders.device

import com.appodealstack.bidon.config.models.Device
import com.appodealstack.bidon.databinders.DataBinder
import com.appodealstack.bidon.utils.json.JsonParsers
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class DeviceBinder(
    private val dataSource: DeviceDataSource,
) : DataBinder<JSONObject> {
    override val fieldName: String = "device"

    override suspend fun getJsonObject(): JSONObject = JsonParsers.serialize(createDevice())

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
            connectionType = dataSource.getConnectionTypeCode()
        )
    }
}