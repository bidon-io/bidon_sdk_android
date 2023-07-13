package org.bidon.sdk.segment.models

/**
 * Created by Aleksei Cherniaev on 14/06/2023.
 */
internal data class SegmentAttributes(
    val age: Int?,
    val gender: Gender?,
    val customAttributes: Map<String, Any>,
    val inAppAmount: Double?,
    val isPaying: Boolean?,
    val gameLevel: Int?,
) {
    companion object {
        val Empty
            get() = SegmentAttributes(
                age = null,
                gender = null,
                customAttributes = emptyMap(),
                inAppAmount = null,
                isPaying = null,
                gameLevel = null,
            )
    }
}
