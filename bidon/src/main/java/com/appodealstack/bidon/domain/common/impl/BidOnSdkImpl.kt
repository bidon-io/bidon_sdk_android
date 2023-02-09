package com.appodealstack.bidon.domain.common.impl

import com.appodealstack.bidon.BidOnBuilder
import com.appodealstack.bidon.BidOnSdk
import com.appodealstack.bidon.domain.config.BidOnInitializer
import com.appodealstack.bidon.domain.config.impl.BidOnInitializerImpl

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class BidOnSdkImpl(
    private val bidOnInitializer: BidOnInitializer = BidOnInitializerImpl()
) : BidOnSdk, BidOnBuilder by bidOnInitializer {

    override fun isInitialized(): Boolean = bidOnInitializer.isInitialized
}
