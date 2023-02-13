package com.appodealstack.bidon.databinders.placement

import com.appodealstack.bidon.databinders.DataSource

internal interface PlacementDataSource : DataSource {
    fun getName(): String
    fun getRewardAmount(): Int?
    fun getRewardType(): String?
    fun getCappingSetting(): String
    fun getCappingValue(): Int
}