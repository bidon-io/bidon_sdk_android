package com.appodealstack.bidon.domain.config

import com.appodealstack.bidon.BidOnBuilder

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal interface BidOnInitializer : BidOnBuilder {
    val isInitialized: Boolean
}
