package com.appodealstack.bidon.utilities.datasource.placement

import com.appodealstack.bidon.utilities.datasource.DataSource

internal interface PlacementDataSource : DataSource {
    fun getName(): String
    fun getRewardAmount(): Int?
    fun getRewardCurrency(): String?
    fun getCappingSetting(): String
    fun getCappingValue(): Int
}