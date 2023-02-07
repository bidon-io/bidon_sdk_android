package com.appodealstack.bidon.domain.databinders

import com.appodealstack.bidon.data.binderdatasources.segment.SegmentDataSource
import com.appodealstack.bidon.data.json.BidonJson
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.encodeToJsonElement
/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class SegmentBinder(
    private val dataSource: SegmentDataSource
) : DataBinder {
    override val fieldName: String = "segment_id"

    override suspend fun getJsonElement(): JsonElement = BidonJson.encodeToJsonElement(createSegment())

    private fun createSegment() =
        dataSource.getSegmentId()?.let {
            BidonJson.parseToJsonElement(it)
        }
}
