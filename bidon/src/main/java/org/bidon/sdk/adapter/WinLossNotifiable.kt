package org.bidon.sdk.adapter

import org.bidon.sdk.stats.models.RoundStatus

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 *
 * Use to notify Demands for loss/win. It isn't related to stats [RoundStatus].
 */
interface WinLossNotifiable {
    fun notifyLoss()
    fun notifyWin()
}