package org.bidon.sdk.config.impl

import org.bidon.sdk.config.BidonInitializer
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.databinders.extras.ExtrasImpl
import org.bidon.sdk.logs.logging.Logger
import org.bidon.sdk.logs.logging.impl.LoggerImpl
import org.bidon.sdk.regulation.Consent
import org.bidon.sdk.regulation.impl.ConsentImpl
import org.bidon.sdk.segment.Segmentation
import org.bidon.sdk.segment.impl.SegmentationImpl

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal class Bidon :
    BidonInitializer by BidonInitializerImpl(),
    Logger by LoggerImpl(),
    Extras by ExtrasImpl(),
    Segmentation by SegmentationImpl(),
    Consent by ConsentImpl()