package com.appodealstack.bidon.domain.databinders

import com.appodealstack.bidon.data.binderdatasources.device.DeviceDataSource
import com.appodealstack.bidon.data.json.JsonParsers
import com.appodealstack.bidon.data.models.config.Device
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