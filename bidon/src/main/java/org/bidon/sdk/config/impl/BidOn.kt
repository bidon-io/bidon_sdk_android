package org.bidon.sdk.config.impl

import org.bidon.sdk.config.BidOnInitializer
import org.bidon.sdk.logs.logging.Logger
import org.bidon.sdk.logs.logging.impl.LoggerImpl

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class BidOn :
    BidOnInitializer by BidOnInitializerImpl(),
    Logger by LoggerImpl()