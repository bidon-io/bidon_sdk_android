package com.appodealstack.bidon.data.binderdatasources.device

import com.appodealstack.bidon.data.binderdatasources.DataSource

internal interface DeviceDataSource : DataSource {
    fun getUserAgent(): String?
    fun getManufacturer(): String
    fun getDeviceModel(): String
    fun getOs(): String
    fun getOsVersion(): String
    fun getHardwareVersion(): String
    fun getScreenWidth(): Int
    fun getScreenHeight(): Int
    fun getPpi(): Int
    fun getPxRatio(): Float
    fun getJavaScriptSupport(): Int
    fun getLanguage(): String
    fun getCarrier(): String?
    fun getPhoneMCCMNC(): String?
    fun getConnectionTypeCode(): String
}