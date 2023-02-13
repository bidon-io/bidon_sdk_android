package com.appodealstack.bidon.databinders.user.impl

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.appodealstack.bidon.databinders.user.AdvertisingData
import com.appodealstack.bidon.databinders.user.AdvertisingProfile
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.*
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class AdvertisingDataImpl(
    private val context: Context
) : AdvertisingData {
    override val advertisingProfile: AdvertisingProfile
        get() = advertisingProfileFlow.value

    private val advertisingProfileFlow = MutableStateFlow(getInitialState())

    private fun getInitialState(): AdvertisingProfile {
        return getGoogleAdId() ?: getHuaweiAdId() ?: getAmazonAdId() ?: AdvertisingProfile.Denied
    }

    private fun getGoogleAdId(): AdvertisingProfile? {
        return try {
            AdvertisingIdClient.getAdvertisingIdInfo(context).let { info ->
                info.id?.let { adId ->
                    AdvertisingProfile.Google(
                        advertisingId = adId,
                        isLimitAdTrackingEnabled = info.isLimitAdTrackingEnabled
                    )
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getHuaweiAdId(): AdvertisingProfile? {
        return try {
            val info = Class.forName("com.huawei.hms.ads.identifier.AdvertisingIdClient")
                .getDeclaredMethod("getAdvertisingIdInfo", Context::class.java)
                .invoke(null, context)
            val infoClass = Class.forName("com.huawei.hms.ads.identifier.AdvertisingIdClient\$Info")
            val id = infoClass.getDeclaredMethod("getId").invoke(info) as String
            val isLimitAdTrackingEnabled = infoClass
                .getDeclaredMethod("isLimitAdTrackingEnabled")
                .invoke(info) as Boolean
            AdvertisingProfile.Huawei(
                advertisingId = id,
                isLimitAdTrackingEnabled = isLimitAdTrackingEnabled
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun getAmazonAdId(): AdvertisingProfile? {
        return try {
            require("amazon" == Build.MANUFACTURER.lowercase(Locale.getDefault()))
            val contentResolver = context.contentResolver
            val id = Settings.Secure.getString(contentResolver, "advertising_id")
            val isLimitAdTrackingEnabled = Settings.Secure.getInt(contentResolver, "limit_ad_tracking") != 0
            AdvertisingProfile.Amazon(
                advertisingId = id,
                isLimitAdTrackingEnabled = isLimitAdTrackingEnabled
            )
        } catch (e: Exception) {
            null
        }
    }
}