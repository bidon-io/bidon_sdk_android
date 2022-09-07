package com.appodealstack.bidon.data.binderdatasources

import com.appodealstack.bidon.domain.databinders.AdaptersBinder
import com.appodealstack.bidon.domain.databinders.AppBinder
import com.appodealstack.bidon.domain.databinders.DataBinderType
import com.appodealstack.bidon.domain.databinders.DeviceBinder
import com.appodealstack.bidon.domain.databinders.GeoBinder
import com.appodealstack.bidon.domain.databinders.PlacementBinder
import com.appodealstack.bidon.domain.databinders.SessionBinder
import com.appodealstack.bidon.domain.databinders.TokenBinder
import com.appodealstack.bidon.domain.databinders.UserBinder
import kotlinx.serialization.json.JsonElement

internal class DataProviderImpl(
    private val deviceBinder: DeviceBinder,
    private val appBinder: AppBinder,
    private val geoBinder: GeoBinder,
    private val sessionBinder: SessionBinder,
    private val userBinder: UserBinder,
    private val tokenBinder: TokenBinder,
    private val placementBinder: PlacementBinder,
    private val adaptersBinder: AdaptersBinder
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
                DataBinderType.AvailableAdapters -> adaptersBinder
            }
            binder.fieldName to binder.getJsonElement()
        }
    }
}
