package com.appodealstack.bidon.logs.analytic

/**
 * Created by Aleksei Cherniaev on 21/02/2023.
 */

/**
 * Ad revenue info
 */
data class AdValue(
    val adRevenue: Double,
    val currency: String = DefaultCurrency,
    val precision: Precision
) {
    companion object {
        const val DefaultCurrency = "USD"
    }
}