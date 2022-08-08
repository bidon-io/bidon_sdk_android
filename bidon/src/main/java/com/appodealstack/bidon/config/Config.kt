package com.appodealstack.bidon.config

import android.os.Bundle

sealed interface Config {
    fun interface Demand {
        fun configParams(): Bundle
    }

    interface Analytic
    interface Auction
}