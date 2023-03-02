package org.bidon.sdk.logs.analytic

import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.auction.models.LineItem

/**
 * Created by Aleksei Cherniaev on 21/02/2023.
 */

/**
 * Ad revenue precision variants
 */
enum class Precision {
    /**
     * Accurate ad revenue provided by the [Adapter] (AdNetwork)
     */
    Precise,

    /**
     * Based on eCPM ad revenue [LineItem.pricefloor].
     *
     * Available if the [Adapter] doesn't provide precise value
     */
    Estimated
}