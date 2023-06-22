package org.bidon.admob.ext

import android.os.Bundle
import org.bidon.admob.AdmobDemandId
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.regulation.Regulation

/**
 * Created by Aleksei Cherniaev on 22/06/2023.
 */
internal fun Regulation.asBundle() = Bundle().apply {
    logInfo("AdmobAdapter", "Applying regulation to ${AdmobDemandId.demandId}")
    this@asBundle.usPrivacyString?.let {
        putString("IABUSPrivacy_String", it)
    }
    this@asBundle.gdprConsentString?.let {
        putString("IABConsent_ConsentString", it)
    }
    putBoolean("IABConsent_SubjectToGDPR", this@asBundle.gdprConsent)
}