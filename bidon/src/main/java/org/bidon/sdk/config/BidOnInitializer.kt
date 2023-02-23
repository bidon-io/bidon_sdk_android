package org.bidon.sdk.config

import org.bidon.sdk.BidOnBuilder

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface BidOnInitializer : BidOnBuilder {
    val isInitialized: Boolean
}
