package org.bidon.sdk.segment

import org.bidon.sdk.segment.models.SegmentAttributes
import org.bidon.sdk.utils.keyvaluestorage.KeyValueStorage

/**
 * Created by Aleksei Cherniaev on 15/06/2023.
 */
internal interface SegmentSynchronizer {
    val attributes: SegmentAttributes
    val segmentId: String?

    /**
     * For parsing segmentId from responses /auction and /config-requests
     */
    fun parseSegmentId(rootJsonResponse: String)

    /**
     * For reading previous segmentId from [KeyValueStorage]
     */
    fun setSegmentId(segmentId: String?)
}