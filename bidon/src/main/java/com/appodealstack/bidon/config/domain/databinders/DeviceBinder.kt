package com.appodealstack.bidon.config.domain.databinders

import com.appodealstack.bidon.config.domain.DataBinder
import com.appodealstack.bidon.config.domain.models.Device
import com.appodealstack.bidon.core.BidonJson
import com.appodealstack.bidon.utilities.datasource.device.DeviceDataSource
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement

internal class DeviceBinder(
    private val dataSource: DeviceDataSource,
) : DataBinder {
    override val fieldName: String = "device"

    override suspend fun getJsonElement(): JsonElement =
        BidonJson.encodeToJsonElement(createDevice())

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