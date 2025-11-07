package org.bidon.sdk.adapter

import org.bidon.sdk.stats.models.RoundStatus

/**
 * Created by Bidon Team on 06/02/2023.
 *
 * Use to notify Demands for loss/win. It isn't related to stats [RoundStatus].
 */
public interface WinLossNotifiable {
    public fun notifyLoss(winnerNetworkName: String, winnerNetworkPrice: Double)
    public fun notifyWin()
}