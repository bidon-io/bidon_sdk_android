package com.appodealstack.bidon.data.binderdatasources.placement

import com.appodealstack.bidon.data.binderdatasources.DataSource

internal interface PlacementDataSource : DataSource {
    fun getName(): String
    fun getRewardAmount(): Int?
    fun getRewardType(): String?
    fun getCappingSetting(): String
    fun getCappingValue(): Int
}