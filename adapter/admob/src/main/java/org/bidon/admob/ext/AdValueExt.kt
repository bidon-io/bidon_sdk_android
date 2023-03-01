package org.bidon.admob.ext

import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision

/**
 * Created by Aleksei Cherniaev on 21/02/2023.
 */

typealias GoogleAdValue = com.google.android.gms.ads.AdValue

internal fun GoogleAdValue.asBidonAdValue(): AdValue {
    return AdValue(
        adRevenue = this.valueMicros / 1_000_000.0,
        precision = when (this.precisionType) {
            0 -> Precision.Estimated // "UNKNOWN"
            1 -> Precision.Precise // "PRECISE"
            2 -> Precision.Estimated // "ESTIMATED"
            3 -> Precision.Precise // "PUBLISHER_PROVIDED"
            else -> Precision.Estimated // "unknown type ${adValue.precisionType}"
        },
        currency = AdValue.USD,
    )
}