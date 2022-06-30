package com.appodealstack.mads.config

import android.os.Bundle

sealed interface Config {
    fun interface Demand {
        fun configParams(): Bundle
    }

    interface Analytic
    interface Auction
}