package org.bidon.sdk.databinders.placement

import org.bidon.sdk.databinders.DataSource

internal interface PlacementDataSource : DataSource {
    fun getName(): String
    fun getRewardAmount(): Int?
    fun getRewardType(): String?
    fun getCappingSetting(): String
    fun getCappingValue(): Int
}