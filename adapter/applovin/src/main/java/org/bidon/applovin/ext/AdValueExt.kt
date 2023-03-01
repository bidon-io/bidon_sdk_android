package org.bidon.applovin.ext

import com.applovin.mediation.MaxAd
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision

/**
 * Created by Aleksei Cherniaev on 21/02/2023.
 */
internal fun Double?.asBidonAdValue(): AdValue {
    return AdValue(
        adRevenue = (this ?: 0.0) / 1000.0,
        precision = Precision.Estimated,
        currency = AdValue.USD
    )
}

internal fun MaxAd.asBidonAdValue(): AdValue {
    return AdValue(
        adRevenue = this.revenue,
        precision = when (this.revenuePrecision) {
            "exact" -> Precision.Precise
            else -> Precision.Estimated
        },
        currency = AdValue.USD
    )
}
