package com.appodealstack.mads.demands.banners

sealed interface AutoRefresh {
    object Off : AutoRefresh
    data class On(val timeoutMs: Long) : AutoRefresh
}