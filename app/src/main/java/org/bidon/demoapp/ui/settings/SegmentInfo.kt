package org.bidon.demoapp.ui.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.segment.models.Gender

/**
 * Segment values
 */
internal object SegmentInfo {
    private val genderMutableStateFlow = MutableStateFlow<Gender?>(null)
    private val ageMutableStateFlow = MutableStateFlow(0)
    private val levelMutableStateFlow = MutableStateFlow(0)
    private val inAppAmountMutableStateFlow = MutableStateFlow(0.0)
    private val isPayingMutableStateFlow = MutableStateFlow(false)
    private val customAttributesMutableStateFlow = MutableStateFlow<Map<String, Any>>(emptyMap())

    val gender: StateFlow<Gender?> = genderMutableStateFlow.asStateFlow()
    val age: StateFlow<Int> = ageMutableStateFlow.asStateFlow()
    val level: StateFlow<Int> = levelMutableStateFlow.asStateFlow()
    val inAppAmount: StateFlow<Double> = inAppAmountMutableStateFlow.asStateFlow()
    val isPaying: StateFlow<Boolean> = isPayingMutableStateFlow.asStateFlow()
    val customAttributes: StateFlow<Map<String, Any>> = customAttributesMutableStateFlow.asStateFlow()

    fun setGender(gender: Gender?) {
        BidonSdk.segment.gender = gender
        this.genderMutableStateFlow.value = gender
    }

    fun setAge(age: Int) {
        BidonSdk.segment.age = age
        this.ageMutableStateFlow.value = age
    }

    fun setLevel(level: Int) {
        BidonSdk.segment.level = level
        this.levelMutableStateFlow.value = level
    }

    fun setTotalInAppAmount(inAppAmount: Double) {
        BidonSdk.segment.totalInAppAmount = inAppAmount
        this.inAppAmountMutableStateFlow.value = inAppAmount
    }

    fun setPaying(isPaying: Boolean) {
        BidonSdk.segment.isPaying = isPaying
        this.isPayingMutableStateFlow.value = isPaying
    }

    fun addCustomAttribute(attribute: String, value: Any) {
        BidonSdk.segment.putCustomAttribute(attribute, value)
        customAttributesMutableStateFlow.value = customAttributes.value + (attribute to value)
    }

    fun clearCustomAttribute() {
        BidonSdk.segment.setCustomAttributes(emptyMap())
        customAttributesMutableStateFlow.value = emptyMap()
    }
}