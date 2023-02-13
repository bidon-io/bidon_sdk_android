package com.appodealstack.bidon.databinders.user.impl

import com.appodealstack.bidon.databinders.user.AdvertisingData
import com.appodealstack.bidon.databinders.user.AdvertisingProfile
import com.appodealstack.bidon.databinders.user.TrackingAuthorizationStatus
import com.appodealstack.bidon.databinders.user.UserDataSource
import com.appodealstack.bidon.utils.keyvaluestorage.KeyValueStorage
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
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
