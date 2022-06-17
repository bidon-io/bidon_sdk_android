package com.appodealstack.adapter.admob

import com.appodealstack.mads.config.Config
import com.appodealstack.mads.config.Configuration
import com.appodealstack.mads.demands.DemandId

class FirebaseRemoteConfigConfiguration: Configuration {
    override suspend fun getConfiguration(demandId: DemandId): Config.Demand {
        TODO("RemoteConfig Not yet implemented")
    }
}