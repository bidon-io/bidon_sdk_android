package com.appodealstack.bidon.data.binderdatasources.segment

import com.appodealstack.bidon.data.binderdatasources.DataSource

internal interface SegmentDataSource : DataSource {
    fun saveSegmentId(segmentId: String?)
    fun getSegmentId(): String?
}