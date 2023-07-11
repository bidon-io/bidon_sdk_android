package org.bidon.bidmachine.ext

import io.bidmachine.models.AuctionResult
import org.bidon.sdk.logs.analytic.AdValue
import org.bidon.sdk.logs.analytic.Precision

/**
 * Created by Bidon Team on 21/02/2023.
 */

internal fun AuctionResult?.asBidonAdValue(): AdValue {
    return AdValue(
        adRevenue = (this?.price ?: 0.0) / 1000.0,
        precision = Precision.Precise,
        currency = AdValue.USD
    )
}