package org.bidon.sdk.stats

interface WinLossNotifier {
    fun notifyLoss(winnerDemandId: String, winnerEcpm: Double)
    fun notifyWin()
}
