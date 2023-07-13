package org.bidon.sdk.auction.impl

import kotlinx.coroutines.flow.MutableStateFlow
import org.bidon.sdk.adapter.AdSource
import org.bidon.sdk.adapter.DemandAd
import org.bidon.sdk.auction.AdTypeParam
import org.bidon.sdk.auction.Auction
import org.bidon.sdk.auction.AuctionHolder
import org.bidon.sdk.auction.AuctionResult
import org.bidon.sdk.config.BidonError
import org.bidon.sdk.logs.logging.impl.logError
import org.bidon.sdk.logs.logging.impl.logInfo
import org.bidon.sdk.utils.di.get
import org.bidon.sdk.utils.ext.asFailure
import org.bidon.sdk.utils.ext.asSuccess
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal class AuctionHolderImpl(
    private val demandAd: DemandAd,
) : AuctionHolder {
    private val auctionState = MutableStateFlow<AuctionHolderState>(AuctionHolderState.Idle)
    private var displayingWinner: AuctionResult? = null
    private var nextWinner: AuctionResult? = null
        set(value) {
            wasNotified.set(false)
            wasShown.set(false)
            field = value
        }
    private val wasNotified = AtomicBoolean(false)
    private val wasShown = AtomicBoolean(false)

    override val isAuctionActive: Boolean
        get() = auctionState.value is AuctionHolderState.InProgress

    override fun startAuction(
        adTypeParam: AdTypeParam,
        onResult: (Result<List<AuctionResult>>) -> Unit
    ) {
        val progressState = AuctionHolderState.InProgress()
        if (auctionState.compareAndSet(expect = AuctionHolderState.Idle, update = progressState)) {
            progressState.auction.start(
                demandAd = demandAd,
                adTypeParamData = adTypeParam,
                onSuccess = { results ->
                    check(results.isNotEmpty()) {
                        "Auction succeed if results is not empty"
                    }
                    logInfo(Tag, "Auction completed successfully: $results")
                    nextWinner = results.first()
                    onResult.invoke(results.asSuccess())
                    auctionState.value = AuctionHolderState.Idle
                },
                onFailure = {
                    nextWinner = null
                    logError(Tag, "Auction failed", it)
                    onResult.invoke(it.asFailure())
                    auctionState.value = AuctionHolderState.Idle
                }
            )
        } else {
            onResult.invoke(BidonError.AuctionInProgress.asFailure())
        }
    }

    override fun popWinnerForShow(): AdSource<*>? {
        synchronized(this) {
            displayingWinner?.adSource?.destroy()
            displayingWinner = nextWinner
            nextWinner = null
            wasShown.set(true)
            return displayingWinner?.adSource
        }
    }

    override fun getNextLoadedWinner(): AdSource<*>? {
        return nextWinner?.adSource
    }

    override fun destroy() {
        (auctionState.value as? AuctionHolderState.InProgress)?.auction?.cancel()
        auctionState.value = AuctionHolderState.Idle
        displayingWinner?.adSource?.destroy()
        displayingWinner = null
        nextWinner?.adSource?.destroy()
        nextWinner = null
    }

    override fun isAdReady(): Boolean {
        return nextWinner?.adSource?.isAdReadyToShow == true
    }

    override fun notifyWin() {
        logInfo(Tag, "Notify Win was invoked")
        if (wasShown.get()) {
            return
        }
        if (auctionState.value is AuctionHolderState.InProgress) {
            return
        }
        if (!wasNotified.getAndSet(true)) {
            nextWinner?.adSource?.sendWin()
        }
    }

    override fun notifyLoss(
        winnerDemandId: String,
        winnerEcpm: Double,
        onAuctionCancelled: () -> Unit,
        onNotified: () -> Unit,
    ) {
        logInfo(Tag, "Notify Loss invoked with Winner($winnerDemandId, $winnerEcpm)")
        when (val state = auctionState.value) {
            AuctionHolderState.Idle -> {
                if (!wasShown.get() && !wasNotified.getAndSet(true)) {
                    nextWinner?.adSource?.sendLoss(
                        winnerDemandId = winnerDemandId,
                        winnerEcpm = winnerEcpm,
                    )
                    onNotified()
                }
            }

            is AuctionHolderState.InProgress -> {
                state.auction.cancel()
                onAuctionCancelled()
                onNotified()
            }
        }
    }
}

@Suppress("CanSealedSubClassBeObject")
internal sealed interface AuctionHolderState {
    object Idle : AuctionHolderState
    class InProgress : AuctionHolderState {
        val auction: Auction by lazy { get() }
    }
}

private const val Tag = "AuctionHolder"