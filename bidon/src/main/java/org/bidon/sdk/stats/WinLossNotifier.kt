package org.bidon.sdk.stats

interface WinLossNotifier {
    fun notifyLoss(winnerDemandId: String, winnerPrice: Double)
    fun notifyWin()
}
