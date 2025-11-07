package org.bidon.sdk.segment

import org.bidon.sdk.segment.models.Gender

/**
 * Created by Aleksei Cherniaev on 14/06/2023.
 */
public interface Segment {
    /**
     * Snowflake ID
     */
    public val segmentUid: String?
    public var age: Int?
    public var gender: Gender?

    /**
     * How many levels user has passed (for games mostly)
     */
    public var level: Int?

    /**
     * Total amount of in-app purchases made by user
     */
    public var totalInAppAmount: Double?

    /**
     * Indicates whether or not user made at least one in-app purchase
     */
    public var isPaying: Boolean

    /**
     * Supported value types are bool, int, long, double, string, Json-object.
     * This method replaces all current values.
     */
    public fun setCustomAttributes(attributes: Map<String, Any>)

    /**
     * Supported value types are bool, int, long, double, string, Json-object.
     * This method add new or update existing attribute without replacing others.
     * If value is null, then the existing attribute will be removed.
     */
    public fun putCustomAttribute(attribute: String, value: Any?)

    /**
     * Retrieves all custom attributes.
     */
    public fun getCustomAttributes(): Map<String, Any>
}
