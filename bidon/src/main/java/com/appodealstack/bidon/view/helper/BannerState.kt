package com.appodealstack.bidon.view.helper

import com.appodealstack.bidon.adapters.Ad
import com.appodealstack.bidon.auctions.data.models.AuctionResult

internal interface BannerState {
    sealed interface ShowState {
        object Idle : ShowState
        class Displaying(val auctionResult: AuctionResult) : ShowState
    }

    sealed interface ShowAction {
        class OnAdShown(val winner: AuctionResult, val ad: Ad) : ShowAction
        object OnShowInvoked : ShowAction
        object OnDestroyInvoked : ShowAction

        class OnStartAutoRefreshInvoked(val timeoutMs: Long) : ShowAction
        object OnStopAutoRefreshInvoked : ShowAction
        object OnRefreshTimeoutFinished : ShowAction
    }

    sealed interface LoadState {
        object Idle : LoadState
        object Loading : LoadState
        class Loaded(val auctionResult: AuctionResult) : LoadState
    }

    sealed interface LoadAction {
        object OnLoadInvoked : LoadAction
        object OnDestroyInvoked : LoadAction
        class OnAuctionSucceed(val auctionResults: List<AuctionResult>) : LoadAction
        class OnAuctionFailed(val cause: Throwable) : LoadAction

        object OnRefreshTimeoutFinished : LoadAction
        object OnWinnerTaken : LoadAction
    }
}