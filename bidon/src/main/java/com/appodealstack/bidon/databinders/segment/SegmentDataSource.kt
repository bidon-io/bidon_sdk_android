package com.appodealstack.bidon.databinders.segment

import com.appodealstack.bidon.databinders.DataSource

internal interface SegmentDataSource : DataSource {
    fun saveSegmentId(segmentId: String?)
    fun getSegmentId(): String?
}