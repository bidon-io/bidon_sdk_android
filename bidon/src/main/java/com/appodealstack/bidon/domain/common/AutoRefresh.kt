package com.appodealstack.bidon.domain.common

internal sealed interface AutoRefresh {
    object Off : AutoRefresh
    data class On(val timeoutMs: Long) : AutoRefresh
}
