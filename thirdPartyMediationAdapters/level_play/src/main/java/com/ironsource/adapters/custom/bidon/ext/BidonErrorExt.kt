package com.ironsource.adapters.custom.bidon.ext

import com.ironsource.mediationsdk.adunit.adapter.utility.AdapterErrorType
import org.bidon.sdk.config.BidonError

internal fun BidonError.asLevelPlayAdapterError() = when (this) {
    is BidonError.NoBid,
    is BidonError.NoFill,
    BidonError.NoRoundResults,
    BidonError.NoAuctionResults -> AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL

    is BidonError.Expired -> AdapterErrorType.ADAPTER_ERROR_TYPE_AD_EXPIRED

    else -> AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL
}

internal fun AdapterErrorType.getErrorCode() = when (this) {
    AdapterErrorType.ADAPTER_ERROR_TYPE_NO_FILL -> NO_FILL_ERROR
    AdapterErrorType.ADAPTER_ERROR_TYPE_AD_EXPIRED -> AD_EXPIRED_ERROR
    AdapterErrorType.ADAPTER_ERROR_TYPE_INTERNAL -> INTERNAL_ADAPTER_ERROR
}

internal const val MISSING_APP_KEY_ERROR = 100
internal const val AD_IS_NULL_ERROR = 509
internal const val NO_FILL_ERROR = 510
internal const val AD_NOT_READY_ERROR = 511
internal const val ACTIVITY_IS_NULL_ERROR = 512
internal const val AD_EXPIRED_ERROR = 513
internal const val BIDON_SHOW_ERROR = 514
internal const val INTERNAL_ADAPTER_ERROR = 666
