package org.bidon.sdk.config.impl

import org.bidon.sdk.config.BidonInitializer
import org.bidon.sdk.databinders.extras.Extras
import org.bidon.sdk.databinders.extras.ExtrasImpl
import org.bidon.sdk.logs.logging.Logger
import org.bidon.sdk.logs.logging.impl.LoggerImpl

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal class Bidon :
    BidonInitializer by BidonInitializerImpl(),
    Logger by LoggerImpl(),
    Extras by ExtrasImpl()