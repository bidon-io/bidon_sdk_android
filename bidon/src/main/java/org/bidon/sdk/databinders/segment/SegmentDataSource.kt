package org.bidon.sdk.databinders.segment

import org.bidon.sdk.databinders.DataSource

internal interface SegmentDataSource : DataSource {
    fun saveSegmentId(segmentId: String?)
    fun getSegmentId(): String?
}