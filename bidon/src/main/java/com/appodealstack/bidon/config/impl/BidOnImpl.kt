package com.appodealstack.bidon.config.impl

import com.appodealstack.bidon.BidOn
import com.appodealstack.bidon.BidOnBuilder
import com.appodealstack.bidon.config.BidOnInitializer
import com.appodealstack.bidon.logs.logging.Logger
import com.appodealstack.bidon.logs.logging.impl.LoggerImpl

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class BidOnImpl(
    private val bidOnInitializer: BidOnInitializer = BidOnInitializerImpl()
) : BidOn,
    BidOnBuilder by bidOnInitializer,
    Logger by LoggerImpl() {

    override fun isInitialized(): Boolean = bidOnInitializer.isInitialized
}
