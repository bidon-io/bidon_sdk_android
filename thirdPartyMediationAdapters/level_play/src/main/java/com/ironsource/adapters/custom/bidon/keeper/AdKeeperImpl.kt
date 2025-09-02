package com.ironsource.adapters.custom.bidon.keeper

import com.ironsource.adapters.custom.bidon.logger.LevelPLaySdkLogger
import com.ironsource.adapters.custom.bidon.logger.Logger
import java.util.TreeSet

internal class AdKeeperImpl<T : AdInstance>(
    private val adType: String
) : AdKeeper<T>, Logger by LevelPLaySdkLogger {

    private val registeredEcpm: TreeSet<Double> = TreeSet<Double>()
    private var lastRegisteredEcpm: Double? = null
    private var adInstance: T? = null

    override fun registerEcpm(ecpm: Double) {
        log(TAG, "[$adType] Registering eCPM: $ecpm")
        registeredEcpm.add(ecpm)
        lastRegisteredEcpm = ecpm
        log(TAG, "[$adType] Current registered eCPM values: $registeredEcpm")
    }

    override fun lastRegisteredEcpm(): Double? = lastRegisteredEcpm

    override fun keepAd(newAdInstance: T): T? {
        val currentAdInstance = adInstance
        return if (currentAdInstance == null || currentAdInstance.ecpm < newAdInstance.ecpm) {
            log(
                TAG,
                "[$adType] Keeping new ad instance with eCPM: ${newAdInstance.ecpm} (previous: ${currentAdInstance?.ecpm ?: "none"})"
            )
            adInstance = newAdInstance
            currentAdInstance?.notifyLoss(
                winnerDemandId = newAdInstance.demandId,
                winnerPrice = newAdInstance.ecpm
            )
            currentAdInstance
        } else {
            log(
                TAG,
                "[$adType] New ad instance rejected (current eCPM: ${currentAdInstance.ecpm}, new eCPM: ${newAdInstance.ecpm})"
            )
            newAdInstance
        }
    }

    override fun consumeAd(ecpm: Double): T? {
        val currentAdInstance = adInstance ?: run {
            log(TAG, "[$adType] No ad available for consumption")
            return null
        }

        val currentEcpm = currentAdInstance.ecpm
        val lowerBound = registeredEcpm.floor(ecpm) ?: ecpm
        val upperBound = registeredEcpm.higher(ecpm) ?: ecpm

        log(
            TAG,
            "[$adType] Attempting to consume ad with eCPM: $ecpm (range: $lowerBound - $upperBound), current ad eCPM: $currentEcpm"
        )

        return if (currentEcpm in lowerBound..upperBound) {
            log(TAG, "[$adType] Ad with eCPM: $currentEcpm consumed and removed")
            adInstance = null
            currentAdInstance.notifyWin()
            currentAdInstance
        } else {
            log(TAG, "[$adType] No matching ad found in range for eCPM: $ecpm")
            null
        }
    }
}

private const val TAG = "AdKeeperImpl"
