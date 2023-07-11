package org.bidon.sdk.databinders.device

import org.bidon.sdk.databinders.DataSource
/**
 * Created by Bidon Team on 06/02/2023.
 */
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
    fun isTablet(): Boolean
    fun getApiLevel(): String
}