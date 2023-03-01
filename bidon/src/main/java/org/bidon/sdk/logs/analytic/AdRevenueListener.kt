package org.bidon.sdk.logs.analytic

import org.bidon.sdk.ads.Ad

/**
 * Created by Aleksei Cherniaev on 09/02/2023.
 *
 * Bidon SDK allows you to get impression-level revenue data with Ad Revenue Callbacks.
 * This data includes information about network name, revenue, ad type, etc.
 */
interface AdRevenueListener {
    fun onRevenuePaid(ad: Ad, adValue: AdValue) {}
}
