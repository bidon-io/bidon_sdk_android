package org.bidon.sdk.databinders.segment

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Aleksei Cherniaev on 14/06/2023.
 */
internal data class SegmentRequestBody(
    @Deprecated("Use uid instead")
    @field:JsonName("id")
    val id: String?,
    @field:JsonName("uid")
    val uid: ULong?,
    /**
     * JSON Encoded String of [SegmentAttributesRequestBody]
     */
    @field:JsonName("ext")
    val ext: String?,
) : Serializable
