package org.bidon.sdk.config.impl

import org.bidon.sdk.BidOn
import org.bidon.sdk.BidOnBuilder
import org.bidon.sdk.config.BidOnInitializer
import org.bidon.sdk.logs.logging.Logger
import org.bidon.sdk.logs.logging.impl.LoggerImpl

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
