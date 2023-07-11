package org.bidon.sdk.adapter

import org.bidon.sdk.stats.models.RoundStatus

/**
 * Created by Bidon Team on 06/02/2023.
 *
 * Use to notify Demands for loss/win. It isn't related to stats [RoundStatus].
 */
interface WinLossNotifiable {
    fun notifyLoss(winnerNetworkName: String, winnerNetworkPrice: Double)
    fun notifyWin()
}