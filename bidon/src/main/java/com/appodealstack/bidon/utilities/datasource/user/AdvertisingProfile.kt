package com.appodealstack.bidon.utilities.datasource.user

internal sealed interface AdvertisingProfile {
    object Denied : AdvertisingProfile

    data class Google(
        val advertisingId: String,
        val isLimitAdTrackingEnabled: Boolean
    ) : AdvertisingProfile

    data class Huawei(
        val advertisingId: String,
        val isLimitAdTrackingEnabled: Boolean
    ) : AdvertisingProfile

    data class Amazon(
        val advertisingId: String,
        val isLimitAdTrackingEnabled: Boolean
    ) : AdvertisingProfile
}