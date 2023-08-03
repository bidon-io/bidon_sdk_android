@file:Suppress("DEPRECATION")

package org.bidon.sdk.regulation.impl

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import org.bidon.sdk.regulation.Iab
import org.bidon.sdk.regulation.IabConsent

internal class IabConsentImpl(
    private val context: Context
) : IabConsent {

    private val shared: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    override val iab: Iab
        get() = Iab(
            tcfV1 = shared.getString("IABConsent_SubjectToGDPR", null),
            tcfV2 = shared.getString("IABTCF_gdprApplies", null),
            usPrivacy = shared.getString("IABUSPrivacy_String", null),
        )
}