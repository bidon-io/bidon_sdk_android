package org.bidon.sdk.databinders

import org.bidon.sdk.databinders.adapters.AdaptersBinder
import org.bidon.sdk.databinders.app.AppBinder
import org.bidon.sdk.databinders.device.DeviceBinder
import org.bidon.sdk.databinders.placement.PlacementBinder
import org.bidon.sdk.databinders.reg.RegulationsBinder
import org.bidon.sdk.databinders.segment.SegmentBinder
import org.bidon.sdk.databinders.session.SessionBinder
import org.bidon.sdk.databinders.test.TestModeBinder
import org.bidon.sdk.databinders.token.TokenBinder
import org.bidon.sdk.databinders.user.UserBinder

/**
 * Created by Bidon Team on 06/02/2023.
 */
internal class DataProviderImpl(
    private val deviceBinder: DeviceBinder,
    private val appBinder: AppBinder,
    private val sessionBinder: SessionBinder,
    private val userBinder: UserBinder,
    private val tokenBinder: TokenBinder,
    private val placementBinder: PlacementBinder,
    private val adaptersBinder: AdaptersBinder,
    private val segmentBinder: SegmentBinder,
    private val regulationsBinder: RegulationsBinder,
    private val testModeBinder: TestModeBinder,
) : DataProvider {

    override suspend fun provide(dataBinders: List<DataBinderType>): Map<String, Any> {
        return dataBinders.mapNotNull { type ->
            val binder = when (type) {
                DataBinderType.Device -> deviceBinder
                DataBinderType.App -> appBinder
                DataBinderType.Session -> sessionBinder
                DataBinderType.User -> userBinder
                DataBinderType.Token -> tokenBinder
                DataBinderType.Placement -> placementBinder
                DataBinderType.AvailableAdapters -> adaptersBinder
                DataBinderType.Segment -> segmentBinder
                DataBinderType.Reg -> regulationsBinder
                DataBinderType.Test -> testModeBinder
            }
            binder.getJsonObject()?.let { binder.fieldName to it }
        }.toMap()
    }
}
