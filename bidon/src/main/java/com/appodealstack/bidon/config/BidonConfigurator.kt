package com.appodealstack.bidon.config

import android.os.Bundle
import com.appodealstack.bidon.demands.DemandId

internal object BidonConfiguratorInstance : BidonConfigurator by BidonConfiguratorImpl()

internal interface BidonConfigurator {
    suspend fun getDemandConfig(demandId: DemandId): Bundle
    suspend fun getServiceConfig(demandId: DemandId): Bundle
    suspend fun getAuctionConfig(): Bundle
    fun addConfigurations(vararg configurations: Configuration)
}

internal class BidonConfiguratorImpl : BidonConfigurator {

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