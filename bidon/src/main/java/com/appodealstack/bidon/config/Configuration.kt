package com.appodealstack.bidon.config

import com.appodealstack.bidon.demands.DemandId

interface Configuration {
    suspend fun getConfiguration(demandId: DemandId): Config.Demand?
}