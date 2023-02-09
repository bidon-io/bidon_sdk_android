package com.appodealstack.bidon.data.binderdatasources

import com.appodealstack.bidon.domain.databinders.*
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class DataProviderImpl(
    private val deviceBinder: DeviceBinder,
    private val appBinder: AppBinder,
    private val geoBinder: GeoBinder,
    private val sessionBinder: SessionBinder,
    private val userBinder: UserBinder,
    private val tokenBinder: TokenBinder,
    private val placementBinder: PlacementBinder,
    private val adaptersBinder: AdaptersBinder,
    private val segmentBinder: SegmentBinder,
) : DataProvider {

    override suspend fun provide(dataBinders: List<DataBinderType>): Map<String, Any> {
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
                DataBinderType.Segment -> segmentBinder
            }
            binder.fieldName to binder.getJsonObject()
        }
    }
}
