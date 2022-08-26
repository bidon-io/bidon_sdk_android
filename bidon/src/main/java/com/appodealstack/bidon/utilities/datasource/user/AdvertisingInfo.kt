package com.appodealstack.bidon.utilities.datasource.user

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

internal interface AdvertisingInfo {

    val adProfileFlow: MutableStateFlow<AdvertisingInfoImpl.AdvertisingProfile>

    suspend fun getAdvertisingProfile(context: Context): AdvertisingInfoImpl.AdvertisingProfile
}