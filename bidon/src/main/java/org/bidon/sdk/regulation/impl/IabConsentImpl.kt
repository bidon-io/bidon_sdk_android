@file:Suppress("DEPRECATION")

package org.bidon.sdk.regulation.impl

import android.content.Context
import android.preference.PreferenceManager
import org.bidon.sdk.regulation.Iab
import org.bidon.sdk.regulation.IabConsent
import org.bidon.sdk.utils.di.getOrNull
import org.bidon.sdk.utils.json.jsonObject

internal class IabConsentImpl : IabConsent {

    override val iab: Iab
        get() = obtainIab()

    private fun obtainIab(): Iab {
        return try {
            val shared = getOrNull<Context>()?.let {
                PreferenceManager.getDefaultSharedPreferences(it)
            }
            val tcfV1 = shared?.getString("IABConsent_SubjectToGDPR", null)
            val jsonV1 = tcfV1?.let {
                jsonObject {
                    "IABConsent_SubjectToGDPR" hasValue it
                }.toString()
            }

            val tcfV2 = shared?.getInt("IABTCF_gdprApplies", -1).takeIf { it != -1 }
            val jsonV2 = tcfV2?.let {
                jsonObject {
                    "IABTCF_gdprApplies" hasValue it
                }.toString()
            }

            val usPrivacy = shared?.getString("IABUSPrivacy_String", null)

            Iab(
                tcfV1 = jsonV1,
                tcfV2 = jsonV2,
                usPrivacy = usPrivacy,
            )
        } catch (e: Exception) {
            Iab(
                tcfV1 = null,
                tcfV2 = null,
                usPrivacy = null,
            )
        }
    }
}