package com.appodealstack.mads.config

import android.os.Bundle
import com.appodealstack.mads.demands.DemandId

internal object MadsConfiguratorInstance : MadsConfigurator by MadsConfiguratorImpl()

internal interface MadsConfigurator {
    suspend fun getDemandConfig(demandId: DemandId): Bundle
    suspend fun getServiceConfig(demandId: DemandId): Bundle
    suspend fun getAuctionConfig(): Bundle
    fun addConfigurations(vararg configurations: Configuration)
}

internal class MadsConfiguratorImpl : MadsConfigurator {

    private val emptyConfig
        get() = Config.Demand {
            Bundle()
        }

    private val configurations = mutableListOf<Configuration>()

    override suspend fun getDemandConfig(demandId: DemandId): Bundle {
        return (configurations.firstOrNull {
            it.getConfiguration(demandId) != null
        }?.getConfiguration(demandId) ?: emptyConfig).configParams()
    }

    override suspend fun getServiceConfig(demandId: DemandId): Bundle {
        TODO("Not yet implemented")
    }

    override suspend fun getAuctionConfig(): Bundle {
        TODO("Not yet implemented")
    }

    override fun addConfigurations(vararg configurations: Configuration) {
        this.configurations.addAll(configurations)
    }
}