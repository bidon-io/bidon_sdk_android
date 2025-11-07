package org.bidon.sdk.stats

public interface WinLossNotifier {
    public fun notifyLoss(winnerDemandId: String, winnerPrice: Double)
    public fun notifyWin()
}
