package com.appodealstack.bidon.databinders.user

import com.appodealstack.bidon.config.models.User
import com.appodealstack.bidon.databinders.DataBinder
import com.appodealstack.bidon.utils.json.JsonParsers
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
