package com.appodealstack.bidon.domain.databinders

import com.appodealstack.bidon.data.binderdatasources.segment.SegmentDataSource
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class SegmentBinder(
    private val dataSource: SegmentDataSource
) : DataBinder<JSONObject>  {
    override val fieldName: String = "segment_id"

    override suspend fun getJsonObject(): JSONObject = JSONObject(dataSource.getSegmentId() ?: "")
}
