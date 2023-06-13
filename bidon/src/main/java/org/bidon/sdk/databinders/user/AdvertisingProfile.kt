package org.bidon.sdk.databinders.user
/**
 * Created by Bidon Team on 06/02/2023.
 */
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