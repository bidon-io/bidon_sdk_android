package org.bidon.sdk.databinders

import org.bidon.sdk.databinders.adapters.AdaptersBinder
import org.bidon.sdk.databinders.app.AppBinder
import org.bidon.sdk.databinders.device.DeviceBinder
import org.bidon.sdk.databinders.geo.GeoBinder
import org.bidon.sdk.databinders.placement.PlacementBinder
import org.bidon.sdk.databinders.segment.SegmentBinder
import org.bidon.sdk.databinders.session.SessionBinder
import org.bidon.sdk.databinders.token.TokenBinder
import org.bidon.sdk.databinders.user.UserBinder

/**
 * Created by Bidon Team on 06/02/2023.
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
        return dataBinders.mapNotNull { type ->
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
            binder.getJsonObject()?.let { binder.fieldName to it }
        }.toMap()
    }
}
