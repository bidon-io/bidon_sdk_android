package com.appodealstack.bidon.data.binderdatasources.user.impl

import com.appodealstack.bidon.data.binderdatasources.user.AdvertisingData
import com.appodealstack.bidon.data.binderdatasources.user.AdvertisingProfile
import com.appodealstack.bidon.data.binderdatasources.user.TrackingAuthorizationStatus
import com.appodealstack.bidon.data.binderdatasources.user.UserDataSource
import com.appodealstack.bidon.data.keyvaluestorage.KeyValueStorage

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
