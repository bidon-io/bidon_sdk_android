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

    @Deprecated("Use segmentUid instead")
    override var segmentId: String? = null
        private set

    override var segmentUid: String? = null
        private set

    override var age: Int?
        get() = attributesFlow.value.age
        set(value) {
            attributesFlow.value = attributesFlow.value.copy(
                age = value
            )
            logInfo(TAG, "Updated age=$value")
        }

    override var gender: Gender?
        get() = attributesFlow.value.gender
        set(value) {
            attributesFlow.value = attributesFlow.value.copy(
                gender = value
            )
            logInfo(TAG, "Updated gender=$value")
        }

    override var level: Int?
        get() = attributesFlow.value.gameLevel
        set(value) {
            attributesFlow.value = attributesFlow.value.copy(
                gameLevel = value
            )
            logInfo(TAG, "Updated level=$value")
        }

    override var totalInAppAmount: Double?
        get() = attributesFlow.value.inAppAmount
        set(value) {
            attributesFlow.value = attributesFlow.value.copy(
                inAppAmount = value
            )
            logInfo(TAG, "Updated inAppAmount=$value")
        }

    override var isPaying: Boolean
        get() = attributesFlow.value.isPaying ?: false
        set(value) {
            attributesFlow.value = attributesFlow.value.copy(
                isPaying = value
            )
            logInfo(TAG, "Updated isPaying=$value")
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
                ?.takeIf { it.isNotEmpty() }
            keyValueStorage.segmentUid = newSegmentUid
            setSegmentUid(newSegmentUid)
        }
    }

    @Deprecated("Use segmentUid instead")
    override fun setSegmentId(segmentId: String?) {
        logInfo(TAG, "Updated SegmentId($segmentId)")
        this.segmentId = segmentId
    }

    override fun setSegmentUid(segmentUid: String?) {
        logInfo(TAG, "Updated SegmentUid($segmentUid)")
        this.segmentUid = segmentUid
    }
}

private const val TAG = "Segment"