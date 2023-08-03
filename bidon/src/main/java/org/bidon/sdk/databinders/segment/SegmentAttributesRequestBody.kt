package org.bidon.sdk.databinders.segment

import org.bidon.sdk.utils.serializer.JsonName
import org.bidon.sdk.utils.serializer.Serializable

/**
 * Created by Aleksei Cherniaev on 14/06/2023.
 */
internal data class SegmentAttributesRequestBody(
    @field:JsonName("age")
    val age: Int?,
    @field:JsonName("gender")
    val gender: String?,
    @field:JsonName("custom_attributes")
    val customAttributes: Map<String, Any>,
    @field:JsonName("total_in_apps_amount")
    val inAppAmount: Double?,
    @field:JsonName("is_paying")
    val isPaying: Boolean?,
    @field:JsonName("game_level")
    val gameLevel: Int?,
) : Serializable