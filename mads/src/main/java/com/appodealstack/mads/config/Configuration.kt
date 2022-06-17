package com.appodealstack.mads.config

import com.appodealstack.mads.demands.DemandId

interface Configuration {
    suspend fun getConfiguration(demandId: DemandId): Config.Demand?
}