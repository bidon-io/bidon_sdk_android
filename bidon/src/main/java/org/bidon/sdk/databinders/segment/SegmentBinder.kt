package org.bidon.sdk.databinders.segment

import org.bidon.sdk.databinders.DataBinder
import org.bidon.sdk.segment.SegmentSynchronizer
import org.bidon.sdk.utils.serializer.serialize
import org.json.JSONObject

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal class SegmentBinder(
    private val segmentSynchronizer: SegmentSynchronizer
) : DataBinder<JSONObject> {
    override val fieldName: String = "segment"

    override suspend fun getJsonObject(): JSONObject? {
        val segmentId = segmentSynchronizer.segmentId
        val attr = segmentSynchronizer.attributes
        val ext = if (attr.age != null || attr.gender != null ||
            attr.customAttributes.isNotEmpty() ||
            attr.inAppAmount != null || attr.isPaying != null ||
            attr.gameLevel != null
        ) {
            SegmentAttributesRequestBody(
                age = attr.age,
                gender = attr.gender?.code,
                customAttributes = attr.customAttributes,
                inAppAmount = attr.inAppAmount,
                isPaying = attr.isPaying,
                gameLevel = attr.gameLevel,
            )
        } else {
            null
        }
        if (segmentId == null && ext == null) {
            return null
        }
        return SegmentRequestBody(
            id = segmentId,
            ext = ext?.serialize()?.toString()
        ).serialize()
    }
}
