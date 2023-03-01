package org.bidon.sdk.logs.analytic

/**
 * Created by Aleksei Cherniaev on 21/02/2023.
 */

/**
 * Ad revenue info
 */
data class AdValue(
    val adRevenue: Double,
    val currency: String = USD,
    val precision: Precision
) {
    companion object {
        const val USD = "USD"
    }
}