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

const val MISSING_APP_KEY_ERROR = 100
const val AD_IS_NULL_ERROR = 509
const val NO_FILL_ERROR = 510
const val AD_NOT_READY_ERROR = 511
const val ACTIVITY_IS_NULL_ERROR = 512
const val AD_EXPIRED_ERROR = 513
const val BIDON_SHOW_ERROR = 514
const val INTERNAL_ADAPTER_ERROR = 666
