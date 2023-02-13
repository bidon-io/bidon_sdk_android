package com.appodealstack.bidon.databinders

import com.appodealstack.bidon.databinders.adapters.AdaptersBinder
import com.appodealstack.bidon.databinders.app.AppBinder
import com.appodealstack.bidon.databinders.device.DeviceBinder
import com.appodealstack.bidon.databinders.geo.GeoBinder
import com.appodealstack.bidon.databinders.placement.PlacementBinder
import com.appodealstack.bidon.databinders.segment.SegmentBinder
import com.appodealstack.bidon.databinders.session.SessionBinder
import com.appodealstack.bidon.databinders.token.TokenBinder
import com.appodealstack.bidon.databinders.user.UserBinder

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
