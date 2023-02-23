package org.bidon.bidmachine.ext

import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision
import io.bidmachine.models.AuctionResult

/**
 * Created by Aleksei Cherniaev on 21/02/2023.
 */

internal fun AuctionResult?.asBidonAdValue(): AdValue {
    return AdValue(
        adRevenue = (this?.price ?: 0.0) / 1000.0,
        precision = Precision.Precise,
        currency = AdValue.DefaultCurrency
    )
}