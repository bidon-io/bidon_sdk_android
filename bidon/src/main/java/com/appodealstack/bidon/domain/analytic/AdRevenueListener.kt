package com.appodealstack.bidon.domain.analytic

import com.appodealstack.bidon.domain.common.Ad

/**
 * Created by Aleksei Cherniaev on 09/02/2023.
 *
 * BidOn SDK allows you to get impression-level revenue data with Ad Revenue Callbacks.
 * This data includes information about network name, revenue, ad type, etc.
 */
interface AdRevenueListener {
    fun onRevenuePaid(ad: Ad) {}
}