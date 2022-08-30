package com.appodealstack.bidon.utilities.datasource.user.toconsentlib

import com.appodealstack.bidon.utilities.asList
import org.json.JSONArray
import org.json.JSONObject

/**
 * A consent provider for a separate source of advertising traffic?.
 *
 * @param id IAB id. If your are not register as IAB vendor you can use custom id.
 * @param name vendor name. Will be displayed in consent form.
 * @param bundle custom string to check consent result for vendor.
 * @param policyUrl vendor's  policyUrl.
 * @param purposeIds IAB purposes ids array.
 * @param featureIds IAB features ids array.
 * @param legitimateInterestPurposeIds IAB leg int purposes ids array.
 */
// TODO should be in separate ConsentLibrary
class Vendor @JvmOverloads constructor(
    val id: Int,
    val name: String,
    val bundle: String,
    val policyUrl: String,
    val purposeIds: List<Int> = listOf(),
    val featureIds: List<Int> = listOf(),
    val legitimateInterestPurposeIds: List<Int> = listOf()
) {
    internal constructor(json: JSONObject) : this(
        id = json.optInt("apdId"),
        name = json.optString("name"),
        bundle = json.optString("status"),
        policyUrl = json.optString("policyUrl"),
        purposeIds = json.optJSONArray("purposeIds").asList<Int>(),
        featureIds = json.optJSONArray("featureIds").asList<Int>(),
        legitimateInterestPurposeIds = json.optJSONArray("legIntPurposeIds").asList<Int>(),
    )

    internal fun toJson(): JSONObject =
        JSONObject().apply {
            put("apdId", id.takeIf { it != 0 })
            put("name", name.takeIf { it.isNotEmpty() })
            put("status", bundle.takeIf { it.isNotEmpty() })
            put("policyUrl", policyUrl.takeIf { it.isNotEmpty() })
            put("purposeIds", JSONArray(purposeIds).takeIf { it.length() != 0 })
            put("featureIds", JSONArray(featureIds).takeIf { it.length() != 0 })
            put("legIntPurposeIds", JSONArray(legitimateInterestPurposeIds).takeIf { it.length() != 0 })
        }

    /**
     * Class-builder for [Vendor].
     *
     * @param id IAB id. If your are not register as IAB vendor you can use custom id.
     * @param name vendor name. Will be displayed in consent form.
     * @param bundle custom string to check consent result for vendor.
     * @param policyUrl vendor's  policyUrl.
     * @param purposeIds IAB purposes ids array.
     * @param featureIds IAB features ids array.
     * @param legitimateInterestPurposeIds IAB leg int purposes ids array.
     */
    data class Builder @JvmOverloads constructor(
        private var id: Int? = null,
        private var name: String,
        private var bundle: String,
        private var policyUrl: String,
        private var purposeIds: List<Int> = listOf(),
        private var featureIds: List<Int> = listOf(),
        private var legitimateInterestPurposeIds: List<Int> = listOf()
    ) {
        fun id(id: Int): Builder = apply { this.id = id }
        fun purposeIds(purposeIds: List<Int>): Builder = apply { this.purposeIds = purposeIds }
        fun featureIds(featureIds: List<Int>): Builder = apply { this.featureIds = featureIds }
        fun legitimateInterestPurposeIds(legitimateInterestPurposeIds: List<Int>): Builder =
            apply { this.legitimateInterestPurposeIds = legitimateInterestPurposeIds }

        /**
         * build [Vendor].
         *
         * @return new [Vendor].
         * */
        fun build() = Vendor(
            id = id ?: -bundle.hashCode(),
            name = name,
            bundle = bundle,
            policyUrl = policyUrl,
            purposeIds = purposeIds,
            featureIds = featureIds,
            legitimateInterestPurposeIds = legitimateInterestPurposeIds
        )
    }
}