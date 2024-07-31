package org.bidon.sdk.segment

import org.bidon.sdk.segment.models.Gender

/**
 * Created by Aleksei Cherniaev on 14/06/2023.
 */
interface Segment {
    /**
     * Snowflake ID
     */
    val segmentUid: String?
    var age: Int?
    var gender: Gender?

    /**
     * How many levels user has passed (for games mostly)
     */
    var level: Int?

    /**
     * Total amount of in-app purchases made by user
     */
    var totalInAppAmount: Double?

    /**
     * Indicates whether or not user made at least one in-app purchase
     */
    var isPaying: Boolean

    /**
     * Supported value types are bool, int, long, double, string, Json-object.
     * This method replaces all current values.
     */
    fun setCustomAttributes(attributes: Map<String, Any>)

    /**
     * Supported value types are bool, int, long, double, string, Json-object.
     * This method add new or update existing attribute without replacing others.
     * If value is null, then the existing attribute will be removed.
     */
    fun putCustomAttribute(attribute: String, value: Any?)
}
