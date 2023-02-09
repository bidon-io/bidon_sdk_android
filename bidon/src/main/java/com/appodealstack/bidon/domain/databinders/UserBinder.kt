package com.appodealstack.bidon.domain.databinders

import com.appodealstack.bidon.data.binderdatasources.user.UserDataSource
import com.appodealstack.bidon.data.json.JsonParsers
import com.appodealstack.bidon.data.models.config.User
import org.json.JSONObject

/**
 * Created by Aleksei Cherniaev on 06/02/2023.
 */
internal class UserBinder(
    private val dataSource: UserDataSource
) : DataBinder<JSONObject> {
    override val fieldName: String = "user"

    override suspend fun getJsonObject(): JSONObject = JsonParsers.serialize(createUser())

    private fun createUser(): User {
        return User(
            platformAdvertisingId = dataSource.getAdvertisingId(),
            trackingAuthorizationStatus = dataSource.getTrackingAuthorizationStatus(),
            applicationId = dataSource.getApplicationId(),
            consent = null,
            coppa = false,
        )
    }
}
