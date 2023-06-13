package org.bidon.demoapp

import androidx.activity.ComponentActivity
import org.bidon.sdk.BidonSdk
import org.bidon.sdk.logs.logging.Logger

/**
 * Created by Bidon Team on 07/03/2023.
 */
internal object StepSdkInitialization {
    fun perform(activity: ComponentActivity) {
        BidonSdk
            .setLoggerLevel(Logger.Level.Verbose)
            .registerDefaultAdapters()
            .initialize(activity, "b1689e101a2555084e08c2ba7375783bde166625bbeae00f")
        while (BidonSdk.isInitialized().not()) {
            Thread.sleep(1000)
        }
    }
}