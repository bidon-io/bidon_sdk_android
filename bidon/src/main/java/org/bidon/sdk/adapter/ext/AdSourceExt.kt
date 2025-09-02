package org.bidon.sdk.adapter.ext

import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.WinLossNotifiable
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.stats.StatisticsCollector
import org.bidon.sdk.stats.models.BidType

internal val AdSource<*>.ad get() = (this as StatisticsCollector).getAd()

private const val TAG = "AdSourceExt"

/**
 * Sends loss notification to server or adapter based on bid type if conditions are met.
 * Respects externalWinNotificationsEnabled flag and prevents duplicate notifications.
 */
internal fun AdSource<*>.notifyExternalLoss(winnerDemandId: String, winnerPrice: Double) {
    processWinLossNotification(
        notificationType = "loss",
        onRtbNotification = { sendLoss(winnerDemandId, winnerPrice) },
        onCpmNotification = { (this as? WinLossNotifiable)?.notifyLoss(winnerDemandId, winnerPrice) }
    )
}

/**
 * Sends win notification to server or adapter based on bid type if conditions are met.
 * Respects externalWinNotificationsEnabled flag and prevents duplicate notifications.
 */
internal fun AdSource<*>.notifyExternalWin() {
    processWinLossNotification(
        notificationType = "win",
        onRtbNotification = { sendWin() },
        onCpmNotification = { (this as? WinLossNotifiable)?.notifyWin() }
    )
}

/**
 * Common logic for processing win/loss notifications based on bid type.
 */
private inline fun AdSource<*>.processWinLossNotification(
    notificationType: String,
    onRtbNotification: StatisticsCollector.() -> Unit,
    onCpmNotification: () -> Unit
) {
    val statisticsCollector = this as StatisticsCollector

    if (!statisticsCollector.canSendWinLoseNotifications()) {
        logInfo(TAG, "Not sending $notificationType notification: ${statisticsCollector.demandId}")
        return
    }

    statisticsCollector.markWinLoseNotificationsSent()

    val bidType = ad?.bidType ?: statisticsCollector.getStats().bidType
    when (bidType) {
        BidType.RTB -> {
            logInfo(TAG, "Sending $notificationType notification to server: ${statisticsCollector.demandId}")
            statisticsCollector.onRtbNotification()
        }
        BidType.CPM -> {
            logInfo(TAG, "Sending $notificationType notification to adapter: ${statisticsCollector.demandId}")
            onCpmNotification()
        }
        null -> {
            logInfo(TAG, "BidType is null. Not sending $notificationType notification: ${statisticsCollector.demandId}")
        }
    }
}
