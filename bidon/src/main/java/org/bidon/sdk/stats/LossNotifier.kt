package org.bidon.sdk.stats

interface LossNotifier {
    fun notifyLoss(winnerDemandId: String, winnerEcpm: Double)
}
