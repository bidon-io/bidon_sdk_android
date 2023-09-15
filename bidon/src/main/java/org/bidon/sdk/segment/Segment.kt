package org.bidon.sdk.segment

import androidx.annotation.IntRange
import org.bidon.sdk.segment.models.Gender

/**
 * Created by Aleksei Cherniaev on 14/06/2023.
 */
interface Segment {

    /**
     * Current user's Segment ID
     */
    @Deprecated("Use segmentUid instead")
    val segmentId: String?

    /**
     * Snowflake ID
     */
    val segmentUid: String?

    fun setAge(@IntRange(from = 0, to = 150) age: Int?)
    fun setGender(gender: Gender?)

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

    /**
     * How many levels user has passed (for games mostly)
     */
    fun setLevel(level: Int)

    /**
     * Total amount of in-app purchases made by user
     */
    fun setTotalInAppAmount(inAppAmount: Double)

    /**
     * Indicates whether or not user made at least one in-app purchase
     */
    fun setPaying(isPaying: Boolean)
}
