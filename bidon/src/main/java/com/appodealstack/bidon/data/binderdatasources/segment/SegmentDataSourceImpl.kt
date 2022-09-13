package com.appodealstack.bidon.data.binderdatasources.segment

internal class SegmentDataSourceImpl : SegmentDataSource {
    private var segmentId: String? = null

    override fun saveSegmentId(segmentId: String?) {
        this.segmentId = segmentId
    }

    override fun getSegmentId(): String? = segmentId
}