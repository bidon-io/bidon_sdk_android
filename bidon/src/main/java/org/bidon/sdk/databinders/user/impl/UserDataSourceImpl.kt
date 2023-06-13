package org.bidon.sdk.databinders.user.impl

import org.bidon.sdk.databinders.user.AdvertisingData
import org.bidon.sdk.databinders.user.AdvertisingProfile
import org.bidon.sdk.databinders.user.TrackingAuthorizationStatus
import org.bidon.sdk.databinders.user.UserDataSource
import org.bidon.sdk.utils.keyvaluestorage.KeyValueStorage
/**
 * Created by Bidon Team on 06/02/2023.
 */
internal class UserDataSourceImpl(
    private val keyValueStorage: KeyValueStorage,
    private val advertisingData: AdvertisingData,
) : UserDataSource {

    override fun getTrackingAuthorizationStatus(): String {
        val limited = when (val data = advertisingData.advertisingProfile) {
            is AdvertisingProfile.Amazon -> data.isLimitAdTrackingEnabled
            is AdvertisingProfile.Google -> data.isLimitAdTrackingEnabled
            is AdvertisingProfile.Huawei -> data.isLimitAdTrackingEnabled
            AdvertisingProfile.Denied -> return TrackingAuthorizationStatus.Denied.code
        }
        return if (limited) {
            TrackingAuthorizationStatus.Restricted.code
        } else {
            TrackingAuthorizationStatus.Authorized.code
        }
    }

    override fun getApplicationId(): String = keyValueStorage.applicationId

    override fun getAdvertisingId(): String {
        return when (val data = advertisingData.advertisingProfile) {
            is AdvertisingProfile.Amazon -> data.advertisingId
            is AdvertisingProfile.Google -> data.advertisingId
            is AdvertisingProfile.Huawei -> data.advertisingId
            AdvertisingProfile.Denied -> defaultAdvertisingId
        }
    }
}

private const val defaultAdvertisingId: String = "00000000-0000-0000-0000-000000000000"
