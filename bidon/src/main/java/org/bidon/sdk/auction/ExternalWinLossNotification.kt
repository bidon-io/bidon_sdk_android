package org.bidon.sdk.auction

/**
 * Created by Aleksei Cherniaev on 29/06/2023.
 */
internal interface ExternalWinLossNotification {
    fun notifyWin()

    fun notifyLoss(
        winnerDemandId: String,
        winnerEcpm: Double,

        /**
         * The publisher should be informed about failed loading in case of auction is cancelled.
         */
        onAuctionCancelled: () -> Unit,
        onNotified: () -> Unit,
    )
}