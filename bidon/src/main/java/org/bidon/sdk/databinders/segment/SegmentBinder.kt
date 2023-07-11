package org.bidon.sdk.databinders.segment

import org.bidon.sdk.databinders.DataBinder

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal class SegmentBinder(
    private val dataSource: SegmentDataSource
) : DataBinder<String> {
    override val fieldName: String = "segment_id"

    override suspend fun getJsonObject(): String? = dataSource.getSegmentId()
}
