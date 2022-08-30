package com.appodealstack.bidon.config.domain.impl

import com.appodealstack.bidon.config.domain.DataBinderType
import com.appodealstack.bidon.config.domain.DataProvider
import com.appodealstack.bidon.config.domain.databinders.*
import com.appodealstack.bidon.config.domain.databinders.AppBinder
import com.appodealstack.bidon.config.domain.databinders.DeviceBinder
import com.appodealstack.bidon.config.domain.databinders.GeoBinder
import com.appodealstack.bidon.config.domain.databinders.SessionBinder
import com.appodealstack.bidon.config.domain.databinders.UserBinder
import kotlinx.serialization.json.JsonElement

internal class DataProviderImpl(
    private val deviceBinder: DeviceBinder,
    private val appBinder: AppBinder,
    private val geoBinder: GeoBinder,
    private val sessionBinder: SessionBinder,
    private val userBinder: UserBinder,
    private val tokenBinder: TokenBinder,
    private val placementBinder: PlacementBinder
) : DataProvider {

    override suspend fun provide(dataBinders: List<DataBinderType>): Map<String, JsonElement> {
        return dataBinders.associate { type ->
            val binder = when (type) {
                DataBinderType.Device -> deviceBinder
                DataBinderType.App -> appBinder
                DataBinderType.Geo -> geoBinder
                DataBinderType.Session -> sessionBinder
                DataBinderType.User -> userBinder
                DataBinderType.Token -> tokenBinder
                DataBinderType.Placement -> placementBinder
            }
            binder.fieldName to binder.getJsonElement()
        }
    }
}
