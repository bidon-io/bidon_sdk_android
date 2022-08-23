package com.appodealstack.bidon.utilities.datasource.user

import android.content.Context
import android.os.Build
import android.provider.Settings.Secure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.util.*

object AdvertisingInfo {

    internal const val defaultAdvertisingId: String = "00000000-0000-0000-0000-000000000000"

    val adProfileFlow = MutableStateFlow<AdvertisingProfile>(DefaultAdvertisingProfile)

    private val supportedAdvertisingProfiles = listOf(
        GoogleAdvertisingProfile(),
        AmazonAdvertisingProfile(),
        HuaweiAdvertisingProfile(),
        DefaultAdvertisingProfile
    )

    suspend fun getAdvertisingProfile(
        context: Context
    ): AdvertisingProfile = withContext(Dispatchers.IO) {
        for (profile in supportedAdvertisingProfiles) {
            try {
                if (profile.isEnabled(context)) {
                    profile.extractParams(context)
                    adProfileFlow.emit(profile)
                    return@withContext profile
                }
            } catch (throwable: Throwable) {
            }
        }
        val defaultAdProfile = DefaultAdvertisingProfile.apply { extractParams(context) }
        adProfileFlow.emit(defaultAdProfile)
        return@withContext defaultAdProfile
    }

    abstract class AdvertisingProfile {
        var id: String = defaultAdvertisingId
            protected set
        var isLimitAdTrackingEnabled = false
            protected set
        var isAdvertisingIdWasGenerated = false
            protected set

        @Throws(Throwable::class)
        internal open fun isEnabled(context: Context): Boolean = true

        @Throws(Throwable::class)
        internal open fun extractParams(context: Context) {
            if (isLimitAdTrackingEnabled || id == defaultAdvertisingId || id.isBlank()) {
                id = getUUID(context)
                isAdvertisingIdWasGenerated = true
            }
        }

        internal open fun getUUID(context: Context): String {
            val sharedPref = context.getSharedPreferences("appodeal", Context.MODE_PRIVATE)
            var uuid: String? = null
            if (sharedPref.contains("uuid")) {
                uuid = sharedPref.getString("uuid", defaultAdvertisingId)
            }
            if (uuid.isNullOrEmpty()) {
                uuid = UUID.randomUUID().toString()
                val editor = sharedPref.edit()
                editor.putString("uuid", uuid)
                editor.apply()
            }
            return uuid
        }

        override fun toString(): String {
            return "${this.javaClass.simpleName}(id='$id', isLimitAdTrackingEnabled=$isLimitAdTrackingEnabled, isAdvertisingIdWasGenerated=$isAdvertisingIdWasGenerated)"
        }
    }

    object DefaultAdvertisingProfile : AdvertisingProfile()

    private class GoogleAdvertisingProfile : AdvertisingProfile() {

        @Throws(Throwable::class)
        override fun isEnabled(context: Context): Boolean {
            Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient")
            return true
        }

        @Throws(Throwable::class)
        override fun extractParams(context: Context) {
            val info = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient")
                .getDeclaredMethod("getAdvertisingIdInfo", Context::class.java)
                .invoke(null, context)
            val infoClass =
                Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient\$Info")
            id = infoClass.getDeclaredMethod("getId").invoke(info) as String
            isLimitAdTrackingEnabled = infoClass
                .getDeclaredMethod("isLimitAdTrackingEnabled")
                .invoke(info) as Boolean
            super.extractParams(context)
        }
    }

    private class HuaweiAdvertisingProfile : AdvertisingProfile() {

        @Throws(Throwable::class)
        override fun isEnabled(context: Context): Boolean {
            Class.forName("com.huawei.hms.ads.identifier.AdvertisingIdClient")
            return true
        }

        @Throws(Throwable::class)
        override fun extractParams(context: Context) {
            val info = Class.forName("com.huawei.hms.ads.identifier.AdvertisingIdClient")
                .getDeclaredMethod("getAdvertisingIdInfo", Context::class.java)
                .invoke(null, context)
            val infoClass = Class.forName("com.huawei.hms.ads.identifier.AdvertisingIdClient\$Info")
            id = infoClass.getDeclaredMethod("getId").invoke(info) as String
            isLimitAdTrackingEnabled = infoClass
                .getDeclaredMethod("isLimitAdTrackingEnabled")
                .invoke(info) as Boolean
            super.extractParams(context)
        }
    }

    private class AmazonAdvertisingProfile : AdvertisingProfile() {

        @Throws(Throwable::class)
        override fun isEnabled(context: Context): Boolean = "Amazon" == Build.MANUFACTURER

        @Throws(Throwable::class)
        override fun extractParams(context: Context) {
            val contentResolver = context.contentResolver
            id = Secure.getString(contentResolver, "advertising_id")
            isLimitAdTrackingEnabled = Secure.getInt(contentResolver, "limit_ad_tracking") != 0
            super.extractParams(context)
        }
    }
}