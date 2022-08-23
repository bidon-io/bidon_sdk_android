package com.appodealstack.bidon.utilities.datasource

import com.appodealstack.bidon.core.ContextProvider
import com.appodealstack.bidon.utilities.datasource.app.AppDataSourceImpl
import com.appodealstack.bidon.utilities.datasource.device.DeviceDataSourceImpl
import com.appodealstack.bidon.utilities.datasource.location.LocationDataSourceImpl
import com.appodealstack.bidon.utilities.datasource.placement.PlacementDataSourceImpl
import com.appodealstack.bidon.utilities.datasource.session.SessionDataSourceImpl
import com.appodealstack.bidon.utilities.datasource.token.TokenDataSourceImpl
import com.appodealstack.bidon.utilities.datasource.user.UserDataSourceImpl

internal interface DataSource {

    fun getDataSource(type: SourceType, contextProvider: ContextProvider): DataSource {
        return when (type) {
            SourceType.App -> AppDataSourceImpl(contextProvider)
            SourceType.Device -> DeviceDataSourceImpl(contextProvider)
            SourceType.Location -> LocationDataSourceImpl(contextProvider)
            SourceType.Session -> SessionDataSourceImpl(contextProvider)
            SourceType.Token -> TokenDataSourceImpl()
            SourceType.User -> UserDataSourceImpl()//TODO how to pass regulator correctly)
            SourceType.Placement -> PlacementDataSourceImpl()
        }
    }
}

enum class SourceType {
    App,
    Device,
    Location,
    Session,
    Token,
    User,
    Placement
}