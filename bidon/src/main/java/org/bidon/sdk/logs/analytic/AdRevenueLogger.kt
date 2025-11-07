package org.bidon.sdk.logs.analytic

import org.bidon.sdk.ads.Ad

/**
 * Created by Bidon Team on 06/02/2023.
 */
public interface AdRevenueLogger {
    public fun logAdRevenue(ad: Ad)
}
