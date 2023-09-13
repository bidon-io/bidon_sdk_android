package org.bidon.sdk.segment.impl

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.segment.Segment
import org.bidon.sdk.segment.SegmentSynchronizer
import org.bidon.sdk.segment.models.Gender
import org.bidon.sdk.segment.models.SegmentAttributes
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.keyvaluestorage.KeyValueStorage
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 15/06/2023.
 */
internal class SegmentImpl : Segment, SegmentSynchronizer {
    private val keyValueStorage: KeyValueStorage get() = get()

    private var attributesFlow = MutableStateFlow(SegmentAttributes.Empty)

    override val attributes: SegmentAttributes
        get() = attributesFlow.value

    override var segmentId: String? = null
        private set

    override var segmentUid: ULong? = null
        private set

    override fun setAge(age: Int?) {
        attributesFlow.value = attributesFlow.value.copy(
            age = age
        )
        logInfo(TAG, "Updated age=$age")
    }

    override fun setGender(gender: Gender?) {
        attributesFlow.value = attributesFlow.value.copy(
            gender = gender
        )
        logInfo(TAG, "Updated gender=$gender")
    }

    override fun putCustomAttribute(attribute: String, value: Any?) {
        this.attributesFlow.update { current ->
            current.copy(
                customAttributes = current.customAttributes
                    .toMutableMap()
                    .also {
                        if (value == null) {
                            it.remove(attribute)
                        } else {
                            it[attribute] = value
                        }
                    }
            )
        }
        logInfo(TAG, "Updated attribute=($attribute, $value)")
    }

    override fun setCustomAttributes(attributes: Map<String, Any>) {
        this.attributesFlow.value = this.attributesFlow.value.copy(
            customAttributes = attributes
        )
        logInfo(TAG, "Updated attributes=$attributes")
    }

    override fun setLevel(level: Int) {
        attributesFlow.value = attributesFlow.value.copy(
            gameLevel = level
        )
        logInfo(TAG, "Updated level=$level")
    }

    override fun setTotalInAppAmount(inAppAmount: Double) {
        attributesFlow.value = attributesFlow.value.copy(
            inAppAmount = inAppAmount
        )
        logInfo(TAG, "Updated inAppAmount=$inAppAmount")
    }

    override fun setPaying(isPaying: Boolean) {
        attributesFlow.value = attributesFlow.value.copy(
            isPaying = isPaying
        )
        logInfo(TAG, "Updated isPaying=$isPaying")
    }

    override fun parseSegmentId(rootJsonResponse: String) {
        runCatching {
            val newSegmentId = JSONObject(rootJsonResponse)
                .optJSONObject("segment")
                ?.optString("id", "")
                ?.takeIf { it.isNotEmpty() }
            keyValueStorage.segmentId = newSegmentId
            setSegmentId(newSegmentId)
            val newSegmentUid = JSONObject(rootJsonResponse)
                .optJSONObject("segment")
                ?.optString("uid", "")
                ?.takeIf { it.isNotEmpty() }?.toULongOrNull()
            keyValueStorage.segmentUid = newSegmentUid
            setSegmentUid(newSegmentUid)
        }
    }

    @Deprecated("Use segmentUid instead")
    override fun setSegmentId(segmentId: String?) {
        logInfo(TAG, "Updated SegmentId($segmentId)")
        this.segmentId = segmentId
    }

    override fun setSegmentUid(segmentUid: ULong?) {
        logInfo(TAG, "Updated SegmentUid($segmentUid)")
        this.segmentUid = segmentUid
    }
}

private const val TAG = "Segment"