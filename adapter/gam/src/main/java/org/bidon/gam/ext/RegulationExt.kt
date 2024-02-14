package org.bidon.gam.ext

import android.os.Bundle
import org.bidon.gam.GamDemandId
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.regulation.Regulation

internal fun Regulation.asBundle() = Bundle().apply {
    logInfo("GamAdapter", "Applying regulation to ${GamDemandId.demandId}")
    this@asBundle.usPrivacyString?.let {
        putString("IABUSPrivacy_String", it)
    }
    this@asBundle.gdprConsentString?.let {
        putString("IABConsent_ConsentString", it)
    }
    putBoolean("IABConsent_SubjectToGDPR", this@asBundle.gdprApplies)
}