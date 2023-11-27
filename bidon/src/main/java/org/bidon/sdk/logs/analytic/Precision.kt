package org.bidon.sdk.logs.analytic

import org.bidon.sdk.adapter.Adapter
import org.bidon.sdk.auction.models.AdUnit

/**
 * Created by Bidon Team on 21/02/2023.
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
     * Based on eCPM ad revenue [AdUnit.pricefloor].
     *
     * Available if the [Adapter] doesn't provide precise value
     */
    Estimated
}