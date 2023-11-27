package org.bidon.sdk.segment

import org.bidon.sdk.segment.models.SegmentAttributes
import org.bidon.sdk.utils.keyvaluestorage.KeyValueStorage

/**
 * Created by Aleksei Cherniaev on 15/06/2023.
 */
internal interface SegmentSynchronizer {
    val attributes: SegmentAttributes
    val segmentUid: String?

    /**
     * For parsing segmentUid from responses /auction and /config-requests
     */
    fun parseSegmentUid(rootJsonResponse: String)

    /**
     * For reading previous segmentUid from [KeyValueStorage]
     */
    fun setSegmentUid(segmentUid: String?)
}